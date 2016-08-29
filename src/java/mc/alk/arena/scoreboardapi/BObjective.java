package mc.alk.arena.scoreboardapi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.bukkit.OfflinePlayer;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;

import mc.alk.arena.util.Log;

public class BObjective extends SAPIObjective {
    Objective o;

    TreeSet<SAPIScore> scores;
    Set<SEntry> cur15 = new HashSet<>();
    int worst = Integer.MAX_VALUE;

    public BObjective(SScoreboard board, String id,String displayName, String criteria) {
        this(board,id,displayName,criteria,50);
    }

    public BObjective(String id, String displayName, String criteria, int priority) {
        this(null,id, displayName, criteria,priority);
    }

    public BObjective(SScoreboard board, String id,String displayName, String criteria, int priority) {
        super(id,displayName, criteria,priority);
        if (board != null)
            setScoreboard(board);
        scores = new TreeSet<>( 
                ( o1, o2)  -> {
                        int c = o2.getScore() - o1.getScore();
                        if (c != 0)
                            return c;
                        return o1.getEntry().getId().compareTo(o2.getEntry().getId());
                });
    }

    @Override
    public void setDisplayName(String displayName){
        super.setDisplayName(displayName);
        if (o == null)
            return;
        _setDisplayName();
    }

    @Override
    public void setDisplayNameSuffix(String suffix){
        super.setDisplayNameSuffix(suffix);
        if (o == null)
            return;
        _setDisplayName();
    }

    @Override
    public void setDisplayNamePrefix(String prefix){
        super.setDisplayNameSuffix(prefix);
        if (o == null)
            return;
        _setDisplayName();
    }

    @Override
    protected void _setDisplayName(){
        super._setDisplayName();
        if (o == null)
            return;
        o.setDisplayName(getDisplayName());
    }

    @Override
    public void setScoreboard(SScoreboard board) {
        super.setScoreboard(board);

        o = board.bukkitScoreboard.getObjective(id);
        if (o == null)
            o = board.bukkitScoreboard.registerNewObjective(id,criteria);
        setDisplayName(getDisplayName());
    }

    public static void t(){
        Log.debug(":");
    }

    @Override
    public void initPoints(List<SEntry> _entries, List<Integer> points) {
        for (int i=0;i<_entries.size();i++) {
            SEntry e = _entries.get(i);
//            if (entries.containsKey(e)) {
////                return entries.get(e);
//                //todo set points
//                continue;
//            }
            int point = points.get(i);
            SAPIScore o = new SAPIScore(e, point);
            entries.put(e, o);
//            _setScore(e, point);
            scores.add(o);
        }
        int i = 0;
        Iterator<SAPIScore> iter = scores.iterator();
        while(iter.hasNext() && i++ < 16) {
            SAPIScore sc = iter.next();
            _setScore(sc.getEntry(), sc.getScore());
        }
    }

    @Override
    protected boolean setPoints(SAPIScore o, int points) {
        if ( (o.getEntry() instanceof STeam && isDisplayTeams()) ||
                (o.getEntry() instanceof SAPIPlayerEntry && isDisplayPlayers()) ||
                (!(o.getEntry() instanceof SAPIPlayerEntry) && !(o.getEntry() instanceof STeam))){
            addScore(o, points);
        } else {
            super.setPoints(o, points);
        }
        return true;
    }


    private void addScore(final SAPIScore e, final int points) {
        scores.remove(e);
        e.setScore(points);
        scores.add(e);

        if (scores.size() <= SAPI.MAX_ENTRIES) {
            _setScore(e.getEntry(), points);
            cur15.add(e.getEntry());
            worst = Math.min(points, worst);
        } else {
            HashSet<SEntry> now15 = new HashSet<>(SAPI.MAX_ENTRIES);
            ArrayList<SAPIScore> added = new ArrayList<>(2);
            Iterator<SAPIScore> iter = scores.iterator();
            for (int i = 0; i < SAPI.MAX_ENTRIES && iter.hasNext(); i++) {
                SAPIScore sapiScore = iter.next();
                now15.add(sapiScore.getEntry());
                if (!cur15.contains(sapiScore.getEntry())) {
                    added.add(sapiScore);
                }
            }
            cur15.removeAll(now15);
            for ( SEntry se : cur15 ) {
                o.getScoreboard().resetScores(se.getOfflinePlayer().getName());
            }
            cur15 = now15;
            if (added.isEmpty()) {
                if (cur15.contains(e.getEntry())) {
                    _setScore(e.getEntry(), points);
                }
            } else {
                for (SAPIScore se : added) {
                    _setScore(se.getEntry(), se.getScore());
                }
            }
        }

    }

