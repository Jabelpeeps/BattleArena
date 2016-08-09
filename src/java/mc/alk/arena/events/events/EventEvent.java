package mc.alk.arena.events.events;

import mc.alk.arena.competition.AbstractComp;
import mc.alk.arena.events.CompetitionEvent;

public class EventEvent extends CompetitionEvent{
	public EventEvent(AbstractComp event) {
		super( event );
	}

	/**
	 * Returns the match for this event
	 * @return Match
	 */
	public AbstractComp getEvent() {
		return (AbstractComp) getCompetition();
	}
}
