package mc.alk.arena.serializers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;

import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.controllers.ArenaClassController;
import mc.alk.arena.controllers.OptionSetController;
import mc.alk.arena.controllers.ParamController;
import mc.alk.arena.controllers.StateController;
import mc.alk.arena.objects.ArenaClass;
import mc.alk.arena.objects.ArenaParams;
import mc.alk.arena.objects.ArenaSize;
import mc.alk.arena.objects.CommandLineString;
import mc.alk.arena.objects.CompetitionSize;
import mc.alk.arena.objects.CompetitionState;
import mc.alk.arena.objects.JoinType;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.MatchState;
import mc.alk.arena.objects.StateGraph;
import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.objects.arenas.ArenaType;
import mc.alk.arena.objects.exceptions.ConfigException;
import mc.alk.arena.objects.exceptions.InvalidOptionException;
import mc.alk.arena.objects.messaging.AnnouncementOptions;
import mc.alk.arena.objects.messaging.AnnouncementOptions.AnnouncementOption;
import mc.alk.arena.objects.modules.ArenaModule;
import mc.alk.arena.objects.modules.BrokenArenaModule;
import mc.alk.arena.objects.options.StateOptions;
import mc.alk.arena.objects.options.TransitionOption;
import mc.alk.arena.objects.victoryconditions.OneTeamLeft;
import mc.alk.arena.objects.victoryconditions.VictoryType;
import mc.alk.arena.util.EffectUtil;
import mc.alk.arena.util.InventoryUtil;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.MinMax;
import mc.alk.arena.util.SerializerUtil;

/**
 *
 * @author alkarin
 *
 */
public class ConfigSerializer extends BaseConfig {
    static Plugin plugin;
    final String name;

    public ConfigSerializer(Plugin _plugin, File configFile, String _name) {
        setConfig(configFile);
        name = _name;
        plugin = _plugin;
    }

    public MatchParams loadMatchParams() throws ConfigException, InvalidOptionException {
        return ConfigSerializer.loadMatchParams( this, name );
    }

    public static MatchParams loadMatchParams( BaseConfig config, String name )
                                                            throws ConfigException, InvalidOptionException {      
        ConfigurationSection cs = config.getConfig();
        
        if (config.getConfigurationSection(name) != null) 
            cs = config.getConfigurationSection(name);

        ArenaType at = getArenaType( cs );
        
        if ( at == null && !name.equalsIgnoreCase( "tourney" ) )
            throw new ConfigException( "Could not parse arena type. Valid types. " + ArenaType.getValidList() );

        return loadMatchParams( at, name, cs);
    }

    public static MatchParams loadMatchParams( ArenaType at, String name, ConfigurationSection cs)
                                                            throws ConfigException, InvalidOptionException {
        return loadMatchParams( at, name, cs, false );
    }