    private void _setScore(final SEntry e, final int points) {
        Score sc = o.getScore(e.getOfflinePlayer().getName());
        if (points != 0) {
            sc.setScore(points);
        } else {
            /// flip from 1 to 0 (this is needed for board setup to display 0 values of initial entries
            sc.setScore(1);
            sc.setScore(0);
        }
    }


    @Override
    public int getPoints(SEntry l) {
        OfflinePlayer p = l.getOfflinePlayer();
        return o.getScore(p.getName()).getScore();
    }

    @Override
    public SObjective setDisplaySlot(SAPIDisplaySlot slot) {
        super.setDisplaySlot(slot);
        if (scoreboard == null)
            return this;
        if (o != null && scoreboard.getObjective(slot)==this){
            o.setDisplaySlot(toBukkitDisplaySlot(slot));
        }
        return this;
    }

    @Override
    public void setDisplayPlayers(boolean display){
        if (display == isDisplayPlayers())
            return;
        displayPlayers = display;
        setDisplay();
    }

    @Override
    public void setDisplayTeams(boolean display){
        if (display == isDisplayTeams())
            return;
        displayTeams = display;
        setDisplay();
    }


    private void setDisplay() {
        scores.clear();
        cur15.clear();
        if (this.scoreboard != null) {
            for (SEntry entry : this.scoreboard.getEntries()) {
                if (!contains(entry))
                    continue;
                if ((displayPlayers && entry instanceof SAPIPlayerEntry) ||
                        (displayTeams && entry instanceof STeam) ||
                        (!(entry instanceof SAPIPlayerEntry) && !(entry instanceof STeam))) {
                    SAPIScore sc = entries.get(entry);
                    addScore(sc, sc.getScore());
                } else {
                    o.getScoreboard().resetScores(entry.getOfflinePlayer().getName());
                }
            }
        }
    }

    public static DisplaySlot toBukkitDisplaySlot(SAPIDisplaySlot slot) {
        switch (slot){
            case BELOW_NAME:
                return DisplaySlot.BELOW_NAME;
            case PLAYER_LIST:
                return DisplaySlot.PLAYER_LIST;
            case SIDEBAR:
                return DisplaySlot.SIDEBAR;
            case NONE:
                return null;
            default:
                return null;
        }
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("&6 --- ").append(this.id).append(" : ").append(this.getDisplayName()).
                append("&4 : ").append(this.getPriority()).append("\n");
        if (scoreboard == null){
            sb.append("&4 Bukkit scoreboard still not set!!");
            return sb.toString();
        }
        Collection<SEntry> es = scoreboard.getEntries();
        if (o == null){
            sb.append("&4 Bukkit Objective still not set!!");
            return sb.toString();
        }
        List<SEntry> zeroes = new ArrayList<>();
        List<SEntry> skipped = new ArrayList<>();
        for (SEntry e: es){
            //			OfflinePlayer p = this.getDisplaySlot()==SAPIDisplaySlot.PLAYER_LIST ? e.getPlayerListName() : e.getOfflinePlayer();
            if (!this.contains(e)){
                skipped.add(e);
                continue;
            }
            Set<Score> scores = o.getScoreboard().getScores(e.getOfflinePlayer().getName());
            for (Score score : scores){
                if (score.getObjective().equals(o)){
                    if (score.getScore() != 0){
                        if (e instanceof BukkitTeam){
                            BukkitTeam bt = ((BukkitTeam)e);
                            sb.append("&e ").append(e.getId()).append(" : ").append(e.getDisplayName()).append(" = ").
                                    append(score.getScore()).append("  &eteamMembers=\n");
                            for (String p : bt.getPlayers()){
                                SEntry ep = this.getScoreboard().getOrCreateEntry(p);
                                String c = this.contains(ep) ? "&e" : "&8";
                                sb.append("  ").append(c).append("- &f").append(bt.getPrefix()).append(p).
                                        append(bt.getSuffix()).append(c).append(" = ").
                                        append(o.getScore(p).getScore()).append("\n");
                            }
                        } else {
                            sb.append("&6 ").append(e.getId()).append(" : ").append(e.getDisplayName()).append(" = ").
                                    append(score.getScore()).append("\n");
                        }
                    } else {
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

//    public void setDisplayName(String displayNamePrefix, String displayName, String displayNameSuffix, STeam team) {
//        String display = SAPIUtil.createLimitedString(
//                displayNamePrefix, displayName, displayNameSuffix, SAPI.MAX_OBJECTIVE_DISPLAYNAME_SIZE);
//        handler.setDisplayName(o, team, display);
//    }
}
