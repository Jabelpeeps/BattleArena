package mc.alk.arena.objects.tracker;

import java.util.ArrayList;

import org.bukkit.OfflinePlayer;

public class PlayerStat extends TeamStat {

	public PlayerStat(OfflinePlayer player) {
		this(player.getName());
	}

	public PlayerStat(String player) {
		name = strID = player;
		members = new ArrayList<>();
		members.add(player);
		count = 1;
	}
}
