package mc.alk.arena.scoreboardapi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import mc.alk.arena.controllers.Scheduler;

@RequiredArgsConstructor
public class SScoreboard {

    static int ids = 0;
    HashMap<Integer, SEntry> row = new HashMap<>();
    HashMap<String, Integer> idmap = new HashMap<>();
    @Getter protected Scoreboard bukkitScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
    @Getter protected final Plugin plugin;
    @Getter protected final String name;
//    protected Handler handler = new Handler();
    protected Map<String, SObjective> objectives = new HashMap<>();
    protected HashMap<SAPIDisplaySlot, SObjective> slots = new HashMap<>();

    HashMap<String,Scoreboard> oldBoards = new HashMap<>();

    public SObjective registerNewObjective( SObjective obj ) {
        objectives.put( obj.getId().toUpperCase(), obj );
        if ( obj.getScoreboard() == null || !obj.getScoreboard().equals(this) ) {
            obj.setScoreboard( this );
        }
        if ( obj.getDisplaySlot() != null )
            setDisplaySlot( obj.getDisplaySlot(), obj, false, true );
        return obj;
    }

    public SAPIObjective registerNewObjective(String id, String displayName, String criteria, SAPIDisplaySlot slot) {
        BObjective o =  new BObjective( this,id, displayName,criteria);
        o.setDisplayName(displayName);
        o.setDisplaySlot(slot);
        registerNewObjective(o);
        return o;
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

    public void removeScoreboard(Player player) {
            Scoreboard b = oldBoards.remove(player.getName());
            
            if ( b != null ) 
                player.setScoreboard(b);
            else 
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());                
    }

    @AllArgsConstructor
    private class BoardUpdate {
        HashMap<Objective, Integer> scores;
        Team team;
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

    public void setEntryDisplayName(SEntry e, String name) {
        BoardUpdate bu = clearBoard(e);
        e.setDisplayName( name );
        updateBoard(e, bu);
    }

    public void setEntryNamePrefix(SEntry e, String name) {
        BoardUpdate bu = clearBoard(e);
        e.setDisplayNamePrefix( name );
        updateBoard(e, bu);
    }

    public void setEntryNameSuffix(SEntry e, String name) {
        BoardUpdate bu = clearBoard(e);
        e.setDisplayNameSuffix( name );
        updateBoard(e, bu);
    }

    public SEntry removeEntry(SEntry e) {
        bukkitScoreboard.resetScores( e.getBaseDisplayName() ); 
        Team t = bukkitScoreboard.getTeam( e.getBaseDisplayName() );
        
        if ( t != null) 
            t.removeEntry( e.getBaseDisplayName() );
        
        Integer id = idmap.remove( e.getId() );
        
        if ( id != null ) {
            e = row.remove(id);
        }
        if ( e != null ) {
            for ( SObjective o : getObjectives() ) {
                o.removeEntry( e );
            }
        }
        return e;
    }

    public BukkitTeam createTeamEntry(String id, String displayName) {
        BukkitTeam st = getTeam(id);
        if (st!=null)
            return st;
        Team t = this.bukkitScoreboard.getTeam(id);
        if (t == null){
            t = this.bukkitScoreboard.registerNewTeam(id);
        }

        t.setDisplayName(displayName);
        BukkitTeam bt = new BukkitTeam(this, t);
        registerEntry(bt);
        return bt;
    }

    public BukkitTeam getTeam(String id) {
        SEntry e = getEntry(id);
        return (e == null || !(e instanceof BukkitTeam)) ? null : (BukkitTeam)e;
    }

    public void addAllEntries(SObjective objective) {
        for ( SEntry entry : getEntries() ) {
            objective.addEntry(entry, 0);
        }
    }

    public boolean hasThisScoreboard(Player player) {
        return bukkitScoreboard != null && player.getScoreboard() != null && player.getScoreboard().equals(bukkitScoreboard);
    }

    public void setDisplaySlot(SAPIDisplaySlot slot, SObjective objective) {
        setDisplaySlot( slot, objective, false );
    }

    public void setDisplaySlot(SAPIDisplaySlot slot, SObjective objective, boolean fromObjective) {
        setDisplaySlot( slot, objective, fromObjective, true );
    }

