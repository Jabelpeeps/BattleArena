package mc.alk.arena.objects.scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.options.TransitionOption;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.scoreboardapi.SAPIDisplaySlot;
import mc.alk.arena.scoreboardapi.SEntry;
import mc.alk.arena.scoreboardapi.SObjective;
import mc.alk.arena.scoreboardapi.SScoreboard;
import mc.alk.arena.scoreboardapi.STeam;
import mc.alk.arena.util.Log;

public class ArenaScoreboard {
    final protected SScoreboard board;
    final HashMap<ArenaTeam,STeam> teams = new HashMap<>();
    boolean colorPlayerNames = Defaults.USE_COLORNAMES;

    public ArenaScoreboard(String scoreboardName) {
        board = new SScoreboard( BattleArena.getSelf(), scoreboardName );
    }
    public ArenaScoreboard(String scoreboardName, MatchParams params) {
        this(scoreboardName);
        colorPlayerNames = Defaults.USE_COLORNAMES &&
                (!params.getStateGraph().hasAnyOption(TransitionOption.NOTEAMNAMECOLOR));
    }

    public ArenaObjective createObjective(String id, String criteria, String displayName) {
        return createObjective(id,criteria,displayName,SAPIDisplaySlot.SIDEBAR);
    }

    public ArenaObjective createObjective(String id, String criteria, String displayName, SAPIDisplaySlot slot) {
        return createObjective(id,criteria,displayName,SAPIDisplaySlot.SIDEBAR, 50);
    }

    public ArenaObjective createObjective(String id, String criteria, String displayName, SAPIDisplaySlot slot, int priority) {
        ArenaObjective o = new ArenaObjective(id,criteria,displayName,slot,priority);
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
            board.setScoreboard(p);
        }
        if (colorPlayerNames)
            t.setPrefix(team.getTeamChatColor()+"");
        teams.put(team, t);

