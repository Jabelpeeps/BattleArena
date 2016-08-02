package mc.alk.arena.controllers.plugins;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import mc.alk.arena.util.DisguiseUtil;
import mc.alk.arena.util.plugins.LibsDisguiseUtil;

public class DisguiseInterface {
	public static final int DEFAULT = Integer.MAX_VALUE;

    private static DisguiseUtil handler;

    public static void setLibsDisguise(Plugin plugin){
        handler = LibsDisguiseUtil.setPlugin(plugin);
    }

    public static boolean enabled(){
		return handler != null;
	}

	public static void undisguise(Player player) {
		if (!enabled()) return;
		handler.undisguise(player);
	}

	public static void disguisePlayer(Player player, String disguise) {
		if (!enabled()) return;
        handler.disguisePlayer(player, disguise);
	}

}
