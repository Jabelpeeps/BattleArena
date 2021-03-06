package mc.alk.arena.serializers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.controllers.BattleArenaController;
import mc.alk.arena.controllers.ParamController;
import mc.alk.arena.controllers.RoomController;
import mc.alk.arena.controllers.Scheduler;
import mc.alk.arena.controllers.containers.AreaContainer;
import mc.alk.arena.controllers.containers.RoomContainer;
import mc.alk.arena.listeners.PlayerHolder;
import mc.alk.arena.objects.ArenaParams;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.StateGraph;
import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.objects.arenas.ArenaType;
import mc.alk.arena.objects.arenas.Persistable;
import mc.alk.arena.objects.exceptions.InvalidOptionException;
import mc.alk.arena.objects.exceptions.NeverWouldJoinException;
import mc.alk.arena.objects.exceptions.RegionNotFound;
import mc.alk.arena.objects.options.EventOpenOptions;
import mc.alk.arena.objects.options.TransitionOption;
import mc.alk.arena.objects.spawns.BlockSpawn;
import mc.alk.arena.objects.spawns.ChestSpawn;
import mc.alk.arena.objects.spawns.EntitySpawn;
import mc.alk.arena.objects.spawns.ItemSpawn;
import mc.alk.arena.objects.spawns.SpawnGroup;
import mc.alk.arena.objects.spawns.SpawnInstance;
import mc.alk.arena.objects.spawns.SpawnLocation;
import mc.alk.arena.objects.spawns.TimedSpawn;
import mc.alk.arena.plugins.WorldGuardController;
import mc.alk.arena.util.InventoryUtil;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.MinMax;
import mc.alk.arena.util.SerializerUtil;

public class ArenaSerializer extends BaseConfig {
    static BattleArenaController arenaController;
    static HashMap<Plugin, Set<ArenaSerializer>> configs = new HashMap<>();

    Plugin plugin;

    public static void setBAC(BattleArenaController bac){
        arenaController = bac;
    }

    public ArenaSerializer(Plugin _plugin, File _file){
        setConfig(_file);
        plugin = _plugin;

        config = new YamlConfiguration();
        Set<ArenaSerializer> paths = configs.get(_plugin);
        
        if (paths == null){
            paths = new HashSet<>();
            configs.put(_plugin, paths);
        } 
        else { /// check to see if we have this path already
            for (ArenaSerializer as : paths){
                if (as.file.getPath().equals(this.file.getPath())){
                    return;}
            }
        }
        paths.add(this);
    }

    public static void loadAllArenas(){
        for ( Plugin plugin : configs.keySet() ){
            loadAllArenas(plugin);
        }
    }

    public static void loadAllArenas(Plugin plugin){
        for (ArenaSerializer serializer: configs.get(plugin)){
            serializer.loadArenas();
        }
    }

    public static void loadAllArenas(Plugin plugin, ArenaType arenaType) {
        Set<ArenaSerializer> serializers = configs.get(plugin);
        if (serializers == null || serializers.isEmpty()){
            Log.err(plugin.getName() +" has no arenas to load");
            return;
        }

        for (ArenaSerializer serializer: serializers){
            serializer.loadArenas( arenaType );
        }
    }

    public void loadArenas() {
        try {
            config.load(file);
        } 
        catch (Exception e) {Log.printStackTrace(e); }
        
        loadArenas( BattleArena.getBAController(), config, null );
    }

    public void loadArenas( ArenaType arenaType ) {
        try {
            config.load(file);
        } 
        catch (Exception e){Log.printStackTrace(e);}
        
        loadArenas( BattleArena.getBAController(), config, arenaType);
    }