    public static MatchParams loadMatchParams( ArenaType at, String name, ConfigurationSection cs, boolean isNonBaseConfig ) 
                                                            throws ConfigException, InvalidOptionException {

        MatchParams mp = at != null ? new MatchParams(at) : new MatchParams();
        
        if ( !isNonBaseConfig || cs.contains( "victoryCondition" ) )
            mp.setVictoryType( loadVictoryType( cs ) ); 

        if ( !isNonBaseConfig ) {
            
            mp.setName( name );
            mp.setCommand( cs.getString( "command", name ) );
            if ( cs.contains( "cmd" ) )
                mp.setCommand( cs.getString( "cmd" ) );
        }
        loadGameSize( cs, mp, isNonBaseConfig );

        if ( cs.contains( "prefix" ) ) 
            mp.setPrefix( cs.getString( "prefix", "&6[" + name + "]" ) );
        
        if ( cs.contains( "displayName" ) ) 
            mp.setArenaDisplayName( cs.getString( "displayName" ) );
        
        if ( cs.contains( "signDisplayName" ) ) 
            mp.setSignDisplayName( cs.getString( "signDisplayName" ) );

        loadTimes( cs, mp ); 
        
        if ( !isNonBaseConfig || cs.contains( "nLives" ) )
            mp.setNLives( toPositiveSize( cs.getString( "nLives" ), 1 ) ); 

        loadBTOptions( cs, mp, isNonBaseConfig ); 

        if (cs.contains("nConcurrentCompetitions"))
            mp.setNumConcurrentCompetitions(ArenaSize.toInt(cs.getString("nConcurrentCompetitions","infinite")));
        
        if (cs.contains("waitroomClosedWhileRunning"))
            mp.setCloseWaitroomWhileRunning(cs.getBoolean("waitroomClosedWhileRunning",true));
        
        if (cs.contains("cancelIfNotEnoughPlayers"))
            mp.setCancelIfNotEnoughPlayers(cs.getBoolean("cancelIfNotEnoughPlayers",false));
        
        if (cs.contains("arenaCooldown"))
            mp.setArenaCooldown(cs.getInt("arenaCooldown"));
        
        if (cs.contains("allowedTeamSizeDifference"))
            mp.setAllowedTeamSizeDifference(cs.getInt("allowedTeamSizeDifference"));
        
        if (cs.contains("forceStartTime"))
            mp.setForceStartTime(cs.getInt("forceStartTime"));

        if (cs.contains("teamParams")) {
            
            ConfigurationSection teamcs = cs.getConfigurationSection("teamParams");
            Set<String> teamKeys = teamcs.getKeys(false);
            
            if (teamKeys != null) {
                Map<Integer, MatchParams> teamParams = new HashMap<>();
                for ( String s : teamKeys ) {
                    try {
                        Integer index = Integer.valueOf(s.substring(4)) - 1;
                        MatchParams p = loadMatchParams( null, s, teamcs.getConfigurationSection(s), true );
                        p.setParent(mp);
                        teamParams.put(index, p);
                    } catch (Exception e) {
                        Log.warn("team index " + s + " not found");
                    }
                }
                if (!teamParams.isEmpty()) {
                    mp.setArenaTeamParams(teamParams);
                }
            }
        }
        loadAnnouncementsOptions(cs, mp); 

        List<String> modules = loadModules(cs, mp); 

        StateGraph tops = loadTransitionOptions(cs, mp); 
        mp.setStateGraph(tops);

        mp.setParent(ParamController.getDefaultConfig());
        
        if (!isNonBaseConfig){
            ParamController.removeMatchType(mp);
            ParamController.addMatchParams(mp);
        }

        try {
            PlayerContainerSerializer pcs = new PlayerContainerSerializer();
            pcs.setConfig(BattleArena.getSelf().getDataFolder()+"/saves/containers.yml");
            pcs.load(mp);
        } 
        catch (Exception e){
            Log.err("Error loading Player Containers");
        }

        if ( !isNonBaseConfig && Defaults.DEBUG ) {
            String mods = modules.isEmpty() ? "" : " mods=" + StringUtils.join(modules,", ");
            Log.info("[" + plugin.getName() + "] Loaded " + mp.getName() + " params" + mods);
        }

        return mp;
    }

    public static VictoryType loadVictoryType(ConfigurationSection cs) throws ConfigException {
        
        VictoryType vt;
        if (cs.contains("victoryCondition"))
            vt = VictoryType.fromString(cs.getString("victoryCondition"));
        else
            vt = VictoryType.getType(OneTeamLeft.class);

        // TODO make unknown types with a valid plugin name be deferred until after the other plugin is loaded
        if (vt == null)
            throw new ConfigException("Could not add the victoryCondition " +cs.getString("victoryCondition") +"\n"
                    +"valid types are " + VictoryType.getValidList());
        return vt;
    }


