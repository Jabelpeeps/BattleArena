package mc.alk.arena.objects.joining;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.options.JoinOptions;
import mc.alk.arena.objects.teams.ArenaTeam;

public class TeamJoinObject extends QueueObject{
	@Getter final ArenaTeam team;

	public TeamJoinObject(ArenaTeam _team, MatchParams params, JoinOptions options) {
		super(options, params);
		team = _team;
		priority = _team.getPriority();
		numPlayers += _team.size();
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
	public boolean hasTeam(ArenaTeam team) {
		if (this.team.getId() == team.getId())
			return true;
		for (ArenaPlayer ap : this.team.getPlayers()){
			if (team.hasMember(ap)){
				return true;
			}
		}
		return false;
	}

	public boolean hasStartPerms() { return false; }
}
