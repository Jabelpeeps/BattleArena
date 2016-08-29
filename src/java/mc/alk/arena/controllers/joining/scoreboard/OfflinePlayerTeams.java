package mc.alk.arena.controllers.joining.scoreboard;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/**
 * @author alkarin
 */
class OfflinePlayerTeams {

    static HashMap<String, OfflinePlayer> map = new HashMap<>();

    static OfflinePlayer getOfflinePlayer(String name) {
        OfflinePlayer op = map.get(name);
        
        if ( op == null ) {
            op = Bukkit.getPlayerExact( name );
            
            if ( op == null ) return null;
        }
        map.put(name, op);
        
        return op;
    }

}
