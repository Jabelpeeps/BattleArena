package mc.alk.arena.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import mc.alk.arena.competition.Match;
import mc.alk.arena.objects.teams.ArenaTeam;

public class VictoryUtil {
	static final Random rand = new Random();

	public static List<ArenaTeam> getLeaderByHighestKills(Match match){
		List<ArenaTeam> teams = match.getTeams();
		List<ArenaTeam> victors = getLeaderByHighestKills(teams);
		if (victors.size() > 1){ /// try to tie break by number of deaths
			victors = getLeaderByLeastDeaths(victors);
		}
		return victors;
	}

	public static List<ArenaTeam> getLeaderByHighestKills(List<ArenaTeam> teams){
		int highest = Integer.MIN_VALUE;
		List<ArenaTeam> victors = new ArrayList<>();
		for (ArenaTeam t: teams){
			int nkills = t.getNKills();
			if (nkills == highest){ /// we have some sort of tie
				victors.add(t);}
			if (nkills > highest){
				victors.clear();
				highest = nkills;
				victors.add(t);
			}
		}
		return victors;
	}

	public static List<ArenaTeam> getLeaderByLeastDeaths(List<ArenaTeam> teams){
		int lowest = Integer.MAX_VALUE;
		List<ArenaTeam> result = new ArrayList<>();
		for (ArenaTeam t: teams){
			final int ndeaths = t.getNDeaths();
			if (ndeaths == lowest){ /// we have some sort of tie
				result.add(t);}
			if (ndeaths < lowest){
				result.clear();
				lowest = ndeaths;
				result.add(t);
			}
		}
		return result;
	}

	public static List<ArenaTeam> getRanksByHighestKills(List<ArenaTeam> teams) {
		ArrayList<ArenaTeam> ts = new ArrayList<>(teams);
		Collections.sort( ts, 
		        ( arg0, arg1 ) -> {
        				int c = Integer.compare( arg0.getNKills(), arg1.getNKills() );
        				return c != 0 ? -c 
        				              : Integer.compare( arg0.getNDeaths(), arg1.getNDeaths() );
        		});
		return ts;
	}

	public static TreeMap<Integer, Collection<ArenaTeam>> getRankingByHighestKills(List<ArenaTeam> teams) {
		TreeMap<Integer,Collection<ArenaTeam>> map = new TreeMap<>( Collections.reverseOrder() );
		for (ArenaTeam t: teams){
			Collection<ArenaTeam> col = map.get(t.getNKills());
			if (col == null){
				col = new ArrayList<>();
				map.put(t.getNKills(), col);
			}
			col.add(t);
		}
		return map;
	}

}
