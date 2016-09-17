package mc.alk.arena.listeners.competition;

import org.bukkit.event.entity.FoodLevelChangeEvent;

import lombok.AllArgsConstructor;
import mc.alk.arena.listeners.PlayerHolder;
import mc.alk.arena.objects.arenas.ArenaListener;
import mc.alk.arena.objects.events.ArenaEventHandler;
import mc.alk.arena.objects.options.TransitionOption;

@AllArgsConstructor
public class HungerListener implements ArenaListener{
    PlayerHolder holder;

	@ArenaEventHandler
	public void onFoodLevelChangeEvent(FoodLevelChangeEvent event){
		if (holder.hasOption(TransitionOption.HUNGEROFF))
			event.setCancelled(true);
	}
}
