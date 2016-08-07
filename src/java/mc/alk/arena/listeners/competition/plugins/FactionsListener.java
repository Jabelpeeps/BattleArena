package mc.alk.arena.listeners.competition.plugins;


import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.massivecraft.factions.event.EventFactionsPowerChange;

import mc.alk.arena.listeners.competition.InArenaListener;

public enum FactionsListener implements Listener{
	INSTANCE;

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onFactionLoss(EventFactionsPowerChange event){
        Player p = event.getMPlayer().getPlayer(); /// Annoyingly this has been null at times
		if (p != null && InArenaListener.inArena( p.getUniqueId() ) ){
			event.setCancelled(true);}
	}

	public static boolean enable() {
		try {
			Class.forName("com.massivecraft.factions.event.FactionsEventPowerChange");
			InArenaListener.addListener(INSTANCE);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
}
