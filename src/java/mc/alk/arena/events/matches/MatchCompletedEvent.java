package mc.alk.arena.events.matches;

import mc.alk.arena.competition.Match;

public class MatchCompletedEvent extends MatchEvent {
	public MatchCompletedEvent(Match match){
		super(match);
	}
}