    private static StateGraph loadTransitionOptions(ConfigurationSection cs, MatchParams mp)
            throws InvalidOptionException {
        if ( Defaults.DEBUG_TRACE ) Log.info( "Loading TransistionOptions from:-" + cs.getName() );
        
        StateGraph allTops = new StateGraph();
        boolean found = false;
        
        /// Set all Transition Options
        for (CompetitionState cstate : StateController.values()){
            
            /// OnCancel gets taken from onComplete and modified
            if (cstate == MatchState.ONCANCEL) continue;
            
            StateOptions tops;
            try {
                tops = getTransitionOptions(cs.getConfigurationSection(cstate.toString()));
                /// check for the most common alternate spelling of onPrestart
                /// also check for the old version of winners (winner)
                if (tops == null && cstate == MatchState.ONPRESTART){
                    tops = getTransitionOptions(cs.getConfigurationSection("onPrestart"));}
                else if (tops == null && cstate == MatchState.WINNERS){
                    tops = getTransitionOptions(cs.getConfigurationSection("winner"));}
                /// Merge any previous options
                StateOptions prevOptions = allTops.getOptions(cstate);
                if (prevOptions != null) {
                    if (tops == null){
                        tops = prevOptions;
                    }
                    else {
                        tops.addOptions(prevOptions);
                    }
                }
            } 
            catch (Exception e){
                Log.err("Invalid Option was not added!!! transition= " + cstate);
                Log.printStackTrace(e);
                continue;
            }
            if (tops == null){
                allTops.removeStateOptions(cstate);
                continue;}
            found = true;
            if ( Defaults.DEBUG_TRACE ) Log.info( "[ARENA] transition= " + cstate + " " + tops );

            if (cstate == MatchState.ONCOMPLETE){
                if (allTops.hasOptionAt(MatchState.ONLEAVE, TransitionOption.CLEARINVENTORY)){
                    tops.addOption(TransitionOption.CLEARINVENTORY);
                }
                StateOptions cancelOps = new StateOptions(tops);
                allTops.addStateOptions(MatchState.ONCANCEL, cancelOps);
                if ( Defaults.DEBUG_TRACE ) Log.info("[ARENA] transition= " + MatchState.ONCANCEL + " " + cancelOps );
            } 
            else if (cstate == MatchState.ONLEAVE){
                if (tops.hasOption(TransitionOption.TELEPORTOUT)){
                    tops.removeOption(TransitionOption.TELEPORTOUT);
                    Log.warn("You should never use the option teleportOut inside of onLeave!");
                }
            } 
            else if (cstate == MatchState.DEFAULTS){
                if (cs.getBoolean("duelOnly", false)){ /// for backwards compatibility
                    tops.addOption(TransitionOption.DUELONLY);}
            }
            splitOptions(allTops, cstate, tops);
            allTops.addStateOptions(cstate, tops);
        }
        if (allTops.hasOptionAt(MatchState.DEFAULTS, TransitionOption.ALWAYSOPEN))
            allTops.addStateOption(MatchState.ONJOIN, TransitionOption.ALWAYSJOIN);
        
        /// By Default if they respawn in the arena.. people must want infinite lives
        if (mp.hasOptionAt(MatchState.ONSPAWN, TransitionOption.RESPAWN) && !cs.contains("nLives")) {
            mp.setNLives(Integer.MAX_VALUE);
        }
        /// start auto setting this option, as really thats what they want
        if ( mp.getNLives() > 1 ){
            allTops.addStateOption(MatchState.ONDEATH, TransitionOption.RESPAWN);}
        if (!found && allTops.getAllOptions().isEmpty())
            return null;
        return allTops;
    }

    private static void splitOptions(StateGraph allTops, CompetitionState state, StateOptions tops) {
        if (    !(state instanceof MatchState) 
                ||  !(  state == MatchState.ONOPEN 
                        || state == MatchState.ONPRESTART 
                        || state == MatchState.ONSTART 
                        || state == MatchState.ONVICTORY ) )
            return;
        MatchState other=null;
        List<TransitionOption> add = new ArrayList<>();
        for (TransitionOption op : tops.getOptions().keySet()) {
            MatchState o = ((MatchState) state).getCorrectState(op);
            if (!o.equals(state)) {
                if (other == null)
                    other = o;
                add.add(op);
            }
        }
        if (!add.isEmpty()) {
            StateOptions newTops = new StateOptions();
            for (TransitionOption op : add) {
                Object o = tops.removeOption(op);
                try {
                    newTops.addOption(op,o);
                } 
                catch (InvalidOptionException e) {
                    e.printStackTrace();
                }
            }
            allTops.addStateOptions(other, newTops);
        }

    }

    private static void loadAnnouncementsOptions(ConfigurationSection cs, MatchParams mp) {
        if (cs.contains("announcements")) {
            AnnouncementOptions an = new AnnouncementOptions();
            BAConfigSerializer.parseAnnouncementOptions(an,true,cs.getConfigurationSection("announcements"), false);
            BAConfigSerializer.parseAnnouncementOptions(an,false,cs.getConfigurationSection("eventAnnouncements"),false);
            mp.setAnnouncementOptions(an);
        }
    }


    private static List<String> loadModules(ConfigurationSection cs, MatchParams mp) {
        List<String> modules = new ArrayList<>();

        if ( cs.contains( "modules" ) ) {
            List<?> keys = cs.getList( "modules" );
            
            if (keys != null){
                for (Object key: keys){
                    ArenaModule am = BattleArena.getModule(key.toString());
                    
                    if (am == null){
                        Log.err("[" + cs.getString("name") + "] Module " + key +" not found!");
                        mp.addModule(new BrokenArenaModule(key.toString()));
                    } 
                    else {
                        Log.info("[" + cs.getString("name") + "] Module " + key + " successfully loaded.");
                        mp.addModule(am);
                        modules.add(am.getName());
                    }
                }
            }
        }
        return modules;
    }

