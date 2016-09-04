package mc.alk.arena.listeners.competition;

import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;

import mc.alk.arena.objects.arenas.ArenaListener;
import mc.alk.arena.objects.events.ArenaEventHandler;

public class PreClearInventoryListener implements ArenaListener {

	@ArenaEventHandler( bukkitPriority = EventPriority.LOWEST )
	public void onPlayerDeath( PlayerDeathEvent event ) {
        event.getDrops().clear();
    }
}
