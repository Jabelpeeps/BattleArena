package mc.alk.arena.events.events;

import mc.alk.arena.competition.AbstractComp;

public class EventCompletedEvent extends EventEvent {
	public EventCompletedEvent(AbstractComp event){
		super(event);
	}
}