    private static void loadBTOptions(ConfigurationSection cs, MatchParams mp, boolean isNonBaseConfig) {
        if (cs.contains("tracking"))
            cs = cs.getConfigurationSection("tracking");

        /// TeamJoinResult in tracking for this match type
        String dbName = cs.contains("database") ? cs.getString( "database", null ) 
                                                : cs.getString( "db", null );
        
        if (dbName == null) 
            dbName = cs.getString("dbTableName", null);
        
        if (dbName != null) {
            mp.setTableName(dbName);
//            BTInterface.addBTI(mp);          
        }
        
        if (cs.contains("overrideBattleTracker"))
            mp.setUseTrackerPvP(cs.getBoolean("overrideBattleTracker", true));
        else 
            mp.setUseTrackerPvP(cs.getBoolean("useTrackerPvP", false));
        
        
        if (!isNonBaseConfig || cs.contains("useTrackerMessages"))
            mp.setUseTrackerMessages(cs.getBoolean("useTrackerMessages", false));
        
        if (cs.contains("teamRating")){
            mp.setUseTeamRating(cs.getBoolean("teamRating",false));}

        if (cs.contains("rated"))
            mp.setRated(cs.getBoolean("rated", true));
    }

    private static void loadGameSize(ConfigurationSection cs, MatchParams mp, boolean isArena) {
        
        if ( cs == null ) return;
        
        if (cs.contains("gameSize") || isArena)
            cs = cs.getConfigurationSection("gameSize");
        
        if ( cs == null ) return;
        
        if (!isArena || cs.contains("teamSize") ) 
            mp.setTeamSize(MinMax.valueOf(cs.getString("teamSize", "1+")));
        
        if (!isArena || cs.contains("nTeams") ) 
            mp.setNTeams(MinMax.valueOf(cs.getString("nTeams", "2+")));
    }


    private static void loadTimes(ConfigurationSection cs, MatchParams mp) {
        if (cs == null) return;
        if (cs.contains("times")) 
            cs = cs.getConfigurationSection("times"); 

        if (cs == null) return;
        if (cs.contains("timeBetweenRounds"))
            mp.setTimeBetweenRounds(cs.getInt("timeBetweenRounds",Defaults.TIME_BETWEEN_ROUNDS));
        
        if (cs.contains("secondsToLoot"))
            mp.setSecondsToLoot( cs.getInt("secondsToLoot", Defaults.SECONDS_TO_LOOT));
        
        if (cs.contains("secondsTillMatch"))
            mp.setSecondsTillMatch( cs.getInt("secondsTillMatch",Defaults.SECONDS_TILL_MATCH));

        if (cs.contains("matchTime"))
            mp.setMatchTime(toPositiveSize(cs.getString("matchTime")));
        
        if (cs.contains("matchUpdateInterval"))
            mp.setIntervalTime(cs.getInt("matchUpdateInterval",Defaults.MATCH_UPDATE_INTERVAL));
    }
    
    /** Zero & Negative should be interpreted as infinite: Integer.MAX_VALUE */
    public static int toPositiveSize(String value) {
        return toPositiveSize(value, CompetitionSize.MAX);
    }    
    public static int toPositiveSize(String value, int defValue) {
        int s = ArenaSize.toInt(value, defValue);
        return s <= 0 ? defValue : s;
    }
    public static int toNonNegativeSize(String value, int defValue) {
        int s = ArenaSize.toInt(value, defValue);
        return s < 0 ? defValue : s;
    }
    public static int toSize(String value, int defValue) {
        return ArenaSize.toInt(value, defValue);
    }
    /**
     * Get and create the ArenaType for this plugin given the Configuration section
     * @param cs section containing the "type"
     * @return The ArenaType
     * @throws ConfigException
     */
    public static ArenaType getArenaType(ConfigurationSection cs) throws ConfigException {
        return ArenaType.fromString(cs.getName()); 
    }
    /**
     * Get the ArenaClass for this plugin given the Configuration section
     * @param cs section containing the "type"
     * @return The ArenaClass
     * @throws ConfigException
     */
    public static Class<? extends Arena> getArenaClass(ConfigurationSection cs) throws ConfigException {
        String type = null;
        if (cs.contains("arenaType") || cs.contains("type") || cs.contains("arenaClass")) {
            type = cs.getString("arenaClass");
            if (type == null)
                type = cs.contains("type") ? cs.getString("type") : cs.getString("arenaType");
        }
        if (type != null){
            ArenaType at = ArenaType.fromString(type);
            if (at != null) {
                return ArenaType.getArenaClass(at);
            }
        }
        return null;
    }

    public static ArenaType getArenaGameType( ConfigurationSection cs) throws ConfigException {
        ArenaType at = null;
        if (cs.contains("gameType")){
            String s = cs.getString("gameType");
            at = ArenaType.fromString(s);
            
            if (at == null)
                at = getArenaType(cs);
        }
        return at;
    }
    
    private static void tryAddOption( StateOptions tops, TransitionOption option, Object obj ) {
        try {
            tops.addOption( option, obj );
        } 
        catch ( InvalidOptionException e ){
            Log.err( e.getMessage() );
            Log.printStackTrace(e);
        }
    }
    
