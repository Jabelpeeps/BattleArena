package mc.alk.arena.util;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import mc.alk.arena.Defaults;
import mc.alk.virtualplayers.VirtualPlayers;


public abstract class ServerUtil {

    public static HashMap<String, OfflinePlayer> playerMap = new HashMap<>();
    
    public static Player findPlayer( UUID id ) {
        
        if ( id != null ) {
            Player player = Bukkit.getPlayer( id );
            if ( player != null )
                return player;
            
            if ( Defaults.DEBUG_VIRTUAL )
                return VirtualPlayers.getPlayer(id);
        }
        return null;
    }

	public static Player findPlayer(String name) {
		if (name == null) return null;
		
		Player foundPlayer = Bukkit.getPlayer(name);
        if (foundPlayer != null) 
            return foundPlayer;
        
        if (Defaults.DEBUG_VIRTUAL){
            foundPlayer = VirtualPlayers.getPlayer(name);
            
            if (foundPlayer != null) return foundPlayer;
        }
        return null;
	}

    @Deprecated
	public static OfflinePlayer findOfflinePlayer(String name) {
		OfflinePlayer p = findPlayer(name);
		if ( p != null ) {
			return p;
		}
        /// Iterate over the worlds to see if a player.dat file exists
        for ( World w : Bukkit.getWorlds() ) {
        	File f = new File( w.getName() + "/players/" + name + ".dat" );
        	if ( f.exists() ) {
        		return Bukkit.getOfflinePlayer(name);
        	}
        }
        return null;
	}

	private static Collection<? extends Player> getOnlinePlayers() {
		if ( Defaults.DEBUG_VIRTUAL ) {
			return Arrays.asList( VirtualPlayers.getOnlinePlayers() );
		}
        return Bukkit.getOnlinePlayers();
	}

	public static void findOnlinePlayers(Set<String> names, Set<Player> foundplayers, Set<String> unfoundplayers) {
		Collection<? extends Player> online = getOnlinePlayers();
		
		for ( String name : names ) {
			Player lastPlayer = null;
			
			for ( Player player : online ) {
				String playerName = player.getName();
				
				if ( playerName.equalsIgnoreCase(name) ) {
					lastPlayer = player;
					break;
				}

				if ( playerName.toLowerCase().indexOf( name.toLowerCase(),0 ) != -1 ) { /// many names match the one given
					if ( lastPlayer != null ) {
						lastPlayer = null;
						break;
					}
					lastPlayer = player;
				}
			}
			if ( lastPlayer != null )
				foundplayers.add( lastPlayer );
			else
				unfoundplayers.add( name );
		}
	}

    public static OfflinePlayer getOfflinePlayer(String name) {
        OfflinePlayer op = playerMap.get(name);
        
        if ( op == null ) {
            op = Bukkit.getPlayerExact( name );
            
            if ( op == null ) return null;
        }
        playerMap.put(name, op);
        
        return op;
    }
}
