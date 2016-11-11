package mc.alk.arena.executors;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.competition.Match;
import mc.alk.arena.controllers.ArenaClassController;
import mc.alk.arena.controllers.BattleArenaController;
import mc.alk.arena.controllers.CompetitionController;
import mc.alk.arena.controllers.ParamController;
import mc.alk.arena.controllers.PlayerController;
import mc.alk.arena.controllers.RoomController;
import mc.alk.arena.controllers.TeleportController;
import mc.alk.arena.controllers.containers.RoomContainer;
import mc.alk.arena.listeners.PlayerHolder.LocationType;
import mc.alk.arena.listeners.custom.MethodController;
import mc.alk.arena.objects.ArenaClass;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.RegisteredCompetition;
import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.objects.arenas.ArenaType;
import mc.alk.arena.objects.joining.WaitingObject;
import mc.alk.arena.objects.spawns.SpawnIndex;
import mc.alk.arena.objects.spawns.SpawnLocation;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.util.ExpUtil;
import mc.alk.arena.util.InventoryUtil;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.MessageUtil;
import mc.alk.arena.util.NotifierUtil;
import mc.alk.arena.util.SerializerUtil;
import mc.alk.arena.util.TeamUtil;
import mc.alk.arena.util.TimingUtil;
import mc.alk.arena.util.TimingUtil.TimingStat;

public class BattleArenaDebugExecutor extends CustomCommandExecutor{

    @MCCommand( cmds = {"enableDebugging","ed"}, admin = true, usage = "enableDebugging <code section> <true | false>" )
    public void enableDebugging(CommandSender sender, String section, Boolean on) {
        
        if ( section.equalsIgnoreCase("transitions") ) Defaults.DEBUG_TRANSITIONS = on;
        else if ( section.equalsIgnoreCase("virtualplayer") || section.equalsIgnoreCase("vp") ) Defaults.DEBUG_VIRTUAL = on;
        else if ( section.equalsIgnoreCase("tracking") ) Defaults.DEBUG_TRACKING = on;
        else if ( section.equalsIgnoreCase("storage") ) Defaults.DEBUG_STORAGE = on;
        else if ( section.equalsIgnoreCase("commands") ) Defaults.DEBUG_COMMANDS = on;
        else if ( section.equalsIgnoreCase("debug") ) Defaults.DEBUG = on;
        else if ( section.equalsIgnoreCase("teams") ) Defaults.DEBUG_MATCH_TEAMS = on;
        else if ( section.equalsIgnoreCase( "messages" )) Defaults.DEBUG_MSGS = on;
        else {
            MessageUtil.sendMessage(sender, "&cDebugging couldnt find code section &6"+ section );
            return;
        }
        MessageUtil.sendMessage(sender, "&4[BattleArena] &2debugging for &6" + section + "&2 now &6" + on );
    }

    @MCCommand( cmds = {"giveTeam"}, op = true, usage = "giveTeam <player> <team index>" )
    public void giveTeamHelmOther(CommandSender sender, ArenaPlayer p, Integer index){
        TeamUtil.setTeamHead(index, p);
        MessageUtil.sendMessage(sender, p.getName() + " Given team " + index );
    }

    @MCCommand( cmds = {"giveTeam"}, op = true, usage = "giveTeam <team index>" )
    public void giveTeamHelm(ArenaPlayer p, Integer index){
        if (index < 0){
            p.getPlayer().setDisplayName(p.getName());
            MessageUtil.sendMessage(p, "&2Removing Team. &6/bad giveTeam <index> &2 to give a team name");
            return;
        }
        TeamUtil.setTeamHead(index, p);
        String tname = TeamUtil.getTeamName(index);
        p.getPlayer().setDisplayName(tname);
        MessageUtil.sendMessage(p, "&2Giving team " +index);
    }

    @MCCommand( cmds = {"giveHelm"}, op = true, exact = 2, usage = "giveHelm <item>" )
    public void giveHelm(Player sender, String[] args) {
        try {
            ItemStack is = InventoryUtil.parseItem(args[1]);
            sender.getInventory().setHelmet(is);
            MessageUtil.sendMessage(sender, "&2Giving helm " +InventoryUtil.getCommonName(is));
        } catch (Exception e) {
            MessageUtil.sendMessage(sender, "&e couldnt parse item " + args[1]);
        }
    }