    public static StateOptions getTransitionOptions(ConfigurationSection cs) 
                                        throws InvalidOptionException, IllegalArgumentException {
        if (cs == null ) return null;
        
        StateOptions tops = new StateOptions();
        
        if (cs.contains("options")) {
            if (!cs.isList("options")) 
                throw new InvalidOptionException( "options: should be a list, instead it was '" + 
                                                    cs.getString( "options", null ) + "'" );
            
            Collection<String> optionsstr = cs.getStringList("options");
            
            for ( String obj : optionsstr ) {
                String[] split = obj.split("=");
                String key = split[0].trim().toUpperCase();
                String value = split.length > 1 ? split[1].trim() : null;

                StateOptions optionSet = OptionSetController.getOptionSet(key);
                if (optionSet != null){
                    tops.addOptions(optionSet);
                    continue;
                }

                TransitionOption to;
                Object ovalue;
                try {
                    to = TransitionOption.fromString(key);
                    if ( to == TransitionOption.ENCHANTS ) /// we deal with these later
                        continue;
                    if ( to.hasValue() && value == null ) {
                        Log.err( "Transition Option " + to + " needs a value! " + key + "=<value>" );
                        continue;
                    }
                    ovalue = to.parseValue(value);
                } 
                catch (Exception e){
                    Log.err("Couldn't parse Option " + key + " value=" + value);
                    continue;
                }
                tops.addOption( to, ovalue );
            }
        }

        if ( cs.contains( "teleportTo" ) )
            tryAddOption( tops, TransitionOption.TELEPORTTO, SerializerUtil.getLocation( cs.getString( "teleportTo" ) ) );

        if ( cs.contains( "giveClass" ) )
            tryAddOption( tops, TransitionOption.GIVECLASS, getArenaClasses( cs.getConfigurationSection( "giveClass" ) ) );

        if ( cs.contains( "giveDisguise" ) )
            tryAddOption( tops, TransitionOption.GIVEDISGUISE, getArenaDisguises( cs.getConfigurationSection( "giveDisguise" ) ) );
        
        if ( cs.contains( "doCommands" ) )
            tryAddOption( tops, TransitionOption.DOCOMMANDS, getDoCommands( cs.getStringList( "doCommands" ) ) );


        /// Convert from old to new style aka ("needItems" and items: list, to needItems:)
        List<ItemStack> items = InventoryUtil.getItemList(cs,"needItems");
        
        if (items == null && tops.hasOption( TransitionOption.NEEDITEMS ) ) {
            items = InventoryUtil.getItemList(cs, "items");
            
            if ( items != null && !items.isEmpty() )
                tryAddOption( tops, TransitionOption.NEEDITEMS, items );
            else
                tops.removeOption( TransitionOption.NEEDITEMS );
        } 
        else if (items != null && !items.isEmpty())
            tryAddOption( tops, TransitionOption.NEEDITEMS, items );
        else 
            tops.removeOption( TransitionOption.NEEDITEMS );

        
        items = InventoryUtil.getItemList(cs,"giveItems");
        
        if (items == null)
            items = InventoryUtil.getItemList(cs,"items");

        if (items!=null && !items.isEmpty()) 
            tryAddOption( tops, TransitionOption.GIVEITEMS, items );
        else 
            tops.removeOption(TransitionOption.GIVEITEMS);
        

        items = InventoryUtil.getItemList(cs,"takeItems");
        
        if (items!=null && !items.isEmpty()) 
            tryAddOption( tops, TransitionOption.TAKEITEMS, items );
        else 
            tops.removeOption(TransitionOption.TAKEITEMS);


        List<PotionEffect> effects = getEffectList(cs, "enchants");
        
        if (effects!=null && !effects.isEmpty())
            tryAddOption( tops, TransitionOption.ENCHANTS, effects );
        else
            tops.removeOption(TransitionOption.ENCHANTS);   
 

//        setPermissionSection(cs,"addPerms",tops);

        return tops.getOptions() == null || tops.getOptions().isEmpty() ? null : tops;
    }

    public static List<CommandLineString> getDoCommands(List<String> list) {
        List<CommandLineString> commands = new ArrayList<>();
        
        for ( String line : list ) {
            commands.add( CommandLineString.parse(line) );
        }
        return commands;
    }

//    private static void setPermissionSection(ConfigurationSection cs, String nodeString, StateOptions tops) {
//        if (cs == null || !cs.contains(nodeString))
//            return ;
//        List<?> olist = cs.getList(nodeString);
//        List<String> permlist = new ArrayList<>();
//
//        for (Object perm: olist){
//            permlist.add(perm.toString());}
//        if (permlist.isEmpty())
//            return;
//        tops.addOption(TransitionOption.ADDPERMS, permlist);
//    }

