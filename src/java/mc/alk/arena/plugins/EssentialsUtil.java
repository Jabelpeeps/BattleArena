package mc.alk.arena.plugins;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import com.earth2me.essentials.UserMap;

import mc.alk.arena.controllers.PlayerController;
import mc.alk.arena.objects.ArenaPlayer;

public class EssentialsUtil {
	static Essentials essentials;
	static UserMap map;
	
	static {
		essentials = (Essentials) Bukkit.getPluginManager().getPlugin( "Essentials" );	
		if ( essentials != null )
		    map = essentials.getUserMap();
	}
    public static boolean isEnabled() {
        return essentials != null && map != null;
    }
	public static User getUser(ArenaPlayer player){
		return isEnabled() ? map.getUser(player.getName()) : null;
	}
	public static void setGod(ArenaPlayer player, boolean enable) {
	    if ( isEnabled() )
	        getUser(player).setGodModeEnabled(enable);
	}
	public static boolean inJail(ArenaPlayer player) {
		return isEnabled() ? getUser(player).getJailTimeout() > System.currentTimeMillis() : false;
	}
	public static boolean isGod(ArenaPlayer player) {
		return isEnabled() ? getUser(player).isGodModeEnabled() : false;
	}
	public static void setBackLocation(Player player, Location loc) {
        if ( isEnabled() )
            getUser(PlayerController.getArenaPlayer( player )).setLastLocation(loc);
	}
	public static Location getBackLocation(ArenaPlayer player) {
		return isEnabled() ? getUser(player).getLastLocation() : null;
	}
}
