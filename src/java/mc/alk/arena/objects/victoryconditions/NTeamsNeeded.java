package mc.alk.arena.objects.victoryconditions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import mc.alk.arena.competition.Match;
import mc.alk.arena.events.teams.TeamDeathEvent;
import mc.alk.arena.objects.MatchResult;
import mc.alk.arena.objects.events.ArenaEventHandler;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.objects.victoryconditions.interfaces.DefinesNumTeams;
import mc.alk.arena.util.MinMax;

public class NTeamsNeeded extends VictoryCondition implements DefinesNumTeams{
	@Getter MinMax neededNumberOfTeams;

	public NTeamsNeeded(Match _match, int nTeams) {
		super(_match);
		neededNumberOfTeams = new MinMax(nTeams);
	}

    @ArenaEventHandler
	public void onTeamDeathEvent(TeamDeathEvent event) {
		/// Killing this player killed the team
		List<ArenaTeam> leftAlive = new ArrayList<>(neededNumberOfTeams.min+1);
		/// Iterate over the players to see if we have one team left standing
		for (ArenaTeam t: match.getTeams()){
			if (t.isDead())
				continue;
			leftAlive.add(t);
			if (leftAlive.size() >= neededNumberOfTeams.min){ ///more than enough teams still in the match
				return;}
		}
		if (leftAlive.isEmpty()){
			match.setLosers();
			return;
		}
		if (leftAlive.size() < neededNumberOfTeams.min){
			MatchResult mr = new MatchResult();
			mr.setVictors(leftAlive);
			Set<ArenaTeam> losers = new HashSet<>(match.getTeams());
			losers.removeAll(leftAlive);
			mr.setLosers(losers);
			match.endMatchWithResult(mr);
		}
	}

    @Override
    public String toString(){
        return "[VC " + getClass().getSimpleName() + " : " + id + " nTeams=" + neededNumberOfTeams + "]" ;
    }
}
