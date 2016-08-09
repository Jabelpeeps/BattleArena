package mc.alk.arena.events.matches;

import mc.alk.arena.competition.Match;

public class MatchPlayersReadyEvent extends MatchEvent {
	public MatchPlayersReadyEvent(Match match){
		super(match);
	}
}