    public static HashMap<Integer,ArenaClass> getArenaClasses(ConfigurationSection cs){
        
        HashMap<Integer,ArenaClass> classes = new HashMap<>();
        Set<String> keys = cs.getKeys(false);
        
        for ( String whichTeam : keys ) {
            int team = -1;
            if ( whichTeam.equalsIgnoreCase("default") ) {
                team = ArenaClass.DEFAULT;
            } 
            else {
                try {
                    team = Integer.parseInt( whichTeam.replaceAll( "team", "" ) ) - 1;
                } 
                catch( NumberFormatException e ) {
                    Log.err( "Couldn't find which team this class belongs to '" + whichTeam + "'" );
                    continue;
                }
            }
            if (team == -1 ) {
                Log.err( "Couldn't find which team this class belongs to '" + whichTeam + "'" );
                continue;
            }
            String className = cs.getString(whichTeam);
            ArenaClass ac = ArenaClassController.getClass(className);
            
            if (ac == null){
                Log.err( "Couldn't find arenaClass " + className );
                ac = new ArenaClass(className);
            }
            classes.put(team, ac);
        }
        return classes;
    }

    public static HashMap<Integer,String> getArenaDisguises(ConfigurationSection cs){
        HashMap<Integer,String> disguises = new HashMap<>();
        Set<String> keys = cs.getKeys(false);
        
        for ( String whichTeam : keys ) {
            int team;
            String disguiseName = cs.getString(whichTeam);
            
            if (whichTeam.equalsIgnoreCase("default")){
                team = Integer.MAX_VALUE;
            } 
            else {
                try {
                    team = Integer.parseInt( whichTeam.replaceAll( "team", "" ) ) - 1;
                } 
                catch( NumberFormatException e){
                    Log.err( "Couldnt find which team this disguise belongs to '" + whichTeam + "'" );
                    continue;
                }
            }
            if (team ==-1){
                Log.err( "Couldnt find which team this disguise belongs to '" + whichTeam + "'" );
                continue;
            }
            disguises.put(team, disguiseName);
        }
        return disguises;
    }

    public static List<PotionEffect> getEffectList(ConfigurationSection cs, String nodeString) {
        if (cs == null || cs.getList(nodeString) == null) return null;
        
        int strengthDefault = 0;
        int timeDefault = 60;
        ArrayList<PotionEffect> effects = new ArrayList<>();
        try {
            String str;
            for ( Object o : cs.getList(nodeString) ) {
                str = o.toString();
                try {
                    PotionEffect ewa = EffectUtil.parseArg(str,strengthDefault,timeDefault);
                    effects.add(ewa);
                } 
                catch (Exception e){
                    Log.err( "Effect " + cs.getCurrentPath() + "." + nodeString + "." + str + 
                            " could not be parsed in classes.yml. " + e.getMessage() );
                }
            }
        } 
        catch (Exception e){
            Log.err( "Effect " + cs.getCurrentPath() + "." + nodeString + " could not be parsed in classes.yml" );
        }
        return effects;
    }

    public static JoinType getJoinType(ConfigurationSection cs) {
        if (cs == null || cs.getName() == null) return null;
        if (cs.getName().equalsIgnoreCase("Tourney")) return JoinType.JOINPHASE;
        
        boolean isMatch = !cs.getBoolean("isEvent",false);
        isMatch = cs.getBoolean("queue",isMatch);
        
        if (cs.contains("joinType")){
            String type = cs.getString("joinType");
            try {
                return JoinType.fromString(type);
            } 
            catch (Exception e){
                Log.printStackTrace(e);
            }
        }
        return isMatch ? JoinType.QUEUE : JoinType.JOINPHASE;
    }

    public void save(MatchParams params){
        ConfigurationSection main = config.createSection(params.getName());
        saveParams(params,main, false);
        super.save();
    }