    protected void loadArenas( BattleArenaController bac, ConfigurationSection cs, ArenaType arenaType ) {
        final String pname = "["+plugin.getName()+"] ";
        if (cs == null){
            Log.info(pname+" " + arenaType + " has no arenas, cs is null");
            return;
        }

        ConfigurationSection as = cs.getConfigurationSection("arenas");
        ConfigurationSection bks = cs.getConfigurationSection("brokenArenas");
        
        if (as == null && bks == null){
            if (Defaults.DEBUG) 
                Log.info(pname+" " + arenaType + " has no arenas, configSectionPath=" + cs.getCurrentPath());
            return;
        }

        List<String> keys = (as == null) ? new ArrayList<>() : new ArrayList<>(as.getKeys(false));
        int oldGoodSize = keys.size();
        Set<String> brokenKeys = bks == null ? new HashSet<>() : bks.getKeys(false);
        int oldBrokenSize = brokenKeys.size();
        keys.addAll(brokenKeys);

        Set<String> brokenArenas = new HashSet<>();
        Set<String> loadedArenas = new HashSet<>();
        for (String name : keys){
            if (loadedArenas.contains(name) || brokenArenas.contains(name)) /// We already tried to load this arena
                continue;
            boolean broken = brokenKeys.contains(name);
            String section = broken ? "brokenArenas" : "arenas";
            if (arenaType != null){ /// Are we looking for 1 particular arena type to load
                String path = section+"."+name;
                String atype = cs.getString(path+".type",null);
                if (atype == null || !ArenaType.isSame(atype,arenaType)){
                    /// Its not the same type.. so don't let it affect the sizes of the arena counts
                    if (brokenArenas.remove(name)){
                        oldBrokenSize--;
                    } else{
                        oldGoodSize--;
                    }
                    continue;
                }
            }
            Arena arena = null;
            try{
                arena = loadArena(plugin, bac,cs.getConfigurationSection(section+"."+name));
                if (arena != null){
                    loadedArenas.add(arena.getName());
                    if (broken){
                        transfer(cs,"brokenArenas."+name, "arenas."+name);}
                }
            } catch(IllegalArgumentException e){
                Log.err(e.getMessage());
            } catch(Exception e){
                Log.printStackTrace(e);
            }
            if (arena == null){
                brokenArenas.add(name);
                if (!broken){
                    transfer(cs,"arenas."+name, "brokenArenas."+name);}
            }
        }
        if (!loadedArenas.isEmpty()) {
            Log.info(pname+"Loaded "+arenaType+" arenas: " + StringUtils.join(loadedArenas,", "));
        } else if (Defaults.DEBUG){
            Log.info(pname+"No arenas found for " + cs.getCurrentPath() +"  arenatype="+arenaType +"  cs="+cs.getName());
        }
        if (!brokenArenas.isEmpty()){
            Log.warn("&c"+pname+"&eFailed loading arenas: " + StringUtils.join(brokenArenas, ", ") + " arenatype="+arenaType +" cs="+cs.getName());
        }
        if (oldGoodSize != loadedArenas.size() || oldBrokenSize != brokenArenas.size()){
            try {
                config.save(file);
            } catch (IOException e) {
                Log.printStackTrace(e);
            }
        }
    }

    private static void transfer(ConfigurationSection cs, String string, String string2) {
        try{
            Map<String,Object> map = new HashMap<>(cs.getConfigurationSection(string).getValues(false));
            cs.createSection(string2, map);
            cs.set(string,null);
        } catch(Exception e){
            Log.printStackTrace(e);
        }
    }

