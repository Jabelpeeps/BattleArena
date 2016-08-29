package mc.alk.arena.controllers.joining.scoreboard;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/**
 * @author alkarin
 */
class OfflinePlayerTeams {
//    static LoadingCache<String, OfflinePlayer> map = CacheBuilder.newBuilder()
//            .expireAfterAccess(10, TimeUnit.MINUTES)
//            .build(
//                    new CacheLoader<String, OfflinePlayer>() {
//                        @Override
//                        public OfflinePlayer load(String key) { // no checked exception
//                            return Bukkit.getOfflinePlayer(key);
//                        }
//                    });
//

    static HashMap<String, OfflinePlayer> map = new HashMap<>();

    static OfflinePlayer getOfflinePlayer(String name) {
        OfflinePlayer op = map.get(name);
        if (op == null) {
            op = Bukkit.getPlayer( name );
            map.put(name, op);
        }
        return op;
    }

}
