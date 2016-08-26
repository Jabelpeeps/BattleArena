package mc.alk.arena.objects.scoreboard;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

import org.bukkit.OfflinePlayer;

import mc.alk.arena.Defaults;
import mc.alk.arena.competition.Match;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchResult;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.objects.victoryconditions.interfaces.ScoreTracker;
import mc.alk.arena.scoreboardapi.BObjective;
import mc.alk.arena.scoreboardapi.SAPIDisplaySlot;
import mc.alk.arena.scoreboardapi.SAPIFactory;
import mc.alk.arena.scoreboardapi.SEntry;
import mc.alk.arena.scoreboardapi.SObjective;
import mc.alk.arena.scoreboardapi.SScoreboard;
import mc.alk.arena.scoreboardapi.STeam;
import mc.alk.arena.util.ScoreMap;


public class ArenaObjective implements SObjective, ScoreTracker{

    final protected ScoreMap<ArenaTeam> teamPoints = new ScoreMap<>();
    final protected ScoreMap<ArenaPlayer> playerPoints = new ScoreMap<>();
    final protected SObjective objective;

	public ArenaObjective(String name, String criteria) {
		this( name, criteria, name, SAPIDisplaySlot.SIDEBAR, 50, 0 );
	}
	/**
	 *
	 * @param name Objective name
	 * @param criteria Objective criteria
	 * @param priority: lower priority means it has precedence
	 */
	public ArenaObjective(String name, String criteria, int priority) {
		this( name, criteria, name, SAPIDisplaySlot.SIDEBAR, priority, 0 );
	}
	public ArenaObjective(String name, String criteria, String displayName, SAPIDisplaySlot slot) {
		this( name, criteria, displayName, slot, 50, 0 );
	}
	public ArenaObjective(String name, String criteria, String displayName, SAPIDisplaySlot slot, int priority) {
		this( name, criteria, displayName, slot, priority, 0 );
	}

	public ArenaObjective(String id, String criteria, String displayName, SAPIDisplaySlot slot, int priority, int points) {

		objective = (Defaults.TESTSERVER 
		        || !Defaults.USE_SCOREBOARD) ? SAPIFactory.createSAPIObjective( id, displayName, criteria, slot, priority ) 
		                                     : SAPIFactory.createObjective( id, displayName, criteria, slot, priority );
		if (displayName != null){
			setDisplayName(displayName);}
	}

	public void setDisplaySlot(ArenaDisplaySlot sidebar) { objective.setDisplaySlot(sidebar.toSAPI()); }
	public Integer getPoints(ArenaTeam t) { return teamPoints.get(t); }

	public void setAllPoints(int points) {
		for (ArenaTeam t: teamPoints.keySet()){
			setPoints(t, points);
		}
		for (ArenaPlayer p: playerPoints.keySet()){
			setPoints(p, points);
		}
	}

	public void setAllPoints(Match match, int points){
		for (ArenaTeam t: match.getTeams()){
			if (objective.isDisplayTeams()){
				setPoints(t, points);
			}
			if (objective.isDisplayPlayers()){
				for (ArenaPlayer p : t.getPlayers()){
					setPoints(p, points);}
			}
		}
	}

	public Integer addPoints(ArenaTeam team, int points) {
		int oldPoints = teamPoints.getPoints(team);
		setPoints( team,points + oldPoints );
		return points + oldPoints;
	}

	public Integer addPoints(ArenaPlayer ap, int points) {
		int oldPoints = playerPoints.getPoints(ap);
		setPoints( ap, points + oldPoints );
		return points + oldPoints;
	}

	public Integer subtractPoints(ArenaTeam team, int points) {
		int oldPoints = teamPoints.getPoints(team);
		setPoints( team, oldPoints - points );
		return oldPoints - points;
	}

	public int subtractPoints(ArenaPlayer ap, int points) {
		int oldPoints = playerPoints.getPoints(ap);
		setPoints( ap, oldPoints - points );
		return oldPoints - points;
	}

	public List<ArenaTeam> getTeamLeaders() { return teamPoints.getLeaders(); }
	public TreeMap<Integer, Collection<ArenaTeam>> getTeamRanks() { return teamPoints.getRankings(); }
	public List<ArenaPlayer> getPlayerLeaders() { return playerPoints.getLeaders(); }
	public TreeMap<Integer, Collection<ArenaPlayer>> getPlayerRanks() { return playerPoints.getRankings(); }