    public static Arena loadArena(Plugin plugin, BattleArenaController bac, ConfigurationSection cs) {
        String name = cs.getName().toLowerCase();

        ArenaType atype = ArenaType.fromString(cs.getString("type"));
        if (atype==null){
            Log.err(" Arena type not found for " + name);
            return null;
        }
        MatchParams mp = new MatchParams(atype);
        try {
            if (cs.contains("params"))
                mp = ConfigSerializer.loadMatchParams( atype, name, cs.getConfigurationSection("params"), true );
        } 
        catch (Exception e) {
            Log.printStackTrace(e);
        }
        /// Get from the "old" way of specifying teamSize and nTeams
        if (cs.contains("teamSize")) {
            MinMax mm = MinMax.valueOf(cs.getString("teamSize"));
            mp.setTeamSize(mm);
        }
        if (cs.contains("nTeams")) {
            MinMax mm = MinMax.valueOf(cs.getString("nTeams"));
            mp.setNTeams(mm);
        }

        if (!mp.valid()){
            Log.err( name + " This arena is not valid arenaq=" + mp.toString());
            return null;
        }

        final Arena arena = ArenaType.createArena(name, mp,false);
        if (arena == null){
            Log.err("Couldnt load the Arena " + name);
            return null;
        }


        /// Spawns
        Map<Integer, List<SpawnLocation>> locs = SerializerUtil.parseLocations(cs.getConfigurationSection("locations"));
        if (locs != null){
            setSpawns(arena,locs);
        }

        /// Wait room spawns
        locs = SerializerUtil.parseLocations(cs.getConfigurationSection("waitRoomLocations"));
        if (locs != null){
            RoomContainer rc = RoomController.getOrCreateRoom(arena, PlayerHolder.LocationType.WAITROOM);
            setSpawns(rc, locs);
        }

        /// Spectate spawns
        locs = SerializerUtil.parseLocations(cs.getConfigurationSection("spectateLocations"));
        if (locs != null){
            RoomContainer rc = RoomController.getOrCreateRoom(arena, PlayerHolder.LocationType.SPECTATE);
            setSpawns(rc, locs);
        }

        /// Visitor spawns
        locs = SerializerUtil.parseLocations(cs.getConfigurationSection("visitorLocations"));
        if (locs != null) {
            RoomContainer rc = RoomController.getOrCreateRoom(arena, PlayerHolder.LocationType.VISITOR);
            setSpawns(rc, locs);
        }

        /// Item/mob/group spawns
        ConfigurationSection spawncs = cs.getConfigurationSection("spawns");
        if (spawncs != null){
            for (String spawnStr : spawncs.getKeys(false)){
                ConfigurationSection scs = spawncs.getConfigurationSection(spawnStr);
                TimedSpawn s;
                try {
                    s = parseSpawnable(scs);
                } catch (Exception e) {
                    Log.printStackTrace(e);
                    continue;
                }
                if (s == null)
                    continue;
                arena.putTimedSpawn(Long.parseLong(spawnStr), s);
            }
        }
        cs = cs.getConfigurationSection("persistable");
        Persistable.yamlToObjects(arena, cs,Arena.class);
        updateRegions(arena);
        arena.publicInit();
        bac.addArena(arena);

        if (arena.getParams().hasAnyOption(TransitionOption.ALWAYSOPEN)) {
            Scheduler.scheduleSynchronousTask( 
                    () -> {
                        try {
                            EventOpenOptions eoo = EventOpenOptions.parseOptions( new String[]{"COPYPARAMS"}, 
                                                                                  null, 
                                                                                  arena.getParams());
                            Arena a = bac.reserveArena(arena);
                            if (a == null) {
                                Log.warn("&cArena &6" + arena.getName() + " &cwas set to always open but could not be reserved");
                            } 
                            else {
                                eoo.setSecTillStart(0);
                                bac.createAndAutoMatch(arena, eoo);
                            }
                        } catch ( InvalidOptionException | NeverWouldJoinException | IllegalStateException e ) {
                            e.printStackTrace();
                        }
                    });
        }
        return arena;
    }

    private static void setSpawns(AreaContainer rc, Map<Integer, List<SpawnLocation>> locs) {
        for (Entry<Integer, List<SpawnLocation>> entry: locs.entrySet()) {
            try {
                List<SpawnLocation> list = entry.getValue();
                for (int i = 0; i <list.size(); i++){
                    rc.setSpawnLoc(entry.getKey(), i, list.get(i));
                }
            } catch (IllegalStateException e) {
                Log.printStackTrace(e);
            }
        }
    }

    private static void updateRegions(Arena arena) {
        if (!WorldGuardController.hasWorldGuard())
            return;
        if (!arena.hasRegion())
            return;
        if (!WorldGuardController.hasRegion(arena.getWorldGuardRegion())){
            Log.err("Arena " + arena.getName() +" has a world guard region defined but it no longer exists inside of WorldGuard."+
                    "You will have to remake the region.  /arena alter <arena name> addregion");}
        MatchParams mp = ParamController.getMatchParamCopy(arena.getArenaType().getName());
        if (mp == null)
            return;
        StateGraph trans = mp.getArenaStateGraph();
        if (trans == null)
            return;
        WorldGuardController.setFlag(arena.getWorldGuardRegion(), "entry", !trans.hasOption(TransitionOption.WGNOENTER));
        try {
            WorldGuardController.trackRegion(arena.getWorldGuardRegion());
        } catch (RegionNotFound e) {
            Log.printStackTrace(e);
        }
    }