    @MCCommand( cmds = {"showListeners"}, admin = true )
    public void showListeners(CommandSender sender, String args[]) {
        MethodController.showAllListeners( sender, args.length > 1 ? args[1] : "" );
    }

    @MCCommand( cmds = {"addKill"}, admin = true, min = 2, usage = "addKill <player>" )
    public void arenaAddKill(CommandSender sender, ArenaPlayer pl) {
        Match am = arenaController.getMatch(pl);
        if (am == null) {
            MessageUtil.sendMessage(sender,"&ePlayer " + pl.getName() +" is not in a match");
            return;
        }
        ArenaTeam t = am.getTeam(pl);
        
        if (t != null) t.addKill(pl);
        MessageUtil.sendMessage(sender,pl.getName()+" has received a kill");
    }

    @MCCommand( cmds = {"getExp"}, admin = true )
    public void getExp(Player player) {
        MessageUtil.sendMessage( player, ChatColor.GREEN + "Experience  " + player.getTotalExperience() + " " + 
                                    ExpUtil.getTotalExperience(player));
    }

    @MCCommand( cmds = {"showVars"}, admin = true )
    public void showVars(CommandSender sender, String paramName, String[] args) {
        MatchParams mp = findMatchParam(sender, paramName);
        if ( mp == null ) return;
        
        MessageUtil.sendMessage( sender, mp.toString() );
        if (args.length > 3 && args[3].equals("parent"))
            MessageUtil.sendMessage(sender, new ReflectionToStringBuilder(mp.getParent().getParent(), ToStringStyle.MULTI_LINE_STYLE)+"");
        else if (args.length > 2 && args[2].equals("parent"))
            MessageUtil.sendMessage(sender, new ReflectionToStringBuilder(mp.getParent(), ToStringStyle.MULTI_LINE_STYLE)+"");
        else
            MessageUtil.sendMessage(sender, new ReflectionToStringBuilder(mp, ToStringStyle.MULTI_LINE_STYLE)+"");
    }

    @MCCommand( cmds = {"showTransitions"}, admin = true )
    public boolean showTransitions(CommandSender sender, String paramName) {
        MatchParams mp = findMatchParam(sender, paramName);
        if ( mp != null ) {
            MessageUtil.sendMessage(sender, mp.toString());
            return true;
        }
        return false; 
    }

    @MCCommand( cmds = {"showPlayerVars"}, admin = true )
    public void showPlayerVars(CommandSender sender, ArenaPlayer player) {
        ReflectionToStringBuilder rtsb = new ReflectionToStringBuilder(player, ToStringStyle.MULTI_LINE_STYLE);
        MessageUtil.sendMessage(sender, rtsb.toString());
    }

    @MCCommand( cmds = {"showArenaVars"}, admin = true )
    public void showArenaVars(CommandSender sender, Arena arena, String[] args) {
        if (args.length > 4 && args[4].equals("parent"))
            MessageUtil.sendMessage(sender, 
                    new ReflectionToStringBuilder(arena.getParams().getParent().getParent(), ToStringStyle.MULTI_LINE_STYLE).toString());
        
        else if (args.length > 3 && args[3].equals("parent"))
            MessageUtil.sendMessage(sender, 
                    new ReflectionToStringBuilder( arena.getParams().getParent(), ToStringStyle.MULTI_LINE_STYLE ).toString() );
        
        else if (args.length > 3 && args[3].equals("transitions"))
            MessageUtil.sendMessage(sender, 
                    new ReflectionToStringBuilder(arena.getParams().getArenaStateGraph(), ToStringStyle.MULTI_LINE_STYLE).toString());
        
        else if (args.length > 2 && args[2].equals("waitroom"))
            MessageUtil.sendMessage(sender, 
                    new ReflectionToStringBuilder(arena.getWaitroom(), ToStringStyle.MULTI_LINE_STYLE).toString());
        
        else if (args.length > 2 && args[2].equals("params"))
            MessageUtil.sendMessage(sender, 
                    new ReflectionToStringBuilder(arena.getParams(), ToStringStyle.MULTI_LINE_STYLE).toString());         
        else 
            MessageUtil.sendMessage(sender, 
                    new ReflectionToStringBuilder(arena, ToStringStyle.MULTI_LINE_STYLE).toString());
    }