	public MatchResult getMatchResult(Match match){
		TreeMap<Integer,Collection<ArenaTeam>> ranks = getTeamRanks();
		/// Deal with teams that haven't scored and possibly aren't inside the ranks
		HashSet<ArenaTeam> unfoundTeams = new HashSet<>(match.getAliveTeams());
		for (Collection<ArenaTeam> t : ranks.values()){
			unfoundTeams.removeAll(t);
		}
		Collection<ArenaTeam> zeroes = ranks.get(0);
		
		if ( zeroes != null )
			zeroes.addAll(unfoundTeams);
		else
			ranks.put(0, unfoundTeams);

		MatchResult result = new MatchResult();
		if ( ranks.isEmpty() )
			return result;
		if (ranks.size() == 1) { /// everyone tied obviously
			for (Collection<ArenaTeam> col : ranks.values()){
				result.setDrawers(col);}
		} 
		else {
			boolean first = true;
			for (Integer key : ranks.keySet()){
				Collection<ArenaTeam> col = ranks.get(key);
				if (first){
					result.setVictors(col);
					first = false;
				} 
				else {
					result.addLosers(col);
				}
			}
		}
        result.setRanking(ranks);
        return result;
	}

	public Integer setPoints(ArenaPlayer p, int points) {
		objective.setPoints(p.getName(), points);
		return playerPoints.setPoints(p, points);
	}

	public Integer setPoints(ArenaTeam t, int points) {
		objective.setPoints(t.getIDString(), points);
		return teamPoints.setPoints(t, points);
	}

	@Override
	public List<ArenaTeam> getLeaders() { return getTeamLeaders(); }
	@Override
	public TreeMap<?, Collection<ArenaTeam>> getRanks() { return getTeamRanks(); }
	@Override
	public void setScoreBoard(ArenaScoreboard scoreboard) { scoreboard.setObjectiveScoreboard(this); }
	@Override
	public void setDisplayName(String displayName) { objective.setDisplayName(displayName); }
    @Override
    public String getDisplayNameSuffix() { return objective.getDisplayNameSuffix(); }
    @Override
	public void setDisplayNameSuffix(String suffix) { objective.setDisplayNameSuffix(suffix); }
    @Override
    public String getDisplayNamePrefix() { return objective.getDisplayNamePrefix(); }
    @Override
    public void setDisplayNamePrefix(String prefix) { objective.setDisplayNamePrefix(prefix); }
    @Override
	public boolean setPoints(SEntry entry, int points) { return objective.setPoints(entry, points); }
	@Override
	public SAPIDisplaySlot getDisplaySlot() { return objective.getDisplaySlot(); }
	@Override
	public int getPriority() { return objective.getPriority(); }
	@Override
	public void setDisplaySlot(SAPIDisplaySlot slot) { objective.setDisplaySlot(slot); }
	@Override
	public String getId() { return objective.getId(); }
	@Override
	public String getDisplayName() { return objective.getDisplayName(); }
    @Override
    public String getBaseDisplayName() { return objective.getBaseDisplayName(); }
    @Override
	public boolean setTeamPoints(STeam t, int points) { return objective.setTeamPoints(t, points); }
	@Override
	public boolean setPoints(String id, int points) { return objective.setPoints(id, points); }
	@Override
	public SEntry addEntry(String id, int points) { return objective.addEntry(id, points); }
	@Override
	public void setDisplayPlayers(boolean b) { objective.setDisplayPlayers(b); }
	@Override
	public void setDisplayTeams(boolean display) { objective.setDisplayTeams(display); }

	@Override
	public void setScoreboard(SScoreboard scoreboard) {
		objective.setScoreboard(scoreboard);
		scoreboard.registerNewObjective(this);
	}

    @Override
    public int getPoints(String id) { return objective.getPoints(id); }
    @Override
    public int getPoints(SEntry e) { return objective.getPoints(e); }
    @Override
	public String toString() { return objective.toString(); }
	@Override
	public boolean isDisplayTeams() { return objective.isDisplayTeams(); }
	@Override
	public boolean isDisplayPlayers() { return objective.isDisplayPlayers(); }
	@Override
	public SScoreboard getScoreboard() { return objective.getScoreboard(); }
	@Override
	public SEntry addEntry(OfflinePlayer player, int points) { return objective.addEntry(player, points); }
	@Override
	public SEntry removeEntry(OfflinePlayer player) { return objective.removeEntry(player); }
	@Override
	public SEntry removeEntry(String id) { return objective.removeEntry(id); }
	@Override
	public boolean addEntry(SEntry entry, int defaultPoints) { return objective.addEntry(entry, defaultPoints); }
	@Override
	public SEntry removeEntry(SEntry entry) { return objective.removeEntry(entry); }
	@Override
	public boolean contains(SEntry e) { return objective.contains(e); }
	@Override
	public STeam addTeam(String id, int points) { return objective.addTeam(id, points); }
	@Override
	public boolean addTeam(STeam entry, int points) { return objective.addTeam(entry, points); }

    public void setDisplayName(String displayNamePrefix, String displayName, String displayNameSuffix, STeam team){
        if (objective instanceof BObjective) {
            ((BObjective) objective).setDisplayName( String.join( "", displayNamePrefix, displayName,displayNameSuffix ) );
        }
    }

    @Override
    public void initPoints(List<SEntry> entries, List<Integer> points) {
        objective.initPoints(entries, points);
    }
}
