package mc.alk.arena.objects.scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.bukkit.OfflinePlayer;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.competition.Match;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchResult;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.objects.victoryconditions.interfaces.ScoreTracker;
import mc.alk.arena.util.MessageUtil;
import mc.alk.arena.util.ScoreMap;


public class ArenaObjective implements ScoreTracker {

    final protected ScoreMap<ArenaTeam> teamPoints = new ScoreMap<>();
    final protected ScoreMap<ArenaPlayer> playerPoints = new ScoreMap<>();
    @Getter protected final String id;
    @Getter protected String criteria;
    protected String combinedDisplayName;
    protected String displayName;
    @Getter protected String displayNameSuffix;
    @Getter protected String displayNamePrefix;
    @Getter protected SAPIDisplaySlot displaySlot = SAPIDisplaySlot.NONE;

    @Getter protected ArenaScoreboard scoreboard;

    /// Used for Team support
    @Getter protected boolean displayPlayers = true;
    @Getter protected boolean displayTeams = true;

    // 1-1000 scale, not strictly enforced
    // the lower priorities will not be preempted when set
    @Getter int priority;

    protected TreeMap<SEntry, ArenaScore> entries = new TreeMap<>();
    protected Objective objective;
    Set<SEntry> cur15 = new HashSet<>();
    int worst = Integer.MAX_VALUE;
    
    TreeSet<ArenaScore> scores = new TreeSet<>( 
            ( o1, o2 )  -> {
                int c = o2.getScore() - o1.getScore();
                if (c != 0)
                    return c;
                return o1.getEntry().getId().compareTo(o2.getEntry().getId());
            });

    @Getter @AllArgsConstructor 
    public class ArenaScore {
        final SEntry entry;
        @Setter int score;
    } 
    