    boolean setDisplaySlot(final SAPIDisplaySlot slot, final SObjective objective, boolean fromObjective, boolean swap ) {
        if (!slots.containsKey(slot)){
            _setDisplaySlot(slot,objective,fromObjective);
            return true;
        }
        int opriority = slots.get(slot).getPriority();
        
        if (objective.getPriority() <= opriority){
            SAPIDisplaySlot swapSlot = slot.swap();
            SObjective movingObjective = slots.get(slot);
            if (!slots.containsKey(swapSlot) || opriority <= slots.get(swapSlot).getPriority()) {
                _setDisplaySlot(swapSlot,movingObjective,fromObjective);
            }
            _setDisplaySlot(slot,objective,fromObjective);
            return true;
        }
        return false;
    }

    private void _setDisplaySlot(SAPIDisplaySlot slot, SObjective objective, boolean fromObjective) {
        slots.put(slot, objective);
        if (!fromObjective)
            objective.setDisplaySlot(slot);
    }

    public SObjective getObjective(SAPIDisplaySlot slot) {
        return slots.get(slot);
    }

    public SObjective getObjective(String id) {
        return objectives.get(id.toUpperCase());
    }

    public List<SObjective> getObjectives() {
        return new ArrayList<>(objectives.values());
    }
    
    public String getPrintString() {
        StringBuilder sb = new StringBuilder();
        for (Entry<SAPIDisplaySlot,SObjective> entry : slots.entrySet()){
            sb.append("&5").append(entry.getKey()).append(" : ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    public SEntry createEntry(OfflinePlayer p) {
        return createEntry( p, p.getName() );
    }

    public SEntry createEntry(OfflinePlayer p, String displayName) {
        return getOrCreateEntry(p, displayName);
    }

    public SEntry createEntry(String id, String displayName) {
        return getOrCreateEntry(id, displayName);
    }

    public SEntry removeEntry(OfflinePlayer p) {
        SEntry sb = getEntry(p);
        if (sb != null){
            return removeEntry(sb);}
        return null;
    }

    public void clear() { objectives.clear(); }

    public SEntry getOrCreateEntry(String p) {
        Player player = Bukkit.getPlayerExact( p );
        
        if ( player == null ) return null;
        
        return getOrCreateEntry( player );
    }

    public boolean setEntryDisplayName(String id, String name) {
        SEntry e = getEntry(id);
        if (e == null)
            return false;
        setEntryDisplayName(e, name);
        return true;
    }

    public boolean setEntryNamePrefix(String id, String name) {
        SEntry e = getEntry(id);
        if (e == null)
            return false;
        setEntryNamePrefix(e, name);
        return true;
    }
    
    public boolean setEntryNameSuffix(String id, String name) {
        SEntry e = getEntry(id);
        if (e == null)
            return false;
        setEntryNameSuffix(e, name);
        return true;
    }

    public SEntry getOrCreateEntry(OfflinePlayer p) {
        return getOrCreateEntry( p, p.getName() );
    }

    public SEntry getOrCreateEntry(OfflinePlayer p, String displayName) {
        SEntry e = getEntry(p);
        if ( e == null ) {
            Integer realid = ids++;
            idmap.put( p.getName(), realid );
            e = new SAPIPlayerEntry( p, displayName );
            row.put(realid, e);
        }
        return e;
    }

    public void registerEntry(SEntry entry){
        if (!contains(entry.getId())){
            Integer realid = ids++;
            idmap.put(entry.getId(), realid);
            row.put(realid, entry);
        }
    }

    public SEntry getOrCreateEntry(String id, String displayName) {
        if (!contains(id)){
            Integer realid = ids++;
            idmap.put(id, realid);
            Player p = Bukkit.getPlayerExact(id);
            SEntry l = p == null ? new SAPIEntry(id,displayName) : new SAPIPlayerEntry(p,displayName);
            row.put(realid, l);
            return l;
        }
        return getEntry(id);
    }

    public SEntry removeEntry(Player p) {
        Integer id = idmap.remove(p.getName());
        if (id != null){
            return row.remove(id);
        }
        return null;
    }

    public STeam getTeamEntry(String id) {
        SEntry e = getEntry(id);
        return e == null || !(e instanceof STeam) ? null : (STeam) e;
    }

    public boolean contains(String id) {
        return idmap.containsKey(id) && row.containsKey(idmap.get(id));
    }

    public boolean contains(OfflinePlayer p) {
        return idmap.containsKey(p.getName()) && row.containsKey(idmap.get(p.getName()));
    }

    public SEntry getEntry(OfflinePlayer p) {
        return !idmap.containsKey( p.getName() ) ? null 
                                                 : row.get( idmap.get( p.getName() ) );
    }

    public SEntry getEntry(String id) {
        return !idmap.containsKey(id) ? null 
                                      : row.get( idmap.get( id ) );
    }

    public Collection<SEntry> getEntries() {
        return new ArrayList<>( row.values() );
    }
}
