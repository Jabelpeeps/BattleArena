package mc.alk.arena.events.matches;

import java.util.List;

import lombok.Getter;
import mc.alk.arena.competition.Match;
import mc.alk.arena.objects.teams.ArenaTeam;

public class MatchPrestartEvent extends MatchEvent {
	@Getter final List<ArenaTeam> teams;

	public MatchPrestartEvent(Match match, List<ArenaTeam> _teams) {
		super(match);
		teams = _teams;
	}
}