    public ArenaObjective( ArenaScoreboard board, String _id, String _displayName, String _criteria ) {
        this( board, _id, _displayName, _criteria, 50 );
    }
    public ArenaObjective( String _id, String _displayName, String _criteria, int _priority ) {
        this( null, _id, _displayName, _criteria, _priority );
    }
    public ArenaObjective( ArenaScoreboard board, String _id, String _displayName, String _criteria, int _priority ) {
        id = _id;
        criteria = MessageUtil.colorChat(_criteria);
        priority = _priority;
        setDisplayName(_displayName);
        
        if (board != null)
            setScoreboard(board);
    }
	public ArenaObjective( String name, String _criteria ) {
		this( name, _criteria, name, SAPIDisplaySlot.SIDEBAR, 50 );
	}
	/**
	 *
	 * @param name Objective name
	 * @param _criteria Objective criteria
	 * @param priority: lower priority means it has precedence
	 */
	public ArenaObjective( String name, String _criteria, int _priority ) {
		this( name, _criteria, name, SAPIDisplaySlot.SIDEBAR, _priority );
	}
	public ArenaObjective( String name, String _criteria, String _displayName, SAPIDisplaySlot slot ) {
		this( name, _criteria, _displayName, slot, 50 );
	}
	public ArenaObjective( String _id, String _criteria, String _displayName, SAPIDisplaySlot slot, int _priority ) {
		this( _id, _displayName, _criteria, _priority );
		setDisplaySlot(slot);
	}

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
			if ( isDisplayTeams() ) {
				setPoints(t, points);
			}
			if ( isDisplayPlayers() ) {
				for (ArenaPlayer p : t.getPlayers()){
					setPoints(p, points);}
			}
		}
	}

	public int addPoints(ArenaTeam team, int points) {
		int oldPoints = teamPoints.getPoints(team);
		setPoints( team, points + oldPoints );
		return points + oldPoints;
	}
	public int addPoints(ArenaPlayer ap, int points) {
		int oldPoints = playerPoints.getPoints(ap);
		setPoints( ap, points + oldPoints );
		return points + oldPoints;
	}

	public int subtractPoints(ArenaTeam team, int points) {
		int oldPoints = teamPoints.getPoints(team);
		setPoints( team, oldPoints - points );
		return oldPoints - points;
	}
	public int subtractPoints(ArenaPlayer ap, int points) {
		int oldPoints = playerPoints.getPoints(ap);
		setPoints( ap, oldPoints - points );
		return oldPoints - points;
	}

	public List<ArenaPlayer> getPlayerLeaders() { return playerPoints.getLeaders(); }
	public TreeMap<Integer, Collection<ArenaPlayer>> getPlayerRanks() { return playerPoints.getRankings(); }

	public MatchResult getMatchResult(Match match){
		TreeMap<Integer, Collection<ArenaTeam>> ranks = getRanks();
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

	public int setPoints(ArenaPlayer p, int points) {
		setPoints(p.getName(), points);
		return playerPoints.setPoints(p, points);
	}
	public int setPoints(ArenaTeam t, int points) {
		setPoints(t.getIDString(), points);
		return teamPoints.setPoints(t, points);
	}

	@Override
	public List<ArenaTeam> getLeaders() { return teamPoints.getLeaders(); }
	@Override
	public TreeMap<Integer, Collection<ArenaTeam>> getRanks() { return teamPoints.getRankings(); }
	
    @Override
    public void setScoreboard( ArenaScoreboard board ) {
        scoreboard = board;
        objective = board.bukkitScoreboard.getObjective(id);
        
        if (objective == null)
            objective = board.bukkitScoreboard.registerNewObjective(id,criteria);
        _setDisplayName();
    }
    /**
     * prefix + base + suffix must be less than 16 characters
     * @param displayName the display name of this Objective
     */
    public void setDisplayName(String _displayName) {
        displayName = MessageUtil.colorChat(_displayName);
        if (objective == null) return;
        _setDisplayName();
    }
    /**
     * prefix + displayName + suffix must be less than or equal 32 characters
     * @param suffix set the suffix
     */
    public void setDisplayNameSuffix(String suffix) {
        displayNamePrefix = MessageUtil.colorChat( suffix );
        if (objective == null) return;
        _setDisplayName();
    }
    /**
     * prefix + displayName + suffix must be less than or equal 32 characters
     * @param prefix set the prefix
     */
    public void setDisplayNamePrefix(String prefix) {
        displayNamePrefix = MessageUtil.colorChat( prefix );
        if (objective == null) return;
        _setDisplayName();
    }

    protected void _setDisplayName(){
        combinedDisplayName = SAPIUtil.createLimitedString(displayNamePrefix, displayName, displayNameSuffix,
                                                            SAPI.MAX_OBJECTIVE_DISPLAYNAME_SIZE);
        if (objective == null) return;
        objective.setDisplayName( combinedDisplayName );
    }

    public void initPoints(List<SEntry> _entries, List<Integer> points) {
        for ( int i = 0; i < _entries.size(); i++ ) {
            SEntry e = _entries.get(i);
            int point = points.get(i);
            ArenaScore score = new ArenaScore(e, point);
            entries.put(e, score);
            scores.add(score);
        }
        int i = 0;
        Iterator<ArenaScore> iter = scores.iterator();
        while(iter.hasNext() && i++ < 16) {
            ArenaScore sc = iter.next();
            _setScore(sc.getEntry(), sc.getScore());
        }
    }

    protected boolean setPoints( ArenaScore score, int points ) {
        if (    ( displayTeams && score.getEntry() instanceof STeam ) ||
                ( displayPlayers && score.getEntry() instanceof SAPIPlayerEntry ) ||
                ( !(score.getEntry() instanceof SAPIPlayerEntry) && !(score.getEntry() instanceof STeam) ) ) {
            addScore( score, points );
        } 
        else if ( score.getScore() != points ) {
            score.setScore( points );
        }
        return true;
    }

    private void addScore( ArenaScore e, int points ) {
        scores.remove(e);
        e.setScore(points);
        scores.add(e);

        if ( scores.size() <= SAPI.MAX_ENTRIES ) {
            _setScore( e.getEntry(), points );
            cur15.add(e.getEntry());
            worst = Math.min(points, worst);
        } 
        else {
            HashSet<SEntry> now15 = new HashSet<>(SAPI.MAX_ENTRIES);
            ArrayList<ArenaScore> added = new ArrayList<>(2);
            Iterator<ArenaScore> iter = scores.iterator();
            
            for ( int i = 0; i < SAPI.MAX_ENTRIES && iter.hasNext(); i++ ) {
                ArenaScore sapiScore = iter.next();
                now15.add( sapiScore.getEntry() );
                if ( !cur15.contains( sapiScore.getEntry() ) ) {
                    added.add( sapiScore );
                }
            }
            cur15.removeAll( now15 );
            for ( SEntry se : cur15 ) {
                objective.getScoreboard().resetScores(se.getBaseDisplayName());
            }
            cur15 = now15;
            if (added.isEmpty()) {
                if (cur15.contains(e.getEntry())) 
                    _setScore(e.getEntry(), points);                
            } 
            else {
                for ( ArenaScore se : added)  {
                    _setScore( se.getEntry(), se.getScore() );
                }
            }
        }
    }

    private void _setScore( SEntry e, int points) {
        Score sc = objective.getScore( e.getBaseDisplayName() );
        if (points != 0) {
            sc.setScore(points);
        } 
        else {
            /// flip from 1 to 0 (this is needed for board setup to display 0 values of initial entries
            sc.setScore(1);
            sc.setScore(0);
        }
    }
    
    public int getPoints(SEntry l) {
        String p = l.getBaseDisplayName();
        if ( p == null ) return 0;
        return objective.getScore( p ).getScore();
    }

    public ArenaObjective setDisplaySlot(SAPIDisplaySlot slot) {
        displaySlot = slot;
        if ( scoreboard == null ) return this;
        
        scoreboard.setDisplaySlot( slot, this, true );
                
        if ( objective != null && scoreboard.getObjective(slot) == this) {
            objective.setDisplaySlot( slot.toBukkitDisplaySlot() );
        }
        return this;
    }

    public void setDisplayPlayers(boolean display){
        if (display == displayPlayers ) return;
        displayPlayers = display;
        setDisplay();
    }

    @Override
    public void setDisplayTeams(boolean display){
        if (display == displayTeams ) return;
        displayTeams = display;
        setDisplay();
    }

    private void setDisplay() {
        scores.clear();
        cur15.clear();
        if ( scoreboard != null) {
            for ( SEntry entry : scoreboard.getEntries()) {
                if (!contains(entry)) continue;
                
                if ((displayPlayers && entry instanceof SAPIPlayerEntry) ||
                        (displayTeams && entry instanceof STeam) ||
                        (!(entry instanceof SAPIPlayerEntry) && !(entry instanceof STeam))) {
                    ArenaScore sc = entries.get(entry);
                    addScore(sc, sc.getScore());
                } 
                else {
                    objective.getScoreboard().resetScores(entry.getBaseDisplayName());
                }
            }
        }
    }

    /**
     * Get the display name: prefix + base + suffix
     * @return the display name
     */
    public String getDisplayName() { return combinedDisplayName; }
    /**
     * Get the base display (without the prefix or suffix)
     * @return the display name
     */
    public String getBaseDisplayName() { return displayName; }

    public SEntry addEntry( OfflinePlayer p, int points ) { return addEntry(p.getName(),points); }

    public SEntry addEntry( String _id, int points ) {
        SEntry e = scoreboard.getEntry(_id);
        if (e == null){
            if (getScoreboard() != null)
                e = scoreboard.createEntry(_id, _id);
            else
                throw new IllegalStateException("You cannot add an entry that hasnt already been created " + _id);
        }
        addEntry( e, points );
        return e;
    }

    public boolean addEntry( SEntry entry, int points ) {
        
        if (entry instanceof STeam) {
            return addTeam((STeam) entry, points);}
        
        boolean has = entries.containsKey(entry);
        if ( !has ) {
            getOrCreateSAPIScore(entry, points);
        } 
        else {
            setPoints(getOrCreateSAPIScore(entry), points);
        }
        return has;
    }

    public STeam addTeam( String _id, int points ) {
        STeam t = scoreboard.getTeam(_id);
        if ( t != null ) {
            addTeam( t, points );
        }
        return t;
    }

    public boolean addTeam( STeam entry, int points ) {
        boolean has = entries.containsKey(entry);
        ArenaScore sc = getOrCreateSAPIScore(entry, points);
        if ( !isDisplayPlayers() ) {
            for ( String e : entry.getPlayers() ) {
                sc = getOrCreateSAPIScore(scoreboard.createEntry(e));
                if (isDisplayPlayers()){
                    setPoints(sc, points);}
            }
        }
        return has;
    }

    protected final ArenaScore getOrCreateSAPIScore(SEntry e){
        return getOrCreateSAPIScore( e, 0 );
    }

    public void removeEntry( String _id ) {
        SEntry e = scoreboard.getEntry(_id);
        if ( e != null ) 
            removeEntry(e);
    }

    public void removeEntry( SEntry entry ) {      
        entries.remove( entry );
    }

    public int getPoints( String _id ) {
        SEntry l = scoreboard.getEntry(_id);
        if (l == null) return -1;
        return getPoints(l);
    }

    private void setPoints( String _id, int points ) {
        if (scoreboard == null) return;
        
        SEntry l = scoreboard.getEntry(_id);
        
        if ( l == null ) return;
        
        setPoints( l, points );
    }

    public void setPoints( SEntry entry, int points ) {
        getOrCreateSAPIScore( entry, points );
    }

    protected final ArenaScore getOrCreateSAPIScore( SEntry e, int points ) {
        if ( entries.containsKey( e ) ) return entries.get( e );
        
        ArenaScore score = new ArenaScore( e, points );
        entries.put( e, score );
        setPoints( score, points );
        return score;
    }

    public boolean setTeamPoints( STeam team, int points ) {
        if ( displayTeams ) {
            setPoints( team, points );
        }
        if ( displayPlayers ) {
            for ( String p : team.getPlayers() ) {
                SEntry e = scoreboard.createEntry( p );
                if ( e == null ) continue;
                setPoints( e, points );
            }
        }
        return true;
    }

    public boolean contains( SEntry e ) { return entries.containsKey(e); }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("&6 --- ").append( id ).append(" : ").append( combinedDisplayName ).
                append("&4 : ").append( priority ).append("\n");
        if (scoreboard == null){
            sb.append("&4 Bukkit scoreboard still not set!!");
            return sb.toString();
        }
        Collection<SEntry> es = scoreboard.getEntries();
        if (objective == null){
            sb.append("&4 Bukkit Objective still not set!!");
            return sb.toString();
        }
        List<SEntry> zeroes = new ArrayList<>();
        List<SEntry> skipped = new ArrayList<>();
        for ( SEntry e : es ) {
            if ( !contains(e) ) {
                skipped.add(e);
                continue;
            }
            Set<Score> scoresSet = objective.getScoreboard().getScores(e.getBaseDisplayName());
            for (Score score : scoresSet){
                if (score.getObjective().equals(objective)){
                    if (score.getScore() != 0){
                        if (e instanceof BukkitTeam){
                            BukkitTeam bt = ((BukkitTeam)e);
                            sb.append("&e ").append(e.getId()).append(" : ").append(e.getDisplayName()).append(" = ").
                                    append(score.getScore()).append("  &eteamMembers=\n");
                            for (String p : bt.getPlayers()){
                                SEntry ep = getScoreboard().createEntry(p);
                                String c = contains(ep) ? "&e" : "&8";
                                sb.append("  ").append(c).append("- &f").append(bt.getPrefix()).append(p).
                                        append(bt.getSuffix()).append(c).append(" = ").
                                        append(objective.getScore(p).getScore()).append("\n");
                            }
                        } 
                        else {
                            sb.append("&6 ").append(e.getId()).append(" : ").append(e.getDisplayName()).append(" = ").
                                    append(score.getScore()).append("\n");
                        }
                    } 
                    else {
                        zeroes.add(e);
                    }
                }
            }
        }
        if (!skipped.isEmpty()){
            sb.append(" &cSkipped Entries: ");
            for (SEntry e: skipped){
                sb.append("&6 ").append(e.getId()).append(":").
                        append(e.getDisplayName()).append("&e,");}
            sb.append("\n");
        }
        if (!zeroes.isEmpty()){
            sb.append(" &eZero Entries: ");
            for (SEntry e: zeroes){
                sb.append("&6 '").append(e.getId()).append("':'").
                        append(e.getDisplayName()).append("'&e,");}
            sb.append("\n");
        }
        return sb.toString();
    }
}
