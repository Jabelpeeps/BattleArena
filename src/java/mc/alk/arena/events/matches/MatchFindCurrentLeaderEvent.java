package mc.alk.arena.events.matches;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.competition.Match;
import mc.alk.arena.objects.MatchResult;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.objects.tracker.WLTRecord.WLT;

public class MatchFindCurrentLeaderEvent extends MatchEvent {
    @Getter final List<ArenaTeam> teams;
	@Getter @Setter MatchResult result = new MatchResult();
	@Getter final boolean matchEnding;

    public MatchFindCurrentLeaderEvent(Match match) {
        this(match, match.getTeams(), false);
    }
    public MatchFindCurrentLeaderEvent(Match match, List<ArenaTeam> _teams) {
        this(match, _teams, false);
    }
	public MatchFindCurrentLeaderEvent(Match match, List<ArenaTeam> _teams, boolean _matchEnding) {
		super(match);
		teams = _teams;
		matchEnding = _matchEnding;
	}
	
	public Set<ArenaTeam> getCurrentLeaders() {
		return result.getVictors();
	}
	public void setCurrentLeader(ArenaTeam currentLeader) {
		result.setVictor(currentLeader);
		result.setResult(WLT.WIN);
	}
	public void setCurrentLeaders(Collection<ArenaTeam> currentLeaders) {
		result.setVictors(currentLeaders);
		result.setResult(WLT.WIN);
	}
	public void setCurrentDrawers(Collection<ArenaTeam> currentLeaders) {
		result.setDrawers(currentLeaders);
		result.setResult(WLT.TIE);
	}
	public void setCurrentLosers(Collection<ArenaTeam> currentLosers) {
		result.setLosers(currentLosers);
	}
    public SortedMap<Integer,Collection<ArenaTeam>> getRanking() {
        return result.getRanking();
    }
}