    /**
     * Not sure how best to not save particular values if its an arena or not
     * so for now, its a variable
     * @param params MatchParams
     * @param maincs ConfigurationSection to use
     * @param isNonBaseConfig isNonBaseConfig
     */
    public static void saveParams(MatchParams params, ConfigurationSection maincs, boolean isNonBaseConfig){
        ArenaParams parent = params.getParent();
        params.setParent( null ); 

        if (!isNonBaseConfig){
            maincs.set("name", params.getName());
            maincs.set("command", params.getCommand());
        }

        if ( params.getPrefix() != null ) maincs.set("prefix", params.getPrefix());
        if ( params.getDisplayName() != null ) maincs.set("displayName", params.getDisplayName());
        if ( params.getSignDisplayName() != null ) maincs.set("signDisplayName", params.getSignDisplayName());

        if (    params.getNTeams() != null 
                || params.getTeamSize() != null ) {
            ConfigurationSection cs = maincs.createSection("gameSize");
            
            if ( params.getNTeams() != null ) cs.set("nTeams", params.getNTeams().toString());
            if ( params.getTeamSize() != null ) cs.set("teamSize", params.getTeamSize().toString());
        }
        maincs.set( "nLives", ArenaSize.toString( params.getNLives() ) );
        
        if ( params.getVictoryType()!= null ) maincs.set("victoryCondition", params.getVictoryType().getName());

        if (    params.getSecondsTillMatch() != null 
                || params.getMatchTime() != null 
                || params.getSecondsToLoot() != null 
                || params.getTimeBetweenRounds() != null 
                || params.getIntervalTime() != null ) {
            ConfigurationSection cs = maincs.createSection("times");
            
            if ( params.getSecondsTillMatch() != null ) cs.set( "secondsTillMatch", params.getSecondsTillMatch() );
            if ( params.getMatchTime() != null ) cs.set("matchTime", ArenaSize.toString(params.getMatchTime()));
            if ( params.getSecondsToLoot() != null ) cs.set("secondsToLoot", params.getSecondsToLoot());
            if ( params.getTimeBetweenRounds() != null ) cs.set("timeBetweenRounds", params.getTimeBetweenRounds());
            if ( params.getIntervalTime() != null ) cs.set("matchUpdateInterval", params.getIntervalTime());
        }
        if (    params.isRated() != null 
                || params.getDBTableName() != null 
                || params.getUseTrackerMessages() != null ) {
            ConfigurationSection cs = maincs.createSection("tracking");
            
            if ( params.getDBTableName() != null ) cs.set("dbTableName", params.getDBTableName());
            if ( params.isRated() != null ) cs.set("rated", params.isRated());
            if ( params.getUseTrackerMessages() != null ) cs.set("useTrackerMessages", params.getUseTrackerMessages());
        }

        if ( !isNonBaseConfig && params.getType() != null ) {
            maincs.set("arenaType", params.getType().getName());
            try {
                maincs.set("arenaClass", ArenaType.getArenaClass(params.getType()).getSimpleName());
            } 
            catch(Exception e){
                maincs.set("arenaClass", params.getType().getClass().getSimpleName());
            }
        }

        /// Save specific team params
        if ( params.getTeamParams() != null ) {
            ConfigurationSection cs = maincs.createSection("teamParams");
            for ( Entry<Integer, MatchParams> entry : params.getTeamParams().entrySet() ) {
                saveParams( entry.getValue(), cs.createSection( "team" + ( entry.getKey() + 1 ) ), false );
            }
        }

        if (params.getNConcurrentCompetitions() != null)  
            maincs.set("nConcurrentCompetitions", ArenaSize.toString(params.getNConcurrentCompetitions()));

        if (params.isWaitroomClosedWhenRunning() != null)  
            maincs.set("waitroomClosedWhileRunning", params.isWaitroomClosedWhenRunning());

        if (params.isCancelIfNotEnoughPlayers() != null)  
            maincs.set("cancelIfNotEnoughPlayers", params.isCancelIfNotEnoughPlayers());

        maincs.set("arenaCooldown", params.getArenaCooldown());
        maincs.set("allowedTeamSizeDifference", params.getAllowedTeamSizeDifference());

        if (params.getForceStartTime() != null ) maincs.set("forceStartTime", params.getForceStartTime());

        Collection<ArenaModule> modules = params.getModules();
        if (modules != null && !modules.isEmpty()){ maincs.set("modules", getModuleList(modules));}

        AnnouncementOptions ao = params.getAnnouncementOptions();
        if (ao != null){
            Map<MatchState, Map<AnnouncementOption, Object>> map = ao.getMatchOptions();
            
            if (map != null){
                ConfigurationSection cs = maincs.createSection("announcements");
                
                for (Entry<MatchState, Map<AnnouncementOption, Object>> entry : map.entrySet()){
                    List<String> ops = new ArrayList<>();
                    
                    for (Entry<AnnouncementOption,Object> entry2 : entry.getValue().entrySet()){
                        Object o = entry2.getValue();
                        ops.add(entry2.getKey() +(o != null ? o.toString() :""));
                    }
                    cs.set(entry.getKey().name(), ops);
                }
            }

            map = ao.getEventOptions();
            if (map != null){
                for (Entry<MatchState, Map<AnnouncementOption, Object>> entry : map.entrySet()){
                    List<String> ops = new ArrayList<>();
                    for (Entry<AnnouncementOption,Object> entry2 : entry.getValue().entrySet()){
                        Object o = entry2.getValue();
                        ops.add(entry2.getKey() +(o != null ? o.toString() :""));
                    }
                    if (!ops.isEmpty()){
                        ConfigurationSection cs = maincs.createSection("eventAnnouncements");
                        cs.set(entry.getKey().name(), ops);
                    }
                }
            }
        }

        StateGraph alltops = params.getArenaStateGraph();
        if (alltops != null) {
            Map<CompetitionState, StateOptions> transitions =
                    new TreeMap<>( (o1, o2) -> { return o1.globalOrdinal() - o2.globalOrdinal(); } );
            
            transitions.putAll(alltops.getAllOptions());
            for (CompetitionState ms : transitions.keySet()) {
                try {
                    if (ms == MatchState.ONCANCEL)
                        continue;
                    StateOptions tops = transitions.get(ms);
                    if (tops == null)
                        continue;
                    if (tops.getOptions() == null)
                        continue;
                    tops = new StateOptions(tops); // make a copy so we can modify while saving
                    Map<TransitionOption, Object> ops = new TreeMap<>(tops.getOptions());
                    List<String> list = new ArrayList<>();

                    for (Entry<String, StateOptions> entry : OptionSetController.getOptionSets().entrySet()) {
                        if (tops.containsAll(entry.getValue())) {
                            list.add(entry.getKey());
                            for (TransitionOption op : entry.getValue().getOptions().keySet()) {
                                ops.remove(op);
                            }
                        }
                    }
                    
                    Map<String, Object> tmap = new LinkedHashMap<>();
                    ops = new TreeMap<>(ops); 
                    for (TransitionOption to : ops.keySet()) {
                        try {
                            String s;
                            switch (to) {
                                case NEEDITEMS:
                                    tmap.put(to.toString(), getItems(tops.getNeedItems()));
                                    break;
                                case GIVEITEMS:
                                    tmap.put(to.toString(), getItems(tops.getGiveItems()));
                                    break;
                                case TAKEITEMS:
                                    tmap.put(to.toString(), getItems(tops.getTakeItems()));
                                    break;
                                case GIVECLASS:
                                    tmap.put(to.toString(), getArenaClasses(tops.getClasses()));
                                    break;
                                case ENCHANTS:
                                    tmap.put(to.toString(), getEnchants(tops.getEffects()));
                                    break;
                                case DOCOMMANDS:
                                    tmap.put(to.toString(), getDoCommandsStringList(tops.getDoCommands()));
                                    break;
                                case TELEPORTTO:
                                    tmap.put(to.toString(), SerializerUtil.getLocString(tops.getTeleportToLoc()));
                                    break;
                                default:
                                    Object value = ops.get(to);
                                    if (value == null)
                                        s = to.toString();
                                    else
                                        s = to.toString() + "=" + value.toString();
                                    
                                    list.add(s);
                            }
                        } catch (Exception e) {
                            Log.err("[BA Error] couldn't save " + to);
                            Log.printStackTrace(e);
                        }
                    }
                    tmap.put("options", list);
                    maincs.set(ms.toString(), tmap);
                } 
                catch (Exception e) {
                    Log.printStackTrace(e);
                }
            }
        }
        params.setParent( parent ); 
    }

