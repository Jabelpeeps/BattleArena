package mc.alk.arena.events.events;

import org.bukkit.event.Cancellable;

import mc.alk.arena.competition.AbstractComp;

public class EventOpenEvent extends EventEvent implements Cancellable {
	/// Cancel status
	boolean cancelled = false;

	public EventOpenEvent(AbstractComp event){
		super(event);
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
}
