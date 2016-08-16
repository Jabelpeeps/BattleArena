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
import mc.alk.tracker.controllers.ConfigController;
import mc.alk.tracker.controllers.MessageController;
import mc.alk.tracker.controllers.SignController;
import mc.alk.tracker.controllers.TrackerImpl;
import mc.alk.tracker.executors.BattleTrackerExecutor;
import mc.alk.tracker.executors.TrackerExecutor;
import mc.alk.tracker.listeners.BTEntityListener;
import mc.alk.tracker.listeners.BTPluginListener;
import mc.alk.tracker.listeners.SignListener;
import mc.alk.tracker.objects.StatSign;
import mc.alk.tracker.serializers.SignSerializer;
import mc.alk.tracker.serializers.YamlConfigUpdater;
import mc.alk.tracker.serializers.YamlMessageUpdater;

public class Tracker {

    static JavaPlugin BA;
    final static Map<String, TrackerInterface> interfaces = Collections.synchronizedMap( new ConcurrentHashMap<>() );
    static SignController signController = new SignController();
    static SignSerializer signSerializer;

    static {
        BA = BattleArena.getSelf();
        File dir = BA.getDataFolder();
        if (!dir.exists())
            dir.mkdirs();
        
        ConfigurationSerialization.registerClass( StatSign.class );
        loadConfigs();

        Bukkit.getPluginManager().registerEvents(new BTEntityListener(), BA);
        Bukkit.getPluginManager().registerEvents(new BTPluginListener(), BA);

        BA.getCommand("battleTracker").setExecutor(new BattleTrackerExecutor());
        BA.getCommand("btpvp").setExecutor(new TrackerExecutor(getInterface(Defaults.PVP_INTERFACE)));
        BA.getCommand("btpve").setExecutor(new TrackerExecutor(getInterface(Defaults.PVE_INTERFACE)));

        BTPluginListener.loadPlugins();
    }

    public static boolean isEnabled() {
        return !interfaces.isEmpty();
    }
    
    public static void loadConfigs() {
        
        ConfigController.setConfig( load( "/default_files/config.yml", 
                                          BA.getDataFolder().getPath() + "/tracker.yml" ) );
        MessageController.setConfig( load( "/default_files/messages.yml", 
                                           BA.getDataFolder().getPath() + "/tracker_messages.yml" ) );

        Scheduler.scheduleSynchronousTask( () -> {
                signSerializer = new SignSerializer(signController);
                signSerializer.setConfig( BA.getDataFolder().getPath() + "/signs.yml" );
                signSerializer.loadAll();
        }, 22);

        YamlConfigUpdater cu = new YamlConfigUpdater();
        cu.update(ConfigController.getConfig(), ConfigController.getFile());

        YamlMessageUpdater mu = new YamlMessageUpdater();
        mu.update(MessageController.getConfig(), MessageController.getFile());

        ConfigController.loadAll();
        MessageController.load();

        if (Defaults.USE_SIGNS) {
            Bukkit.getPluginManager().registerEvents(new SignListener(signController), BA);
            Bukkit.getScheduler().scheduleSyncRepeatingTask(BA, () -> signController.updateSigns(), 20, 1000);
        }
    }

    public static void saveConfig() {
        synchronized (interfaces) {
            for (TrackerInterface ti : interfaces.values()) {
                ti.flush();
            }
        }
        /// can happen if tracker never loads properly (like starting and immediately stopping)
        if (signSerializer != null) {
            signSerializer.saveAll();
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
            interfaces.put(iname, new TrackerImpl(interfaceName, trackerOptions));
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
