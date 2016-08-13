package mc.alk.arena.events.events;

import lombok.Getter;
import mc.alk.arena.competition.AbstractComp;
import mc.alk.arena.objects.MatchResult;

public class EventResultEvent extends EventEvent {
	@Getter final MatchResult result;
	public EventResultEvent(AbstractComp event, MatchResult _result) {
		super(event);
		result = _result;
	}
}
