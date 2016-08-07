package mc.alk.util.version;


import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Handles the construction of new Version objects specific to BUKKIT & SPONGE.
 *
 * @author Nikolai
 */
public class Platform {

    public static Version<Plugin> getPluginVersion(String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        Tester<Plugin> tester = TesterFactory.getNewTester(plugin);
        String version = (plugin == null) ? "" : plugin.getDescription().getVersion();
        
        return new Version<Plugin>(version, tester);
    }

    public static Version getMinecraftVersion() {
        String version = Bukkit.getServer().getBukkitVersion();
        return new Version(version);
    }

    public static Version getApiVersion() {
        String version = Bukkit.getServer().getBukkitVersion();
        return new Version(version);
    }

    public static Version getImplementationVersion() {
        String version = Bukkit.getServer().getVersion();
        return new Version(version);
    }

    public static String getNmsPackage() {
 
        return Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
    }
    
    public String name() {
        return "bukkit";
    }

 

 
  
}
