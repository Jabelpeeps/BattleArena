package mc.alk.arena.events.events;

import mc.alk.arena.competition.AbstractComp;

public class EventFinishedEvent extends EventEvent {
	public EventFinishedEvent(AbstractComp event){
		super(event);
	}
}
