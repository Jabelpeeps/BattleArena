package mc.alk.arena.plugins;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.Selection;

import mc.alk.arena.util.Log;
import mc.alk.arena.util.VersionFactory;

/**
 * Stub class for future expansion
 *
 * @author alkarin
 *
 */
public class WorldEditUtil {

    public static WorldEditPlugin wep;
    
    static {
        checkIfLoadedYet();
    }

    public static boolean checkIfLoadedYet() {
        wep = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");

        if ( !VersionFactory.getPluginVersion( "WorldEdit" ).isCompatible( "6" ) ) {
            wep = null;
            Log.warn( "WorldEdit version 6 is the minimum that is supported" );
        }
        return hasWorldEdit();
    }
    
    public static boolean hasWorldEdit() {
        return wep != null;
    }

    public static Selection getSelection(Player player) {
        return wep != null ? wep.getSelection(player)
                           : null;
    }

    public static WorldEditPlugin getWorldEditPlugin() {
        return wep;
    }
    
}