    public static List<String> getModuleList(Collection<ArenaModule> modules) {
        List<String> list = new ArrayList<>();
        if ( modules != null ) {
            for ( ArenaModule m : modules ) 
                list.add(m.getName());
        }
        return list;
    }

    public static List<String> getEnchants(List<PotionEffect> effects) {
        List<String> list = new ArrayList<>();
        if (effects != null){
            for (PotionEffect is: effects)
                list.add(EffectUtil.getEnchantString(is));
        }
        return list;
    }

    public static List<String> getItems(List<ItemStack> items) {
        List<String> list = new ArrayList<>();
        if (items != null){
            for (ItemStack is: items){
                if (is != null)
                    list.add(InventoryUtil.getItemString(is));
            }
        }
        return list;
    }

    public static Map<String,Object> getArenaClasses(Map<Integer, ArenaClass> classes) {
        HashMap<String,Object> map = new HashMap<>();
        for (Integer teamNumber: classes.keySet()){
            String teamName = teamNumber == ArenaClass.DEFAULT.intValue() ? "default" : "team" + (teamNumber + 1);
            map.put(teamName, classes.get(teamNumber).getName());
        }
        return map;
    }
    public static List<String> getDoCommandsStringList(List<CommandLineString> doCommands) {
        List<String> list = new ArrayList<>();
        if (doCommands != null){
            for ( CommandLineString s : doCommands )
                list.add(s.getRawCommand());
        }
        return list;
    }
}
