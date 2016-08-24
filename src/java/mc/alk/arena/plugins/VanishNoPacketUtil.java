package mc.alk.arena.plugins;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.kitteh.vanish.VanishManager;
import org.kitteh.vanish.VanishPlugin;

public class VanishNoPacketUtil {
	static VanishPlugin vanish;
	static VanishManager manager;

	static {
		vanish = (VanishPlugin) Bukkit.getPluginManager().getPlugin("VanishNoPacket");
		if ( vanish != null )
		    manager = vanish.getManager();
	}
	public static boolean isEnabled() {
	    return vanish != null && manager != null;
	}
	public static boolean isVanished(Player player) {
		return manager.isVanished(player);
	}
	public static void toggleVanish(Player player) {
	    manager.toggleVanish(player);
	}
}
