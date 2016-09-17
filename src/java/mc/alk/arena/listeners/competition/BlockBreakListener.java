package mc.alk.arena.listeners.competition;

import org.bukkit.event.block.BlockBreakEvent;

import lombok.AllArgsConstructor;
import mc.alk.arena.listeners.PlayerHolder;
import mc.alk.arena.objects.arenas.ArenaListener;
import mc.alk.arena.objects.events.ArenaEventHandler;
import mc.alk.arena.objects.events.ArenaEventPriority;
import mc.alk.arena.objects.options.TransitionOption;

@AllArgsConstructor
public class BlockBreakListener implements ArenaListener{
    PlayerHolder holder;

	@ArenaEventHandler( priority = ArenaEventPriority.HIGH )
	public void onPlayerBlockBreak( BlockBreakEvent event ) {
        if (holder.hasOption(TransitionOption.BLOCKBREAKOFF)) 
            event.setCancelled(true);
        else if (holder.hasOption(TransitionOption.BLOCKBREAKON))
            event.setCancelled(false);
    }
}
