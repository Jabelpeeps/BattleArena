package mc.alk.arena.objects.joining;

import java.util.List;

import lombok.Getter;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.Matchup;
import mc.alk.arena.objects.teams.ArenaTeam;

public class MatchTeamQObject extends QueueObject {
	@Getter final Matchup matchup;

	public MatchTeamQObject(Matchup _matchup){
		super( _matchup.getJoinOptions() );
		matchup = _matchup;
		priority = _matchup.getPriority();
		for ( ArenaTeam t : _matchup.getTeams() ) {
			numPlayers += t.size();
		}
        listeners = _matchup.getArenaListeners();
    }
	
	@Override
	public boolean hasMember(ArenaPlayer p) { return matchup.hasMember(p); }
	@Override
	public ArenaTeam getTeam(ArenaPlayer p) { return matchup.getTeam(p); }
	@Override
	public int size() { return matchup.size(); }
	@Override
	public String toString() { return priority + " " + matchup.toString(); }
	@Override
	public List<ArenaTeam> getTeams() { return matchup.getTeams(); }
	@Override
	public boolean hasTeam(ArenaTeam team) {
		List<ArenaTeam> teams = matchup.getTeams();
		return teams != null && teams.contains(team);
	}
}
