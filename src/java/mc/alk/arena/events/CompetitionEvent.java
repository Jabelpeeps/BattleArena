package mc.alk.arena.events;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.competition.Competition;

public class CompetitionEvent extends BAEvent {
	@Getter @Setter protected Competition competition;

	public CompetitionEvent() {}
	
	public CompetitionEvent( Competition _competition ) {
	    competition = _competition;
	}
}
