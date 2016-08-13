package mc.alk.arena.events.matches;

import lombok.Getter;
import mc.alk.arena.competition.Match;
import mc.alk.arena.objects.MatchState;

public class MatchFinishedEvent extends MatchEvent {
	@Getter final MatchState state;

	public MatchFinishedEvent(Match match){
		super(match);
		state = match.getState();
	}
}
