package mc.alk.arena.listeners.competition;

import org.bukkit.event.block.BlockPlaceEvent;

import lombok.AllArgsConstructor;
import mc.alk.arena.listeners.PlayerHolder;
import mc.alk.arena.objects.arenas.ArenaListener;
import mc.alk.arena.objects.events.ArenaEventHandler;
import mc.alk.arena.objects.events.ArenaEventPriority;
import mc.alk.arena.objects.options.TransitionOption;

@AllArgsConstructor
public class BlockPlaceListener implements ArenaListener{
    PlayerHolder holder;

	@ArenaEventHandler( priority = ArenaEventPriority.HIGH) 
	public void onPlayerBlockPlace( BlockPlaceEvent event ) {
		if ( holder.hasOption( TransitionOption.BLOCKPLACEOFF ) )
			event.setCancelled(true);
		else if ( holder.hasOption( TransitionOption.BLOCKPLACEON ) )
			event.setCancelled(false);
	}
}
