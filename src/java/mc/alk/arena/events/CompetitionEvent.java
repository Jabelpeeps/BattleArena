package mc.alk.arena.events;

import mc.alk.arena.competition.Competition;

public class CompetitionEvent extends BAEvent {
	protected Competition competition;

	public CompetitionEvent() {}
	
	public CompetitionEvent( Competition _competition ) {
	    competition = _competition;
	}
	
	public void setCompetition(Competition _competition){
		competition = _competition;
	}

	public Competition getCompetition(){
		return competition;
	}
}
