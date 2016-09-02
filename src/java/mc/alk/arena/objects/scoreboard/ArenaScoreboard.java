package mc.alk.arena.objects.scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.controllers.Scheduler;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.options.TransitionOption;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.util.Log;

@RequiredArgsConstructor
public class ArenaScoreboard {
    
    static int ids = 0;
    HashMap<Integer, SEntry> row = new HashMap<>();
    HashMap<String, Integer> idmap = new HashMap<>();
    @Getter public Scoreboard bukkitScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
    @Getter protected final Plugin plugin;
    @Getter protected final String name;
    protected Map<String, ArenaObjective> objectives = new HashMap<>();
    protected HashMap<SAPIDisplaySlot, ArenaObjective> slots = new HashMap<>();

    HashMap<String, Scoreboard> oldBoards = new HashMap<>();
    final HashMap<ArenaTeam, STeam> teams = new HashMap<>();
    boolean colorPlayerNames = Defaults.USE_COLORNAMES;
    
    @AllArgsConstructor
    private class BoardUpdate {
        HashMap<Objective, Integer> scores;
        Team team;
    }

    public ArenaScoreboard(String scoreboardName) {
        this( BattleArena.getSelf(), scoreboardName );
    }
    public ArenaScoreboard(String scoreboardName, MatchParams params) {
        this( scoreboardName );
        colorPlayerNames = Defaults.USE_COLORNAMES &&
                (!params.getStateGraph().hasAnyOption(TransitionOption.NOTEAMNAMECOLOR));
    }

    public ArenaObjective createObjective(String id, String criteria, String displayName) {
        return createObjective( id, criteria, displayName, SAPIDisplaySlot.SIDEBAR );
    }
    public ArenaObjective createObjective(String id, String criteria, String displayName, SAPIDisplaySlot slot) {
        return createObjective( id, criteria, displayName, SAPIDisplaySlot.SIDEBAR, 50 );
    }
    public ArenaObjective createObjective(String id, String criteria, String displayName, SAPIDisplaySlot slot, int priority) {
        ArenaObjective o = new ArenaObjective( id, criteria, displayName, slot, priority );
        addObjective(o);
        return o;
    }

    public STeam addTeam(ArenaTeam team) {
        STeam t = teams.get(team);
        if (t != null)
            return t;
        t = createTeamEntry(team.getIDString(), team.getScoreboardDisplayName());
        Set<Player> bukkitPlayers = team.getBukkitPlayers();

        t.addPlayers(bukkitPlayers);
        for (Player p: bukkitPlayers){
            setScoreboard(p);
        }
        if (colorPlayerNames)
            t.setPrefix(team.getTeamChatColor()+"");
        teams.put(team, t);

        for ( ArenaObjective o : getObjectives() ) {
            o.addTeam(t, 0);
            if (o.isDisplayPlayers()){
                for (ArenaPlayer player: team.getPlayers()){
                    o.addEntry(player.getName(), 0);
                }
            }
        }
        return t;
    }
    
    public void removeTeam(ArenaTeam team) {
        STeam sTeam = teams.remove(team);
        if ( sTeam != null ) {
            removeEntry( sTeam );
            for ( ArenaObjective objective : getObjectives() ) {
                objective.removeEntry( sTeam );
                for ( String player : sTeam.getPlayers() ) {
                    objective.removeEntry( player );
                }
            }
        }
    }
    
    public void addedToTeam(ArenaTeam team, ArenaPlayer player) {
        STeam t = teams.get( team );
        if ( t == null )
            t = addTeam( team );
        addedToTeam( t, player );
    }

    public void addedToTeam(STeam team, ArenaPlayer player){
        team.addPlayer( player.getPlayer() );
        setScoreboard( player.getPlayer() );
    }

    public void removeFromTeam( ArenaTeam team, ArenaPlayer player ) {
        removeFromTeam( teams.get( team ), player );
    }
    public void removeFromTeam( STeam _team, ArenaPlayer player ) {
        if ( _team == null ) {
            Log.err(teams.size() + "  Removing from a team that doesn't exist player=" + player.getName() + "   team=" + _team );
            return;
        }
        _team.removePlayer( player.getPlayer() );
        removeScoreboard( player.getPlayer() );
    }