    private void saveArenas(boolean log) {
        BattleArena.getBAController();
        ArenaSerializer.saveArenas(BattleArenaController.getAllArenas().values(), file, config, plugin,log);
        try {
            config.save(file);
        } catch (IOException e) {
            Log.printStackTrace(e);
        }
    }

    private static void saveArenas(Collection<Arena> arenas, File f, ConfigurationSection config, Plugin plugin, boolean log){
        ConfigurationSection maincs = config.createSection("arenas");
        config.createSection("brokenArenas");
        List<String> saved = new ArrayList<>();
        for (Arena arena : arenas){
            String arenaname = null;
            try{
                arenaname = arena.getName();
                ArenaType at = arena.getArenaType();
                if (!at.getPlugin().getName().equals(plugin.getName()))
                    continue;
                ArenaParams parentParams = arena.getParams().getParent();
                arena.getParams().setParent(null);
                HashMap<String, Object> amap = new HashMap<>();
                amap.put("type", at.getName());

                /// Spawn locations
                Map<String, List<String>> locs = SerializerUtil.toSpawnMap(arena);
                if (locs != null){
                    amap.put("locations", locs);}

                /// Wait room spawns
                locs = SerializerUtil.toSpawnMap(arena.getWaitroom());
                if (locs != null) {
                    amap.put("waitRoomLocations", locs);}

                /// spectate locations
                locs = SerializerUtil.toSpawnMap(arena.getSpectatorRoom());
                if (locs != null) {
                    amap.put("spectateLocations", locs);}

                locs = SerializerUtil.toSpawnMap(arena.getVisitorRoom());
                if (locs != null) {
                    amap.put("visitorLocations", locs);}

                Map<Long, TimedSpawn> timedSpawns = arena.getTimedSpawns();
                
                if (timedSpawns != null && !timedSpawns.isEmpty()){
                    HashMap<String,Object> spawnmap = new HashMap<>();
                
                    for (Long key: timedSpawns.keySet() ){
                        TimedSpawn ts = timedSpawns.get(key);
                        HashMap<String,Object> itemSpawnMap = saveSpawnable(ts);
                        spawnmap.put(key.toString(), itemSpawnMap);
                    }
                    amap.put("spawns", spawnmap);
                }

                Map<String,Object> persisted = Persistable.objectsToYamlMap(arena, Arena.class);
                if (persisted != null && !persisted.isEmpty()){
                    amap.put("persistable", persisted);
                }
                saved.add(arenaname);

                ConfigurationSection arenacs = maincs.createSection(arenaname);
                SerializerUtil.expandMapIntoConfig(arenacs, amap);

                ConfigSerializer.saveParams(arena.getParams(), arenacs.createSection("params"), true);
                arena.getParams().setParent(parentParams);

                config.set("brokenArenas."+arenaname, null); /// take out any duplicate names in broken arenas
            } 
            catch (Exception e){
                Log.printStackTrace(e);
                if (arenaname != null){
                    transfer(config, "arenas."+arenaname, "brokenArenas."+arenaname);
                }
            }
        }
        if (log)
            Log.info(plugin.getName() + " Saving arenas " + StringUtils.join(saved,",") +" to " +
                    f.getPath() + " configSection="+config.getCurrentPath()+"." + config.getName());
    }


    public void saveArenas() {
        saveArenas(false);
    }
    @Override
    public void save() {
        saveArenas(true);
    }

    public static void saveAllArenas(boolean log){
        for (Plugin plugin: configs.keySet()){
            for (ArenaSerializer serializer: configs.get(plugin)){
                serializer.saveArenas(log);
            }
        }
    }

    public static void saveArenas(Plugin plugin){
        if (!configs.containsKey(plugin))
            return;
        for (ArenaSerializer serializer: configs.get(plugin)){
            serializer.saveArenas(true);
        }
    }

