package mc.alk.arena.objects.messaging;

import org.bukkit.World;
import org.bukkit.entity.Player;

import mc.alk.util.MessageUtil;

public class WorldChannel implements Channel {
	final World world;
	WorldChannel(World world){
		this.world = world;
	}

	@Override
	public void broadcast(String msg) {
		if (msg == null || msg.trim().isEmpty())
			return;
		msg = MessageUtil.colorChat(msg);
		for (Player p: world.getPlayers()){
			p.sendMessage(msg);
		}
	}
}