    @MCCommand( cmds={"showMatchVars"}, admin = true )
    public void showMatchVars(CommandSender sender, Arena arena, String[] vars) {
        Match match = BattleArena.getBAController().getMatch(arena);
        if (match == null) {
            MessageUtil.sendMessage(sender, "&cMatch not currently running in arena " + arena.getName());
            return;
        }
        if (vars.length > 2 && vars[2].equals("transitions")) {
            MessageUtil.sendMessage(sender, match.getParams().getArenaStateGraph().getOptionString());
            return;
        }
        if (vars.length > 2){
            String param = vars[2];
            boolean sb = vars.length > 3 && Boolean.valueOf(vars[3]);
            for( Field field : Match.class.getDeclaredFields() ) {
                if (field.getName().equalsIgnoreCase(param)){
                    field.setAccessible(true);
                    try {
                        if (sb) 
                            MessageUtil.sendMessage(sender, "&2Parameter " + param +" = <"+field.get(match) +">" );
                        else 
                            MessageUtil.sendMessage(sender, 
                                    new ReflectionToStringBuilder( field.get(match), ToStringStyle.MULTI_LINE_STYLE).toString());
                        
                    } catch (Exception e) {
                        MessageUtil.sendMessage(sender, "&cError getting param "+param+" : msg=" + e.getMessage());
                    }
                    return;
                }
            }
            MessageUtil.sendMessage(sender, "&cThe param &6"+param+ "&c does not exist in &6" + match.getClass().getSimpleName());
            return;
        }
        MessageUtil.sendMessage(sender, new ReflectionToStringBuilder(match, ToStringStyle.MULTI_LINE_STYLE).toString());
        return;
    }

    @MCCommand( cmds = {"showLobbyVars"}, admin = true )
    public void showLobbyVars(CommandSender sender, String arenatype) {
        ArenaType type = ArenaType.fromString(arenatype);
        if (type == null){
            MessageUtil.sendMessage(sender, "&cArenaType not found &6" + arenatype);
            return;
        }
        RoomContainer lobby = RoomController.getLobby(type);
        if (lobby == null) {
            MessageUtil.sendMessage(sender, "&cThere is no lobby for &6" + type.getName());
            return;
        }
        MessageUtil.sendMessage( sender, new ReflectionToStringBuilder(lobby, ToStringStyle.MULTI_LINE_STYLE).toString());
        MessageUtil.sendMessage(sender, 
                new ReflectionToStringBuilder(lobby.getParams().getArenaStateGraph(), ToStringStyle.MULTI_LINE_STYLE).toString());
    }

    private MatchParams findMatchParam(CommandSender sender, String paramName) {
        MatchParams mp = ParamController.getMatchParams(paramName);       
        if (mp == null) 
            MessageUtil.sendMessage(sender, "&cCouldn't find matchparams mp=" + paramName);       
        return mp;
    }

    @MCCommand( cmds = {"invalidReasons"}, admin = true )
    public void arenaInvalidReasons(CommandSender sender, Arena arena) {
        Collection<String> reasons = arena.getInvalidReasons();
        MessageUtil.sendMessage(sender, "&eInvalid reasons for &6" + arena.getName());
        
        if (!reasons.isEmpty())
            for (String reason: reasons)
                MessageUtil.sendMessage(sender, reason);
        
        else MessageUtil.sendMessage(sender, "&2There are no invalid reasons for &6" + arena.getName());    
    }

    @MCCommand( cmds = {"invalidQReasons"}, admin = true )
    public void matchQInvalidReasons(CommandSender sender, ArenaPlayer player, Arena arena) {
        WaitingObject qo = BattleArena.getBAController().getQueueObject(player);
        if (qo == null) {
            MessageUtil.sendMessage(sender, "&cThat player is not in a queue");
            return;
        }
        Collection<String> reasons = arena.getInvalidMatchReasons(qo.getParams(), qo.getJoinOptions());
        MessageUtil.sendMessage(sender, "&eInvalid reasons for &6" + arena.getName());
        
        if (!reasons.isEmpty())
            for (String reason: reasons)
                MessageUtil.sendMessage(sender, reason);
        
        else MessageUtil.sendMessage(sender, "&2There are no invalid reasons for &6" + arena.getName());   
    }

    @MCCommand( cmds = {"showClass"}, admin = true )
    public void showClass(CommandSender sender, String stringClass) {
        try {
            Class<?> clazz = Class.forName(stringClass);
            MessageUtil.sendMessage( sender, 
                    new ReflectionToStringBuilder(clazz, ToStringStyle.MULTI_LINE_STYLE).toString() );
        } 
        catch (ClassNotFoundException e) {
            MessageUtil.sendMessage(sender, "&cClass " + stringClass +" not found");
        }
    }

