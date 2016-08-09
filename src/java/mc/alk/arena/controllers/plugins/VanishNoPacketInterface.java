package mc.alk.arena.controllers.plugins;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import mc.alk.arena.plugins.VanishNoPacketUtil;

public class VanishNoPacketInterface {
	private static boolean enabled = false;

	public static void setPlugin(Plugin plugin) {
		VanishNoPacketUtil.setPlugin(plugin);
		enabled = true;
	}

	public static boolean enabled() {
		return enabled;
	}

	public static boolean isVanished(Player player) {
		return enabled && VanishNoPacketUtil.isVanished(player);
	}

	public static void toggleVanish(Player player) {
		if (!enabled)
			return;
		VanishNoPacketUtil.toggleVanish(player);
	}
}
