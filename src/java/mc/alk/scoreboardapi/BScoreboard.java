package mc.alk.scoreboardapi;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

public class BScoreboard extends SAPIScoreboard{

    protected Scoreboard board;

    HashMap<String,Scoreboard> oldBoards = new HashMap<String,Scoreboard>();

    public BScoreboard(Plugin plugin, String name) {
        super(plugin, name);
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        board = manager.getNewScoreboard();
    }

    @Override
    public SAPIObjective registerNewObjective(String id, String displayName, String criteria,
                                              SAPIDisplaySlot slot) {
        BObjective o =  new BObjective(this,id, displayName,criteria);
        o.setDisplayName(displayName);
        o.setDisplaySlot(slot);
        registerNewObjective(o);
        return o;
    }

    @Override
    public void setScoreboard(final Player p) {
        if (p.getScoreboard() != null && !oldBoards.containsKey(p.getName()))
            oldBoards.put(p.getName(), p.getScoreboard());
        new BukkitRunnable(){
            @Override
            public void run() {
                if (oldBoards.containsKey(p.getName())) /// only set them if they havent been removed already
                    p.setScoreboard(board);
            }
        }.runTask(plugin);
    }

    @Override
    public void removeScoreboard(Player player) {
        try {
            Scoreboard b = oldBoards.remove(player.getName());
            if (b != null){
                player.setScoreboard(b);
            } else {
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void transferOldScoreboards(BScoreboard oldScoreboard) {
        this.oldBoards.putAll(oldScoreboard.oldBoards);
    }

    private class BoardUpdate{
        BoardUpdate(HashMap<Objective, Integer> scores, Team team){ this.scores=scores; this.team = team;}
        HashMap<Objective, Integer> scores;
        Team team;
    }

    private BoardUpdate clearBoard(SEntry e){
        HashMap<Objective, Integer> oldScores = new HashMap<Objective, Integer>();
        Set<Score> scores = board.getScores(e.getOfflinePlayer());
        for (Score score : scores) {
            oldScores.put(score.getObjective(), score.getScore());
        }
        board.resetScores(e.getOfflinePlayer());
        Team t = board.getPlayerTeam(e.getOfflinePlayer());
        if (t != null)
            t.removePlayer(e.getOfflinePlayer());

        return new BoardUpdate(oldScores, t);
    }

    private void updateBoard(SEntry e, BoardUpdate bu){
        if (bu.team != null){
            bu.team.addPlayer(e.getOfflinePlayer());
        }
        for (Entry<Objective, Integer> entry : bu.scores.entrySet()) {
            if (entry.getValue() == 0) {
                entry.getKey().getScore(e.getOfflinePlayer()).setScore(1);
            }
            entry.getKey().getScore(e.getOfflinePlayer()).setScore(entry.getValue());
        }
    }

    @Override
    public void setEntryDisplayName(SEntry e, String name) {
        BoardUpdate bu = clearBoard(e);
        super.setEntryDisplayName(e, name);
        updateBoard(e, bu);
    }

    @Override
    public void setEntryNamePrefix(SEntry e, String name) {
        BoardUpdate bu = clearBoard(e);
        super.setEntryNamePrefix(e, name);
        updateBoard(e, bu);
    }

    @Override
    public void setEntryNameSuffix(SEntry e, String name) {
        BoardUpdate bu = clearBoard(e);
        super.setEntryNameSuffix(e, name);
        updateBoard(e, bu);
    }

    @Override
    public SEntry removeEntry(SEntry e) {
        board.resetScores(e.getOfflinePlayer()); /// Note: slow spigot operation
        Team t = board.getPlayerTeam(e.getOfflinePlayer());
        if (t != null) {
            t.removePlayer(e.getOfflinePlayer());} /// Note: slow spigot operation
        return super.removeEntry(e);
    }

    public Scoreboard getBukkitScoreboard(){
        return board;
    }

    @Override
    public SAPITeam createTeamEntry(String id, String displayName) {
        SAPITeam st = this.getTeam(id);
        if (st!=null)
            return st;
        Team t = this.board.getTeam(id);
        if (t == null){
            t = this.board.registerNewTeam(id);
        }

        t.setDisplayName(displayName);
        BukkitTeam bt = new BukkitTeam(this, t);
        handler.registerEntry(bt);
        return bt;
    }

    @Override
    public SAPITeam getTeam(String id) {
        SEntry e = handler.getEntry(id);
        return (e == null || !(e instanceof SAPITeam)) ? null : (SAPITeam)e;
    }

    public void addAllEntries(SObjective objective) {
        for (SEntry entry : handler.getEntries()){
            objective.addEntry(entry, 0);
        }
    }

    @Override
    public boolean hasThisScoreboard(Player player) {
        return board!=null && player.getScoreboard() != null && player.getScoreboard().equals(board);
    }
}