    @MCCommand( cmds = {"showAMQ"}, admin = true )
    public void showAMQ(CommandSender sender) {
        MessageUtil.sendMessage(sender, 
                new ReflectionToStringBuilder( BattleArena.getBAController().getArenaMatchQueue(), 
                                                                ToStringStyle.MULTI_LINE_STYLE ).toString());
    }

    @MCCommand( cmds = {"showBAC"}, admin = true )
    public void showBAC(CommandSender sender) {
        MessageUtil.sendMessage(sender, 
                new ReflectionToStringBuilder(BattleArena.getBAController(), ToStringStyle.MULTI_LINE_STYLE).toString());
    }

    @MCCommand( cmds = {"verify"}, admin = true )
    public void arenaVerify(CommandSender sender) {
        String[] lines = arenaController.toStringQueuesAndMatches().split("\n");
        for (String line : lines)
            MessageUtil.sendMessage(sender,line);
    }

    @MCCommand( cmds = {"showAllArenas"}, admin = true )
    public void arenaShowAllArenas(CommandSender sender) {
        String[] lines = arenaController.toStringArenas().split("\n");
        for (String line : lines)
            MessageUtil.sendMessage(sender,line);
    }

    @MCCommand( cmds = {"showq"}, admin = true )
    public void showQueue(CommandSender sender) {
        MessageUtil.sendMessage(sender,arenaController.queuesToString());
    }

    @MCCommand( cmds = {"showaq"}, admin = true )
    public void showArenaQueue(CommandSender sender) {
        MessageUtil.sendMessage( sender, arenaController.getArenaMatchQueue().toStringArenas() );
    }

    @MCCommand( cmds = {"online"}, admin = true )
    public void arenaVerify(CommandSender sender, OfflinePlayer p) {
        MessageUtil.sendMessage(sender, "Player " + p.getName() +"  is " + p.isOnline());
    }

    @MCCommand( cmds = {"purgeQueue"}, admin = true )
    public void arenaPurgeQueue(CommandSender sender) {
        try {
            Collection<ArenaTeam> teams = arenaController.purgeQueue();
            for (ArenaTeam t: teams){
                t.sendMessage("&eYou have been &cremoved&e from the queue by an administrator");
            }
            MessageUtil.sendMessage(sender,"&2Queue purged");
        } catch (Exception e){
            Log.printStackTrace(e);
            MessageUtil.sendMessage(sender,"&4error purging queue");
        }
    }

    @MCCommand( cmds = {"hasPerm"}, admin = true )
    public void hasPerm(CommandSender sender, String perm, Player p) {
        MessageUtil.sendMessage(sender, "Player " + p.getDisplayName() +"  hasPerm " + perm +" " +p.hasPermission(perm));
    }

    @MCCommand( cmds = {"setexp"}, op = true )
    public void setExp(CommandSender sender, ArenaPlayer p, Integer exp) {
        ExpUtil.setTotalExperience(p.getPlayer(), exp);
        MessageUtil.sendMessage(sender,"&2Player's exp set to " + exp );
    }

    @MCCommand( cmds = {"tp"}, admin = true, order = 337 )
    public void teleportToSpawn(ArenaPlayer sender, Arena arena, SpawnIndex index) {
        teleportToSpawn(sender,arena, LocationType.ARENA, index);
    }

    @MCCommand( cmds = {"tp"}, admin = true, order = 338 )
    public void teleportToSpawn(ArenaPlayer sender, Arena arena, String type, SpawnIndex index) {
        try {
            teleportToSpawn(sender, arena, LocationType.valueOf(type.toUpperCase()), index);
        } catch (IllegalArgumentException e){
            MessageUtil.sendMessage(sender,"&c" + e.getMessage());
        }
    }

