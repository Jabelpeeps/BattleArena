package mc.alk.arena.util;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.Selection;
import mc.alk.arena.controllers.plugins.WorldGuardController;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Use mc.alk.arena.util.plugins.WorldEditUtil instead
 * @author alkarin
 *
 */
@Deprecated
public class WorldEditUtil {

    public static boolean hasWorldEdit() {
        return WorldGuardController.hasWorldEdit();
    }

    public static boolean setWorldEdit(Plugin plugin) {
        return WorldGuardController.setWorldEdit(plugin);
    }

    public static Selection getSelection(Player player) {
        return mc.alk.arena.plugins.worldedit.WorldEditUtil.getSelection(player);
    }

    public static WorldEditPlugin getWorldEditPlugin() {
        return mc.alk.arena.plugins.worldedit.WorldEditUtil.getWorldEditPlugin();
    }

}
