package mc.alk.arena.tracker;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.controllers.Scheduler;
import mc.alk.arena.executors.BattleTrackerExecutor;
import mc.alk.arena.executors.TrackerExecutor;
import mc.alk.arena.listeners.BTSignListener;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.tracker.StatSign;
import mc.alk.arena.util.FileUtil;

public class Tracker {

    static JavaPlugin BA = BattleArena.getSelf();
    final static Map<String, TrackerInterface> interfaces = Collections.synchronizedMap( new ConcurrentHashMap<>() );
    static SignController signController = new SignController();
    final static String CONFIG = "/tracker.yml";
    final static String MESSAGES = "/tracker_messages.yml";

    public static boolean isEnabled() { return !interfaces.isEmpty(); }
    
    public static void loadConfigs() {

        File dir = BA.getDataFolder();
        if (!dir.exists())
            dir.mkdirs();
        
        ConfigurationSerialization.registerClass( StatSign.class );

        String dataDir = BA.getDataFolder().getPath();      
        TrackerConfigController.setConfig( FileUtil.load( dataDir + CONFIG, "/default_files" + CONFIG ) );
        TrackerMessageController.setConfig( FileUtil.load( dataDir + MESSAGES, "/default_files" + MESSAGES ) );

        TrackerConfigController.loadAll();
        TrackerMessageController.load();

        BA.getCommand("battleTracker").setExecutor( new BattleTrackerExecutor() );
        BA.getCommand("btpvp").setExecutor( new TrackerExecutor( getPVPInterface() ) );
        BA.getCommand("btpve").setExecutor( new TrackerExecutor( getPVEInterface() ) );
        
        Scheduler.scheduleSynchronousTask( () -> {
                signController.getSerialiser().setConfig( dataDir + "/signs.yml" );
                signController.getSerialiser().loadAll();
        }, 22);

        YamlConfigUpdater cu = new YamlConfigUpdater();
        cu.update(TrackerConfigController.getConfig(), TrackerConfigController.getFile());

        YamlMessageUpdater mu = new YamlMessageUpdater();
        mu.update(TrackerMessageController.getConfig(), TrackerMessageController.getFile());

        if (Defaults.USE_SIGNS) {
            Bukkit.getPluginManager().registerEvents( new BTSignListener( signController ), BA );
            Bukkit.getScheduler().scheduleSyncRepeatingTask( BA, () -> signController.updateSigns(), 20, 1000 );
        }
    }

    public static void saveConfig() {
        synchronized (interfaces) {
            for ( TrackerInterface ti : interfaces.values() ) 
                ti.flush();
        }
        if (signController.getSerialiser() != null) {
            signController.getSerialiser().saveAll();
        }
    }

    public static TrackerInterface getPVPInterface() {
        return getInterface(Defaults.PVP_INTERFACE, true);
    }

    public static TrackerInterface getPVEInterface() {
        return getInterface(Defaults.PVE_INTERFACE, false);
    }

    public static TrackerInterface getInterface(String interfaceName) {
        return getInterface( interfaceName, false );
    }

    public static TrackerInterface getInterface( MatchParams params ) {
        return getInterface( params.getName(), true );
    }
    
    private static TrackerInterface getInterface(String interfaceName, boolean saveIndividualRecords) {
        String iname = interfaceName.toLowerCase();
        if (!interfaces.containsKey(iname)) {
            interfaces.put(iname, new TrackerInterface(interfaceName, saveIndividualRecords));
        }
        return interfaces.get(iname);
    }

    public static boolean hasInterface(String interfaceName) {
        return interfaces.containsKey( interfaceName.toLowerCase() );
    }
    public static Collection<TrackerInterface> getAllInterfaces() {
        return new ArrayList<>(interfaces.values());
    }
    static Set<String> dontTrack = new HashSet<>();
    static Set<String> dontAnnounce = new HashSet<>();

    public static boolean notTracked(Player target) { return dontTrack.contains(target.getName()); }
    public static boolean notTracked(String target) { return dontTrack.contains(target); }
    
    public static void stopTracking( String playername, boolean now ) {
        if (now) 
            dontTrack.add(playername);
        else 
            Scheduler.scheduleAsynchronousTask( () -> dontTrack.add(playername) );
    }
    
    public static void resumeTracking( String playername, boolean now ) {
        if (now) 
            dontTrack.remove(playername);
        else 
            Scheduler.scheduleAsynchronousTask( () -> dontTrack.remove(playername) );
    }
    
    public static void stopAnnouncing( String playername, boolean now ) {
        if (now) 
            dontAnnounce.add(playername);
        else 
            Scheduler.scheduleAsynchronousTask( () -> dontAnnounce.add(playername) );
    }

    public static void resumeAnnouncing( String playername, boolean now ) {
        if (now)
            dontAnnounce.remove(playername);
        else
            Scheduler.scheduleAsynchronousTask( () -> dontAnnounce.remove(playername) );
    }

    public static void stopAnnouncing(Collection<Player> players) {
        for ( Player p : players )
            dontAnnounce.add(p.getName());
        }
    public static void resumeAnnouncing(Collection<Player> players) {
        for ( Player p : players )
            dontAnnounce.remove(p.getName());
        }
    public static void stopTracking(Collection<Player> players) {
        for ( Player p : players )
            dontTrack.add(p.getName());
        }
    public static void resumeTracking(Collection<Player> players) {
        for ( Player p : players )
            dontTrack.remove(p.getName());
        }
}
