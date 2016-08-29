//package mc.alk.arena.scoreboardapi;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.Set;
//
//import org.bukkit.Bukkit;
//import org.bukkit.OfflinePlayer;
//import org.bukkit.entity.Player;
//import org.bukkit.plugin.Plugin;
//import org.bukkit.scoreboard.Objective;
//import org.bukkit.scoreboard.Score;
//import org.bukkit.scoreboard.Scoreboard;
//import org.bukkit.scoreboard.Team;
//
//import lombok.AllArgsConstructor;
//import lombok.Getter;
//import lombok.RequiredArgsConstructor;
//import mc.alk.arena.controllers.Scheduler;
//
//@RequiredArgsConstructor
//public class BScoreboard implements SScoreboard {
//
//    @Getter protected Scoreboard bukkitScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
//    @Getter protected final Plugin plugin;
//    @Getter protected final String name;
//    protected Handler handler = new Handler();
//    protected Map<String, SObjective> objectives = new HashMap<>();
//    protected HashMap<SAPIDisplaySlot, SObjective> slots = new HashMap<>();
//
//    HashMap<String,Scoreboard> oldBoards = new HashMap<>();
//
//  
//    @Override
//    public SObjective registerNewObjective( SObjective obj ) {
//        objectives.put( obj.getId().toUpperCase(), obj );
//        if ( obj.getScoreboard() == null || !obj.getScoreboard().equals(this) ) {
//            obj.setScoreboard( this );
//        }
//        if ( obj.getDisplaySlot() != null )
//            setDisplaySlot( obj.getDisplaySlot(), obj, false, true );
//        return obj;
//    }
//
//    @Override
//    public SAPIObjective registerNewObjective(String id, String displayName, String criteria,
//                                              SAPIDisplaySlot slot) {
//        BObjective o =  new BObjective( this,id, displayName,criteria);
//        o.setDisplayName(displayName);
//        o.setDisplaySlot(slot);
//        registerNewObjective(o);
//        return o;
//    }
//
//    @Override
//    public void setScoreboard( Player p) {
//        if ( p.getScoreboard() != null 
//                && !oldBoards.containsKey( p.getName() ) )
//            oldBoards.put( p.getName(), p.getScoreboard() );
//        
//        Scheduler.scheduleSynchronousTask( 
//                () -> {
//                    if ( oldBoards.containsKey( p.getName() ) ) 
//                        p.setScoreboard( bukkitScoreboard );           
//                });
//    }
//
//    @Override
//    public void removeScoreboard(Player player) {
//            Scoreboard b = oldBoards.remove(player.getName());
//            
//            if ( b != null ) 
//                player.setScoreboard(b);
//            else 
//                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());                
//    }
//
//    public void transferOldScoreboards(BScoreboard oldScoreboard) {
//        oldBoards.putAll(oldScoreboard.oldBoards);
//    }
//
//    @AllArgsConstructor
//    private class BoardUpdate {
//        HashMap<Objective, Integer> scores;
//        Team team;
//    }
//
//    private BoardUpdate clearBoard( SEntry e ) {
//        HashMap<Objective, Integer> oldScores = new HashMap<>();      
//        Set<Score> scores = bukkitScoreboard.getScores( e.getBaseDisplayName()  );
//
//        for ( Score score : scores ) {
//            oldScores.put(score.getObjective(), score.getScore());
//        }
//        bukkitScoreboard.resetScores( e.getBaseDisplayName() );
//        Team t = bukkitScoreboard.getTeam( e.getBaseDisplayName()  );
//        if (t != null)
//            t.removeEntry( e.getBaseDisplayName()  );
//
//        return new BoardUpdate(oldScores, t);
//    }
//
//    private void updateBoard(SEntry e, BoardUpdate bu){
//        if (bu.team != null){
//            bu.team.addEntry( e.getBaseDisplayName() );
//        }
//        for (Entry<Objective, Integer> entry : bu.scores.entrySet()) {
//            if (entry.getValue() == 0) {
//                entry.getKey().getScore( e.getBaseDisplayName() ).setScore(1);
//            }
//            entry.getKey().getScore( e.getBaseDisplayName() ).setScore(entry.getValue());
//        }
//    }
//
//    @Override
//    public void setEntryDisplayName(SEntry e, String name) {
//        BoardUpdate bu = clearBoard(e);
//        e.setDisplayName( name );
//        updateBoard(e, bu);
//    }
//
//    @Override
//    public void setEntryNamePrefix(SEntry e, String name) {
//        BoardUpdate bu = clearBoard(e);
//        e.setDisplayNamePrefix( name );
//        updateBoard(e, bu);
//    }
//
//    @Override
//    public void setEntryNameSuffix(SEntry e, String name) {
//        BoardUpdate bu = clearBoard(e);
//        e.setDisplayNameSuffix( name );
//        updateBoard(e, bu);
//    }
//
//    @Override
//    public SEntry removeEntry(SEntry e) {
//        bukkitScoreboard.resetScores( e.getBaseDisplayName() ); 
//        Team t = bukkitScoreboard.getTeam( e.getBaseDisplayName() );
//        
//        if ( t != null) 
//            t.removeEntry( e.getBaseDisplayName() );
//        
//        e = handler.removeEntry(e);
//        if (e != null){
//            for (SObjective o : getObjectives()) {
//                o.removeEntry(e);
//            }
//        }
//        return e;
//    }
//
//    @Override
//    public SAPITeam createTeamEntry(String id, String displayName) {
//        SAPITeam st = this.getTeam(id);
//        if (st!=null)
//            return st;
//        Team t = this.bukkitScoreboard.getTeam(id);
//        if (t == null){
//            t = this.bukkitScoreboard.registerNewTeam(id);
//        }
//
//        t.setDisplayName(displayName);
//        BukkitTeam bt = new BukkitTeam(this, t);
//        handler.registerEntry(bt);
//        return bt;
//    }
//
//    @Override
//    public SAPITeam getTeam(String id) {
//        SEntry e = handler.getEntry(id);
//        return (e == null || !(e instanceof SAPITeam)) ? null : (SAPITeam)e;
//    }
//
//    public void addAllEntries(SObjective objective) {
//        for (SEntry entry : handler.getEntries()){
//            objective.addEntry(entry, 0);
//        }
//    }
//
//    @Override
//    public boolean hasThisScoreboard(Player player) {
//        return bukkitScoreboard != null && player.getScoreboard() != null && player.getScoreboard().equals(bukkitScoreboard);
//    }
//
//    @Override
//    public void setDisplaySlot(SAPIDisplaySlot slot, SObjective objective) {
//        setDisplaySlot( slot, objective, false );
//    }
//
//    @Override
//    public void setDisplaySlot(SAPIDisplaySlot slot, SObjective objective, boolean fromObjective) {
//        setDisplaySlot( slot, objective, fromObjective, true );
//    }
//
//
//    boolean setDisplaySlot(final SAPIDisplaySlot slot, final SObjective objective, boolean fromObjective, boolean swap ) {
//        if (!slots.containsKey(slot)){
//            _setDisplaySlot(slot,objective,fromObjective);
//            return true;
//        }
//        int opriority = slots.get(slot).getPriority();
//        /// Check to see if we need to move
//        /// if our new objective priority <= oldpriority
//        if (objective.getPriority() <= opriority){
//            SAPIDisplaySlot swapSlot = slot.swap();
//            SObjective movingObjective = slots.get(slot);
//            if (!slots.containsKey(swapSlot) || opriority <= slots.get(swapSlot).getPriority()) {
//                _setDisplaySlot(swapSlot,movingObjective,fromObjective);
//            }
//            _setDisplaySlot(slot,objective,fromObjective);
//            return true;
//        }
//        return false;
//    }
//
//    private void _setDisplaySlot(SAPIDisplaySlot slot, SObjective objective, boolean fromObjective) {
//        slots.put(slot, objective);
//        if (!fromObjective)
//            objective.setDisplaySlot(slot);
//    }
//
//    @Override
//    public SObjective getObjective(SAPIDisplaySlot slot) {
//        return slots.get(slot);
//    }
//
//    @Override
//    public SObjective getObjective(String id) {
//        return objectives.get(id.toUpperCase());
//    }
//
//    @Override
//    public List<SObjective> getObjectives() {
//        return new ArrayList<>(objectives.values());
//    }
//    
//    @Override
//    public String getPrintString() {
//        StringBuilder sb = new StringBuilder();
//        for (Entry<SAPIDisplaySlot,SObjective> entry : slots.entrySet()){
//            sb.append("&5").append(entry.getKey()).append(" : ").append(entry.getValue()).append("\n");
//        }
//        return sb.toString();
//    }
//
//    @Override
//    public SEntry createEntry(OfflinePlayer p) {
//        return createEntry( p, p.getName() );
//    }
//
//    @Override
//    public SEntry createEntry(OfflinePlayer p, String displayName) {
//        return handler.getOrCreateEntry(p, displayName);
//    }
//
//    @Override
//    public SEntry createEntry(String id, String displayName) {
//        return handler.getOrCreateEntry(id, displayName);
//    }
//    @Override
//    public SEntry removeEntry(OfflinePlayer p) {
//        SEntry sb = handler.getEntry(p);
//        if (sb != null){
//            return removeEntry(sb);}
//        return null;
//    }
//    
//    @Override
//    public SEntry getEntry(String id) {
//        return handler.getEntry(id);
//    }
//
//    @Override
//    public SEntry getEntry(OfflinePlayer player) {
//        return handler.getEntry(player);
//    }
//
//    @Override
//    public void clear() { objectives.clear(); }
//
//    @Override
//    public SEntry getOrCreateEntry(OfflinePlayer p) {
//        return handler.getOrCreateEntry(p);
//    }
//
//    @Override
//    public Collection<SEntry> getEntries() {
//        return handler.getEntries();
//    }
//
//
//    @Override
//    public boolean setEntryDisplayName(String id, String name) {
//        SEntry e = handler.getEntry(id);
//        if (e == null)
//            return false;
//        setEntryDisplayName(e, name);
//        return true;
//    }
//
//    @Override
//    public boolean setEntryNamePrefix(String id, String name) {
//        SEntry e = handler.getEntry(id);
//        if (e == null)
//            return false;
//        setEntryNamePrefix(e, name);
//        return true;
//    }
//    
//    @Override
//    public boolean setEntryNameSuffix(String id, String name) {
//        SEntry e = handler.getEntry(id);
//        if (e == null)
//            return false;
//        setEntryNameSuffix(e, name);
//        return true;
//    }
//}
