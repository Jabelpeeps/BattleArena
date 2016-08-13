package mc.alk.arena.events.events;

import org.bukkit.event.Cancellable;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.competition.AbstractComp;

public class EventOpenEvent extends EventEvent implements Cancellable {
	@Getter @Setter boolean cancelled = false;

	public EventOpenEvent(AbstractComp event){
		super(event);
	}
}
