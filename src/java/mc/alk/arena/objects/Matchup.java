package mc.alk.arena.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.competition.Match;
import mc.alk.arena.controllers.ParamController;
import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.objects.arenas.ArenaListener;
import mc.alk.arena.objects.options.JoinOptions;
import mc.alk.arena.objects.teams.ArenaTeam;


public class Matchup {
	static int count = 0;
	final int id = count++; 

	@Setter @Getter public MatchResult result = new MatchResult();
	@Getter public List<ArenaTeam> teams = new ArrayList<>();

	@Getter List<ArenaListener> arenaListeners = new ArrayList<>();

	@Getter MatchParams matchParams = null;
	@Getter Match match = null;
	@Getter final JoinOptions joinOptions;

	public Matchup(MatchParams _params, ArenaTeam team, ArenaTeam team2, JoinOptions _joinOptions) {
		matchParams = _params;
		teams.add(team);
		teams.add(team2);
		joinOptions = _joinOptions;
	}

	public Matchup(MatchParams _params, Collection<ArenaTeam> _teams, JoinOptions _joinOptions) {
		teams = new ArrayList<>(_teams);
		matchParams = ParamController.copyParams(_params);
		joinOptions = _joinOptions;
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for (ArenaTeam t: teams){
			sb.append("t=").append(t).append(",");
		}
		return sb.toString() + " result=" + result;
	}

	public ArenaTeam getTeam(int i) {
		return teams.get(i);
	}

	@Override
	public boolean equals(Object other) {
        return this == other ||
                other instanceof Matchup &&
                        this.hashCode() == other.hashCode();
    }

	@Override
	public int hashCode() { return id;}

	public void addArenaListener(ArenaListener transitionListener) {
		arenaListeners.add(transitionListener);
	}

	public void addMatch(Match _match) {
		match = _match;
	}

	public Integer getPriority() {
		Integer priority = Integer.MAX_VALUE;
		for (ArenaTeam t: teams){
			if (t.getPriority() < priority){
				priority = t.getPriority();}
		}
		return priority;
	}
	public boolean hasMember(ArenaPlayer p) {
		for (ArenaTeam t: teams){
			if (t.hasMember(p))
				return true;
		}
		return false;
	}
	public ArenaTeam getTeam(ArenaPlayer p) {
		for (ArenaTeam t: teams){
			if (t.hasMember(p))
				return t;
		}
		return null;
	}
	public int size() {
		int size = 0;
		for (ArenaTeam t: teams){
			size += t.size();
		}
		return size;
	}

	public Arena getArena() { return joinOptions.getArena(); }
	public void setArena(Arena arena) { joinOptions.setArena(arena); }

}