    private void teleportToSpawn(ArenaPlayer sender, Arena arena, LocationType type, SpawnIndex index) {
        final SpawnLocation loc;
        switch(type){
            case ANY:
            case ARENA:
                loc = arena.getSpawn(index.teamIndex, index.spawnIndex);
                break;
            case WAITROOM:
                loc = arena.getWaitroom()!= null ?
                        arena.getWaitroom().getSpawn(index.teamIndex, index.spawnIndex) : null;
                break;
            case HOME:
                loc = sender.getOldLocation();
                break;
            case LOBBY:
                loc = arena.getLobby()!= null ?
                        arena.getLobby().getSpawn(index.teamIndex, index.spawnIndex) : null;
                break;
            case SPECTATE:
                loc = arena.getSpectatorRoom()!= null ?
                        arena.getSpectatorRoom().getSpawn(index.teamIndex, index.spawnIndex) : null;
                break;
            case NONE:
            case COURTYARD:
            default:
                loc = null;
                break;
        }
        if ( loc ==null )
            MessageUtil.sendMessage( sender,"&2Spawn " + (index.teamIndex + 1) + " " + (index.spawnIndex +1 ) + 
                                        " doesn't exist for " + (arena != null ? arena.getName() : "" ) + " " + type );
        else {
            TeleportController.teleport( sender, loc.getLocation() );
            MessageUtil.sendMessage( sender, "&2Teleported to &6" + type + " " + (index.teamIndex + 1) + " " + 
                        (index.spawnIndex + 1) + " &2loc=&6" + SerializerUtil.getBlockLocString(loc.getLocation()));
        }
    }

    @MCCommand( cmds = {"giveArenaClass"}, admin = true )
    public void giveArenaClass(CommandSender sender, String className, Player player) {
        ArenaClass clazz = ArenaClassController.getClass(className);
        if ( clazz == null )
            MessageUtil.sendMessage(sender, "&cArena class " + className +" doesn't exist");
        else {
            ArenaClassController.giveClass(PlayerController.toArenaPlayer(player), clazz);
            MessageUtil.sendMessage(sender,  "&2Arena class " + clazz.getDisplayName() +"&2 given to &6" + player.getName());
        }
    }

    @MCCommand( cmds = {"allowAdminCommands"}, admin = true )
    public void allowAdminCommands(CommandSender sender, Boolean enable) {
        Defaults.ALLOW_ADMIN_CMDS_IN_Q_OR_MATCH = enable;
        MessageUtil.sendMessage(sender,"&2Admins can "+ (enable ? "&6use" : "&cnot use")+"&2 commands in match");
    }

    @MCCommand( cmds = {"notify"}, admin = true )
    public void addNotifyListener(CommandSender sender, Player player, String type, Boolean enable) {
        if ( enable ) {
            NotifierUtil.addListener(player, type);
            if (!sender.getName().equals(player.getName()))
                MessageUtil.sendMessage( player, 
                        "&2 " + player.getDisplayName() + " &6now listening &2to " + type + " debugging messages" );            
            else MessageUtil.sendMessage( sender,
                    "&2 " + player.getName() + " &6now listening &2to " + type + " debugging messages" );
            return;
        }   
        NotifierUtil.removeListener(player, type);
        
        if ( !sender.getName().equals( player.getName() ) )
            MessageUtil.sendMessage(player,
                    "&2 " + player.getDisplayName() + " &cstopped listening&2 to " + type + " debugging messages" );       
        else MessageUtil.sendMessage(sender,
                "&2 " + player.getDisplayName() + " &cstopped listening&2 to " + type + " debugging messages" );
    }

    @MCCommand( cmds = {"showContainers"}, admin = true )
    public void showContainers(CommandSender sender, String args[]) {
        MatchParams params = null;
        if (args.length > 1) {
            params = ParamController.getMatchParamCopy(args[1]);
        }
        if ( params == null ) {
            MessageUtil.sendMessage(sender, "&5Lobbies");
            for (RoomContainer c : RoomController.getLobbies()){
                MessageUtil.sendMessage( sender," &2" + c.getName() +" : &6" + c.getContainerState() );
            }
        }
        MessageUtil.sendMessage(sender, "&5Arenas");
        BattleArena.getBAController();
        for (Arena a: BattleArenaController.getAllArenas().values()){
            if (params != null && a.getArenaType() != params.getType())
                continue;
            MessageUtil.sendMessage( sender," &2" + a.getName() +" - &6" + a.getContainerState() );
            if (a.getWaitroom() != null)
                MessageUtil.sendMessage( sender, "   &2   - &6" + a.getWaitroom().getName() + " : &6" + 
                                                        a.getWaitroom().getContainerState() );
        }
    }

    @MCCommand( cmds = {"setTimings"}, admin = true )
    public void setTimings(CommandSender sender, boolean set) {
        Defaults.DEBUG_TIMINGS = true;
        MessageUtil.sendMessage(sender, "&2Timings now " + set);
    }

