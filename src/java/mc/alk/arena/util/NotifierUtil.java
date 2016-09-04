package mc.alk.arena.util;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.bukkit.entity.Player;

public class NotifierUtil {
    public static Map<String,Set<UUID>> listeners = new ConcurrentHashMap<>();

	public static void notify(String type, String msg) {
        if (listeners.get(type)== null)
            return;
		for (UUID name: listeners.get(type)){
			Player p = ServerUtil.findPlayer(name);
			if (p== null || !p.isOnline())
				continue;
			MessageUtil.sendMessage(p, msg);
		}
	}

	public static void notify(String type, Throwable exception) {
        if (listeners.get(type)== null)
			return;
		StringBuilder sb = new StringBuilder();
		for (StackTraceElement e: exception.getStackTrace()){
			sb.append(e.toString());
		}
		String msg = sb.toString();
        for (UUID name: listeners.get(type)){
			Player p = ServerUtil.findPlayer(name);
			if (p== null || !p.isOnline())
				continue;
			MessageUtil.sendMessage(p, msg);
		}
	}

	public static void addListener(Player player, String type) {
		Set<UUID> players = listeners.get(type);
		if (players == null){
			players = new CopyOnWriteArraySet<>();
			listeners.put(type, players);
		}
		players.add( player.getUniqueId() );

	}

	public static void removeListener(Player player, String type) {
		Set<UUID> players = listeners.get(type);
		if (players != null){
			players.remove( player.getUniqueId() );
			if (players.isEmpty())
				listeners.remove(type);
		}
	}

	public static boolean hasListener(String type) {
		return listeners.containsKey(type) && !listeners.get(type).isEmpty();
	}
}
