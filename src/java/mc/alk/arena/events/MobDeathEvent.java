package mc.alk.arena.events;

import org.bukkit.event.entity.EntityDeathEvent;

import lombok.AllArgsConstructor;
import mc.alk.arena.objects.ArenaPlayer;

@AllArgsConstructor
public class MobDeathEvent {
    protected EntityDeathEvent event;
	final ArenaPlayer killer;

	public EntityDeathEvent getBukkitEvent() {
		return event;
	}
	public ArenaPlayer getPlayer(){
		return killer;
	}
}
