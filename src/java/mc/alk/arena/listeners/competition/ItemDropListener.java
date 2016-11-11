package mc.alk.arena.listeners.competition;

import org.bukkit.event.player.PlayerDropItemEvent;

import lombok.AllArgsConstructor;
import mc.alk.arena.listeners.PlayerHolder;
import mc.alk.arena.objects.arenas.ArenaListener;
import mc.alk.arena.objects.events.ArenaEventHandler;
import mc.alk.arena.objects.events.ArenaEventHandler.ArenaEventPriority;
import mc.alk.arena.objects.options.TransitionOption;

@AllArgsConstructor
public class ItemDropListener implements ArenaListener{
    final PlayerHolder holder;

	@ArenaEventHandler( priority = ArenaEventPriority.HIGH )
	public void onPlayerDropItem( PlayerDropItemEvent event ) {
		if ( holder.hasOption( TransitionOption.ITEMDROPOFF ) )
			event.setCancelled( true );
	}
}