    private static TimedSpawn parseSpawnable(ConfigurationSection cs) throws IllegalArgumentException {
        
        if (!cs.contains("spawn") || !cs.contains("time") || !cs.contains("loc")){
            Log.err("configuration section cs = " + cs +"  is missing either spawn, time, or loc");
            return null;
        }
        String strs[] = cs.getString( "time" ).split(" ");
        int is[] = new int[strs.length];
        for ( int i = 0; i < strs.length; i++ ) {
            is[i] = Integer.parseInt( strs[i] );
        }
        
        Location loc = SerializerUtil.getLocation(cs.getString("loc"));
        List<String> strings = SpawnSerializer.convertToStringList(cs.getString("spawn"));
        
        if (strings == null || strings.isEmpty())
            return null;
        
        SpawnInstance si;
        if (cs.contains("type") && cs.getString("type").equalsIgnoreCase("block")){
            
            si = new BlockSpawn(loc.getBlock(),false);
            Material mat = Material.valueOf(cs.getString("spawn"));
            ((BlockSpawn)si).setMaterial(mat);
            try {
                mat = Material.valueOf(cs.getString("despawnMaterial", "AIR"));
            } catch (Exception e) {
                Log.err("Error setting despawnMaterial. " + e.getMessage());
                mat = Material.AIR;
            }
            if (mat != null)
                ((BlockSpawn)si).setDespawnMaterial(mat);    
        }
        else if (cs.contains("type") && cs.getString("type").equalsIgnoreCase("chest")) {
            si = new ChestSpawn(loc.getBlock(), false);
            Material mat = Material.valueOf(cs.getString("spawn"));
            ((BlockSpawn)si).setMaterial(mat);
            List<ItemStack> items = InventoryUtil.getItemList(cs,"items");
            List<ItemStack> giveItems = InventoryUtil.getItemList(cs, "giveItems");
            items = (items.size() >= giveItems.size()) ? items : giveItems;
            ((ChestSpawn)si).setItems(items);
        } 
        else {
            List<SpawnInstance> spawns = SpawnSerializer.parseSpawnable(strings);
            if (spawns == null || spawns.isEmpty())
                return null;

            spawns.get(0).setLocation(loc);
            si = spawns.get(0);
        }
        return new TimedSpawn( is[0], is[1], is[2], si );
    }

    private static HashMap<String, Object> saveSpawnable(TimedSpawn ts) {
        HashMap<String, Object> spawnMap = new HashMap<>();
        SpawnInstance si = ts.getSpawn();
        String key = null;
        String value =null;
        
        if (si instanceof SpawnGroup){
            SpawnGroup in = (SpawnGroup) si;
            key = in.getName();
            value =  "1";
        } 
        else if (si instanceof ItemSpawn){
            ItemSpawn in = (ItemSpawn) si;
            key = InventoryUtil.getItemString(in.getItemStack());

        } 
        else if (si instanceof EntitySpawn){
            EntitySpawn in = (EntitySpawn) si;
            key = in.getEntityString() + " " + in.getNumber();
        } 
        else if (si instanceof ChestSpawn){
            ChestSpawn bs = (ChestSpawn) si;
            key = bs.getMaterial().name();
            ItemStack[] items = bs.getItems();
            spawnMap.put("type", "chest");
            if (items != null) {
                spawnMap.put("items", ConfigSerializer.getItems(Arrays.asList(items)));
            }
        } 
        else if (si instanceof BlockSpawn){
            BlockSpawn bs = (BlockSpawn) si;
            spawnMap.put("type", "block");
            spawnMap.put("despawnMat", (bs.getDespawnMaterial()!=null ? bs.getDespawnMaterial().name() : "AIR"));
            key = bs.getMaterial().name();
        }

        if (value == null)
            spawnMap.put("spawn", key);
        else
            spawnMap.put("spawn", key+":" + value);
        spawnMap.put("loc", SerializerUtil.getLocString(si.getLocation()));
        spawnMap.put("time", ts.getFirstSpawnTime() + " " + ts.getRespawnTime() + " " + ts.getTimeToDespawn());
        return spawnMap;
    }
}
