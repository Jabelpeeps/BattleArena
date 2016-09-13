package mc.alk.arena.controllers;

import org.bukkit.entity.Player;

import mc.alk.arena.Defaults;
import mc.alk.arena.util.Log;
import net.milkbowl.vault.economy.Economy;

public class MoneyController {
	static boolean initialized = false;
	public static Economy economy = null;

	public static boolean hasEconomy() {
		return initialized;
	}

	public static boolean hasEnough( Player player, double fee ) {
	    if ( !initialized ) return true;
	    
	    return economy.has( player, Math.abs( fee ) );	    
	}

	public static void subtract( Player player, double amount ) {
	    if ( !initialized ) return;
	    
	    economy.withdrawPlayer( player, Math.abs( amount ) );
	}

	public static void add( Player player, double amount ) {
	    if ( !initialized ) return;
	    
	    economy.depositPlayer( player, Math.abs( amount ) );
	}
	
	public static void setEconomy(Economy econ) {
		economy = econ;
		initialized = true;
		try {
			String cur = econ.currencyNameSingular();
			if (cur == null || cur.isEmpty()){
				Log.warn("[BattleArena] Warning currency was empty, using name from config.yml");
			} else {
				Defaults.MONEY_STR = cur;
				Defaults.MONEY_SET = true;
			}
		} catch (Throwable e){
			Log.err("[BattleArena] Error setting currency name through vault. Defaulting to BattleArena/config.yml");
			Log.err("[BattleArena] Error was '" + e.getMessage()+"'");
		}
	}
}
