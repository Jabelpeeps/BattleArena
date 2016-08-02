package mc.alk.arena.controllers.plugins;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.LibsDisguises;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;

public class DisguiseController {

    private static boolean enabled = false;

    public static void setLibsDisguise(Plugin plugin) {
        if ( plugin == null || !(plugin instanceof LibsDisguises) ) return;
        
        enabled = true;
    }

    public static boolean enabled() {
		return enabled;
	}

	public static void undisguise(Player player) {
		if ( !enabled ) return;
		
		DisguiseAPI.undisguiseToAll(player);
	}

	public static void disguisePlayer(Player player, String disguise) {
		if ( !enabled ) return;
		
        Disguise dis;
        DisguiseType d = null;

        d = DisguiseType.valueOf( disguise.toUpperCase() );

        if ( d != null ) {
            dis = new MobDisguise( d, false );
        } else {
            dis = new PlayerDisguise( disguise );
        }

        DisguiseAPI.disguiseToAll( player,dis );
	}
}
