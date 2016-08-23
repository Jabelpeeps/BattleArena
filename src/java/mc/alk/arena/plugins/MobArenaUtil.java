package mc.alk.arena.plugins;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.garbagemule.MobArena.MobArena;
import com.garbagemule.MobArena.framework.ArenaMaster;

public class MobArenaUtil {
    
	static MobArena ma;
	static ArenaMaster master;
	
	static {
		ma = (MobArena) Bukkit.getPluginManager().getPlugin("MobArena");
		master = ma.getArenaMaster();
	}
	
	public static boolean isEnabled() {
	    return ma != null && master != null;
	}
	
	public static boolean insideMobArena(Player player) {
		if ( ma == null ) return false;

		return  master.getArenaWithPlayer(player) != null 
		        || master.getArenaWithSpectator(player) != null;
	}
}