    @MCCommand( cmds = {"timings"}, admin = true )
    public void showTimings(CommandSender sender, String[] args) {

        boolean useMs = !(args.length >1 && args[1].equalsIgnoreCase("ns"));
        List<TimingUtil> timers = TimingUtil.getTimers();
        
        if (timers == null){
            MessageUtil.sendMessage(sender, "Timings are not enabled");
            return;
        }
        if ((args.length >1 && args[1].equalsIgnoreCase("reset"))){
            TimingUtil.resetTimers();
             MessageUtil.sendMessage(sender, "Timings are reset");
             return;
        }
        String timeStr = (useMs ? "time(ms)" : "time(ns)");
        MessageUtil.sendMessage(sender, BattleArena.getNameAndVersion() + " " + timeStr );
        
        long gtotal = 0;
        for (TimingUtil timer : timers) {
            
            for (Entry<String,TimingStat> entry : timer.getTimings().entrySet()){
                TimingStat t = entry.getValue();
                long total = useMs ? t.totalTime/1000000 : t.totalTime;
                gtotal += total;
                
                if (useMs){
                    double avg = ((double) t.getAverage() / 1000000);
                    MessageUtil.sendMessage(sender, String.format(
                            "    %s  Time: %d Count: %d Avg: %.2f", entry.getKey(), total, t.count, avg));
                } 
                else {
                    long avg = t.getAverage();
                    MessageUtil.sendMessage(sender, String.format(
                            "    %s  Time: %d Count: %d Avg: %d", entry.getKey(), total, t.count, avg));
                }

            }
        }
        MessageUtil.sendMessage( sender, "    Total time " + gtotal + " " + timeStr );
    }

    @MCCommand( cmds = {"pasteConfig"}, admin = true )
    public void pasteConfig(CommandSender sender, String paramName, String[] args) {
        MatchParams mp = findMatchParam(sender, paramName);
        if (mp == null) return;
        
        RegisteredCompetition rc = CompetitionController.getCompetition(mp.getName());
        if (rc == null) {
            MessageUtil.sendMessage(sender, "&cNo config file found for " + paramName);
            return;
        }
        File f;
        if (args.length > 2 && args[2].equalsIgnoreCase("arenas"))
            f = rc.getArenaSerializer().getFile();
        else
            f = rc.getConfigSerializer().getFile();
        
        if (!f.exists())
            MessageUtil.sendMessage(sender, "&cNo config file found for " + paramName);
        else 
            MessageUtil.sendMessage(sender, "&2Paste command not yet implemented.");
    }

    @MCCommand( cmds = {"showScoreboard"}, admin = true )
    public void showScoreboard(CommandSender sender, Player player) {
        Scoreboard sc = player.getScoreboard();
        if (sc == null) {
            MessageUtil.sendMessage(sender, "&4Scoreboard for " + player.getDisplayName() +" is null");
            return;
        }
        MessageUtil.sendMessage(sender, "&4Scoreboard &f" + sc.hashCode());
        MessageUtil.sendMessage(sender, "&e -- Teams -- ");
        
        Collection<String> ops = sc.getEntries();
        Collection<String> names;
        for ( Team t : sc.getTeams() ) {
            names = new ArrayList<>();
            for ( String p : t.getEntries() ) {
                names.add( p );
            }
            MessageUtil.sendMessage(sender, t.getName() + " - " + t.getDisplayName() + " &f" + t.hashCode() + " = &6" +
                    StringUtils.join(names, ", "));
        }
        for (Objective o : sc.getObjectives()){
            MessageUtil.sendMessage(sender, "&2 -- Objective &e"+o.getName() +" - "+o.getDisplayName());
            TreeMap<Integer, List<String>> m = new TreeMap<>(Collections.reverseOrder());

            for (String op: ops) {
                Score score = o.getScore(op);
                if (score == null) continue;
                
                Team t = sc.getTeam(op);
                List<String> l = m.get(score.getScore());
                if (l == null) {
                    l = new ArrayList<>();
                    m.put(score.getScore(), l);
                }
                String displayName;
                if (t != null) {
                    displayName = (t.getPrefix()!=null?t.getPrefix():"") +
                            op + (t.getSuffix()!=null?t.getSuffix():"");
                } else {
                    displayName = op;
                }
                l.add(displayName);
            }
            for (Entry<Integer,List<String>> e : m.entrySet()) {
                for (String s : e.getValue()) {
                    MessageUtil.sendMessage(sender, s + " : " + e.getKey());
                }
            }
        }
    }
}
