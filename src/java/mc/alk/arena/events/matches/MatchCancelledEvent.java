package mc.alk.arena.events.matches;

import mc.alk.arena.competition.Match;

public class MatchCancelledEvent extends MatchEvent {
	public MatchCancelledEvent(Match match){
		super(match);
	}
}
