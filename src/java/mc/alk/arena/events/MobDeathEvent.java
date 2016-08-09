package mc.alk.arena.events;

import org.bukkit.event.entity.EntityDeathEvent;

import mc.alk.arena.objects.ArenaPlayer;

public class MobDeathEvent {
	final ArenaPlayer killer;
	protected EntityDeathEvent event;
	
	public MobDeathEvent(EntityDeathEvent _event, ArenaPlayer _killer){
		event = _event;
		killer = _killer;
	}

	public EntityDeathEvent getBukkitEvent() {
		return event;
	}

	public ArenaPlayer getPlayer(){
		return killer;
	}
}
