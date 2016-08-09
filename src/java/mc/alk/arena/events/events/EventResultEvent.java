package mc.alk.arena.events.events;

import mc.alk.arena.competition.AbstractComp;
import mc.alk.arena.objects.CompetitionResult;

public class EventResultEvent extends EventEvent {
	final CompetitionResult result;
	public EventResultEvent(AbstractComp event, CompetitionResult result) {
		super(event);
		this.result = result;
	}

	public CompetitionResult getResult(){
		return result;
	}
}