    public void addObjective( ArenaObjective scores ) {
        registerNewObjective( scores );
        addAllEntries( scores );
    }

    public ArenaObjective registerNewObjective( String objectiveName, String criteria, String displayName, SAPIDisplaySlot slot) {
        return createObjective( objectiveName, criteria, displayName, slot );
    }

    public ArenaObjective registerNewObjective( ArenaObjective obj ) {
        objectives.put( obj.getId().toUpperCase(), obj );
        if ( obj.getScoreboard() == null || !obj.getScoreboard().equals(this) ) {
            obj.setScoreboard( this );
        }
        if ( obj.getDisplaySlot() != null )
            setDisplaySlot( obj.getDisplaySlot(), obj, false, true );
        return obj;
    }

    public void setScoreboard( Player p) {
        if ( p.getScoreboard() != null 
                && !oldBoards.containsKey( p.getName() ) )
            oldBoards.put( p.getName(), p.getScoreboard() );
        
        Scheduler.scheduleSynchronousTask( 
                () -> {
                    if ( oldBoards.containsKey( p.getName() ) ) 
                        p.setScoreboard( bukkitScoreboard );           
                });
    }

    public void removeScoreboard( Player player ) {
            Scoreboard b = oldBoards.remove( player.getName() );
            
            if ( b != null ) 
                player.setScoreboard(b);
            else 
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());                
    }

    private BoardUpdate clearBoard( SEntry e ) {
        HashMap<Objective, Integer> oldScores = new HashMap<>();      
        Set<Score> scores = bukkitScoreboard.getScores( e.getBaseDisplayName()  );

        for ( Score score : scores ) {
            oldScores.put(score.getObjective(), score.getScore());
        }
        bukkitScoreboard.resetScores( e.getBaseDisplayName() );
        Team t = bukkitScoreboard.getTeam( e.getBaseDisplayName()  );
        if (t != null)
            t.removeEntry( e.getBaseDisplayName()  );

        return new BoardUpdate(oldScores, t);
    }

    private void updateBoard(SEntry e, BoardUpdate bu){
        if (bu.team != null){
            bu.team.addEntry( e.getBaseDisplayName() );
        }
        for (Entry<Objective, Integer> entry : bu.scores.entrySet()) {
            if (entry.getValue() == 0) {
                entry.getKey().getScore( e.getBaseDisplayName() ).setScore(1);
            }
            entry.getKey().getScore( e.getBaseDisplayName() ).setScore(entry.getValue());
        }
    }

    public void setEntryDisplayName(SEntry e, String _name) {
        BoardUpdate bu = clearBoard(e);
        e.setDisplayName( _name );
        updateBoard(e, bu);
    }

    public void setEntryNamePrefix(SEntry e, String _name) {
        BoardUpdate bu = clearBoard(e);
        e.setDisplayNamePrefix( _name );
        updateBoard(e, bu);
    }

    public void setEntryNameSuffix(SEntry e, String _name) {
        BoardUpdate bu = clearBoard(e);
        e.setDisplayNameSuffix( _name );
        updateBoard(e, bu);
    }

    public SEntry removeEntry(SEntry e) {
        bukkitScoreboard.resetScores( e.getBaseDisplayName() ); 
        Team t = bukkitScoreboard.getTeam( e.getBaseDisplayName() );
        
        if ( t != null ) 
            t.removeEntry( e.getBaseDisplayName() );
        
        Integer id = idmap.remove( e.getId() );
        
        if ( id != null ) {
            e = row.remove(id);
        }
        if ( e != null ) {
            for ( ArenaObjective objective : getObjectives() ) {
                objective.removeEntry( e );
            }
        }
        return e;
    }

    public BukkitTeam createTeamEntry(String id, String displayName) {
        BukkitTeam st = getTeam(id);
        if (st!=null)
            return st;
        Team t = bukkitScoreboard.getTeam(id);
        if (t == null){
            t = bukkitScoreboard.registerNewTeam(id);
        }

        t.setDisplayName(displayName);
        BukkitTeam bt = new BukkitTeam(this, t);
        registerEntry(bt);
        return bt;
    }

    public BukkitTeam getTeam(String id) {
        SEntry e = getEntry(id);
        return (e == null || !(e instanceof BukkitTeam)) ? null 
                                                         : (BukkitTeam) e;
    }

    public void addAllEntries(ArenaObjective objective) {
        for ( SEntry entry : getEntries() ) {
            objective.addEntry(entry, 0);
        }
    }
    public boolean hasThisScoreboard(Player player) {
        return player.getScoreboard() != null && player.getScoreboard().equals(bukkitScoreboard);
    }
    public void setDisplaySlot(SAPIDisplaySlot slot, ArenaObjective objective) {
        setDisplaySlot( slot, objective, false );
    }
    public void setDisplaySlot(SAPIDisplaySlot slot, ArenaObjective objective, boolean fromObjective) {
        setDisplaySlot( slot, objective, fromObjective, true );
    }
    boolean setDisplaySlot(final SAPIDisplaySlot slot, final ArenaObjective objective, boolean fromObjective, boolean swap ) {
        if (!slots.containsKey(slot)){
            _setDisplaySlot(slot,objective,fromObjective);
            return true;
        }
        int opriority = slots.get(slot).getPriority();
        
        if (objective.getPriority() <= opriority){
            SAPIDisplaySlot swapSlot = slot.swap();
            ArenaObjective movingObjective = slots.get(slot);
            if (!slots.containsKey(swapSlot) || opriority <= slots.get(swapSlot).getPriority()) {
                _setDisplaySlot(swapSlot,movingObjective,fromObjective);
            }
            _setDisplaySlot(slot,objective,fromObjective);
            return true;
        }
        return false;
    }

    private void _setDisplaySlot(SAPIDisplaySlot slot, ArenaObjective objective, boolean fromObjective) {
        slots.put(slot, objective);
        if (!fromObjective)
            objective.setDisplaySlot(slot);
    }

    public Collection<SEntry> getEntries() { return new ArrayList<>( row.values() ); }
    public List<STeam> getTeams() { return new ArrayList<>( teams.values() ); }
    public ArenaObjective getObjective(SAPIDisplaySlot slot) { return slots.get( slot ); }
    public ArenaObjective getObjective(String id) { return objectives.get( id.toUpperCase() ); }
    public List<ArenaObjective> getObjectives() { return new ArrayList<>( objectives.values() ); }
    public void clear() { objectives.clear(); }
    
    public String getPrintString() {
        StringBuilder sb = new StringBuilder();
        for (Entry<SAPIDisplaySlot, ArenaObjective> entry : slots.entrySet()){
            sb.append("&5").append(entry.getKey()).append(" : ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    public void removeEntry( String _name ) {
        SEntry sb = getEntry( _name );
        if ( sb != null ) removeEntry( sb );
    }
    public void setEntryDisplayName(String id, String _name) {
        SEntry e = getEntry(id);
        if ( e != null ) setEntryDisplayName( e, _name );
    }
    public void setEntryNamePrefix(String id, String _name) {
        SEntry e = getEntry(id);
        if ( e != null ) setEntryNamePrefix( e, _name );
    }
    public void setEntryNameSuffix(String id, String _name) {
        SEntry e = getEntry(id);
        if ( e != null ) setEntryNameSuffix( e, _name );
    }

    public void registerEntry(SEntry entry){
        if (!contains(entry.getId())){
            Integer realid = ids++;
            idmap.put(entry.getId(), realid);
            row.put(realid, entry);
        }
    }
  
    public SEntry createEntry( String p ) { return createEntry( p, p ); }
    
    public SEntry createEntry( String id, String displayName ) { 
        if (!contains(id)){
            Integer realid = ids++;
            idmap.put(id, realid);
            Player p = Bukkit.getPlayerExact(id);
            SEntry l = p == null ? new SAPIEntry( id, displayName ) : new SAPIPlayerEntry( p, displayName );
            row.put(realid, l);
            return l;
        }
        return getEntry(id);
    }
   
    public STeam getTeamEntry(String id) {
        SEntry e = getEntry(id);
        return e == null || !(e instanceof STeam) ? null : (STeam) e;
    }
    public boolean contains(String id) {
        return idmap.containsKey(id) && row.containsKey(idmap.get(id));
    }
    
    public SEntry getEntry(String id) {
        return !idmap.containsKey(id) ? null 
                                      : row.get( idmap.get( id ) );
    }
    @Override
    public String toString(){
        return getPrintString();
    }
}
