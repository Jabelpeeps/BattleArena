package mc.alk.tracker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.controllers.Scheduler;
import mc.alk.arena.executors.BattleTrackerExecutor;
import mc.alk.arena.executors.TrackerExecutor;
import mc.alk.arena.listeners.BTEntityListener;
import mc.alk.arena.listeners.BTSignListener;
import mc.alk.arena.serializers.tracker.YamlConfigUpdater;
import mc.alk.arena.serializers.tracker.YamlMessageUpdater;
import mc.alk.tracker.controllers.SignController;
import mc.alk.tracker.controllers.TrackerConfigController;
import mc.alk.tracker.controllers.TrackerInterface;
import mc.alk.tracker.controllers.TrackerMessageController;
import mc.alk.tracker.objects.StatSign;

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

        Bukkit.getPluginManager().registerEvents(new BTEntityListener(), BA);

        BA.getCommand("battleTracker").setExecutor(new BattleTrackerExecutor());
        BA.getCommand("btpvp").setExecutor(new TrackerExecutor(getInterface(Defaults.PVP_INTERFACE)));
        BA.getCommand("btpve").setExecutor(new TrackerExecutor(getInterface(Defaults.PVE_INTERFACE)));
        
        File data = BA.getDataFolder();      
        TrackerConfigController.setConfig( load( "/default_files" + CONFIG, data.getPath() + CONFIG ) );
        TrackerMessageController.setConfig( load( "/default_files" + MESSAGES, data.getPath() + MESSAGES ) );

        TrackerConfigController.loadAll();
        TrackerMessageController.load();
        
        Scheduler.scheduleSynchronousTask( () -> {
                signController.getSerialiser().setConfig( data.getPath() + "/signs.yml" );
                signController.getSerialiser().loadAll();
        }, 22);

        YamlConfigUpdater cu = new YamlConfigUpdater();
        cu.update(TrackerConfigController.getConfig(), TrackerConfigController.getFile());

        YamlMessageUpdater mu = new YamlMessageUpdater();
        mu.update(TrackerMessageController.getConfig(), TrackerMessageController.getFile());

        if (Defaults.USE_SIGNS) {
            Bukkit.getPluginManager().registerEvents(new BTSignListener(signController), BA);
            Bukkit.getScheduler().scheduleSyncRepeatingTask(BA, () -> signController.updateSigns(), 20, 1000);
        }
    }

    public static void saveConfig() {
        synchronized (interfaces) {
            for (TrackerInterface ti : interfaces.values()) {
                ti.flush();
            }
        }
        if (signController.getSerialiser() != null) {
            signController.getSerialiser().saveAll();
        }
    }

    public static TrackerInterface getPVPInterface() {
        return getInterface(Defaults.PVP_INTERFACE, new TrackerOptions());
    }

    public static TrackerInterface getPVEInterface() {
        return getInterface(Defaults.PVE_INTERFACE, new TrackerOptions());
    }

    public static TrackerInterface getInterface(String interfaceName) {
        return getInterface(interfaceName, new TrackerOptions());
    }

    public static TrackerInterface getInterface(String interfaceName, TrackerOptions trackerOptions) {
        String iname = interfaceName.toLowerCase();
        if (!interfaces.containsKey(iname)) {
            interfaces.put(iname, new TrackerInterface(interfaceName, trackerOptions));
        }
        return interfaces.get(iname);
    }

    public static boolean hasInterface(String interfaceName) {
        String iname = interfaceName.toLowerCase();
        return interfaces.containsKey(iname);
    }

    public static Collection<TrackerInterface> getAllInterfaces() {
        return new ArrayList<>(interfaces.values());
    }
    
    public static File load(String default_file, String config_file) {
        File file = new File(config_file);
        if (!file.exists()){ /// Create a new file from our default example
            try{
                InputStream inputStream = BA.getClass().getResourceAsStream(default_file);
                OutputStream out=new FileOutputStream(config_file);
                byte buf[]=new byte[1024];
                int len;
                while((len=inputStream.read(buf))>0){
                    out.write(buf,0,len);}
                out.close();
                inputStream.close();
            } catch (Exception e){ }
        }
        return file;
    }
}