        for (SObjective o : this.getObjectives()){
            o.addTeam(t, 0);
            if (o.isDisplayPlayers()){
                for (ArenaPlayer player: team.getPlayers()){
                    o.addEntry(player.getName(), 0);
                }
            }
        }
        return t;
    }
    
    public STeam removeTeam(ArenaTeam team) {
        STeam t = teams.remove(team);
        if (t != null){
            removeEntry(t);
            for (SObjective o : this.getObjectives()){
                o.removeEntry(t);
                for (String player: t.getPlayers()){
                    o.removeEntry(player);
                }
            }
        }
        return t;
    }
    
    public STeam addedToTeam(ArenaTeam team, ArenaPlayer player) {
        STeam t = teams.get(team);
        if (t == null){
            t = addTeam(team);}
        addedToTeam(t,player);
        return t;
    }

    public void addedToTeam(STeam team, ArenaPlayer player){
        team.addPlayer(player.getPlayer());
        board.setScoreboard(player.getPlayer());
    }

    public STeam removedFromTeam(ArenaTeam team, ArenaPlayer player) {
        STeam t = teams.get(team);
        if (t == null) {
            Log.err(teams.size() + "  Removing from a team that doesn't exist player=" + player.getName() + "   team=" + team + "  " + team.getId());
            return null;
        }
        removedFromTeam(t,player);
        return t;
    }

    public void removedFromTeam(STeam team, ArenaPlayer player){
        team.removePlayer(player.getPlayer());
        board.removeScoreboard(player.getPlayer());
    }

    public void leaving(ArenaTeam team, ArenaPlayer player) {
        removedFromTeam(team,player);
    }

    public void setDead(ArenaTeam team, ArenaPlayer player) {
        removedFromTeam(team,player);
    }

    public void addObjective(ArenaObjective scores) {
        board.registerNewObjective(scores);
        board.addAllEntries(scores);
    }

    public List<STeam> getTeams() {
        return new ArrayList<>(teams.values());
    }

    public SObjective registerNewObjective(String objectiveName,
                                           String criteria, String displayName, SAPIDisplaySlot slot) {
        return createObjective(objectiveName,criteria, displayName,slot);
    }

    public void setDisplaySlot(SAPIDisplaySlot slot, SObjective objective) {
        board.setDisplaySlot(slot, objective);
    }

    public void setDisplaySlot(SAPIDisplaySlot slot, SObjective objective,boolean fromObjective) {
        board.setDisplaySlot(slot, objective, fromObjective);
    }

    public SObjective getObjective(SAPIDisplaySlot slot) {
        return board.getObjective(slot);
    }

    public SObjective getObjective(String id) {
        return board.getObjective(id);
    }

    public List<SObjective> getObjectives() {
        return board.getObjectives();
    }

    public String getPrintString() {
        return board.getPrintString();
    }

    public SEntry createEntry(OfflinePlayer p) {
        return board.createEntry(p);
    }

    public SEntry createEntry(OfflinePlayer p, String displayName) {
        return board.createEntry(p,displayName);
    }

    public SEntry createEntry(String id, String displayName) {
        return board.createEntry(id, displayName);
    }

    public STeam createTeamEntry(String id, String displayName) {
        return board.createTeamEntry(id, displayName);
    }

    public SEntry removeEntry(OfflinePlayer p) {
        return board.removeEntry(p);
    }

    public SEntry removeEntry(SEntry e) {
        return board.removeEntry(e);
    }

    public boolean setEntryDisplayName(String id, String name) {
        return board.setEntryDisplayName(id, name);
    }

    public void setEntryDisplayName(SEntry e, String name) {
        board.setEntryDisplayName(e, name);
    }

    public boolean setEntryDisplayName(ArenaPlayer player, String name) {
        return board.setEntryDisplayName(player.getName(), name);
    }

    public boolean setEntryNamePrefix(String id, String name) {
        return board.setEntryNamePrefix(id,name);
    }

    public void setEntryNamePrefix(SEntry entry, String name) {
        board.setEntryNamePrefix(entry,name);
    }

    public boolean setEntryNamePrefix(ArenaPlayer player, String name) {
        return board.setEntryNamePrefix(player.getName(), name);
    }

    public boolean setEntryNameSuffix(String id, String name) {
        return board.setEntryNameSuffix(id, name);
    }

    public void setEntryNameSuffix(SEntry e, String name) {
        board.setEntryNameSuffix(e, name);
    }

    public boolean hasThisScoreboard(Player player) {
        return board.hasThisScoreboard(player);
    }

    public boolean setEntryNameSuffix(ArenaPlayer player, String name) {
        return board.setEntryNameSuffix(player.getName(), name);
    }

    public String getName() {
        return board.getName();
    }

    public SEntry getEntry(String id) {
        return board.getEntry(id);
    }

    public SEntry getEntry(OfflinePlayer player) {
        return board.getEntry(player);
    }

    public STeam getTeam(String id) {
        return board.getTeam(id);
    }

    public void clear() {
        board.clear();
    }

    public SEntry getOrCreateEntry(OfflinePlayer p) {
        return board.getOrCreateEntry(p.getName());
    }

    public Collection<SEntry> getEntries() {
        return board.getEntries();
    }

    public void removeScoreboard(Player player) {
        board.removeScoreboard(player);
    }

    public void setScoreboard(Player player) {
        board.setScoreboard(player);
    }

    public void setObjectiveScoreboard(ArenaObjective arenaObjective) {
        arenaObjective.setScoreboard(board);
    }

    public void setPoints(ArenaObjective objective, ArenaTeam team, int points) {
        objective.setPoints(team, points);
    }

    public void setPoints(ArenaObjective objective, ArenaPlayer player, int points) {
        objective.setPoints(player, points);
    }

    public SScoreboard getBScoreboard() {
        return board;
    }

    public void initPoints(ArenaObjective objective, List<SEntry> es, List<Integer> points) {
        objective.initPoints(es, points);
    }
    @Override
    public String toString(){
        return getPrintString();
    }
}
