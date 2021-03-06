package mc.alk.arena.objects.joining;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import mc.alk.arena.Defaults;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.options.JoinOptions;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.util.Log;

public class TeamJoinObject extends QueueObject {
	@Getter final ArenaTeam team;

	public TeamJoinObject(ArenaTeam _team, MatchParams params, JoinOptions options) {
		super(options, params);
		team = _team;
		priority = _team.getPriority();
		numPlayers += _team.size();
		
		if ( Defaults.DEBUG_TRANSITIONS )
		    Log.info( " new TeamJoinObject:- " + toString() );
	}

	@Override
	public boolean hasMember(ArenaPlayer p) { return team.hasMember(p); }
	@Override
	public ArenaTeam getTeam(ArenaPlayer p) { return team.hasMember(p) ? team : null; }
	@Override
	public int size() { return team.size(); }
	@Override
	public String toString() { return team.getPriority() + " " + team.toString() + ":" + team.getId(); }

	@Override
	public List<ArenaTeam> getTeams() {
		ArrayList<ArenaTeam> teams = new ArrayList<>(1);
		teams.add(team);
		return teams;
	}

	@Override
	public boolean hasTeam( ArenaTeam _team ) {
		if ( team.getId() == _team.getId() ) return true;
		
		for ( ArenaPlayer ap : team.getPlayers() ) {
			if (_team.hasMember(ap) ) {
				return true;
			}
		}
		return false;
	}
}
