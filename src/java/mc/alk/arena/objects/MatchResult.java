package mc.alk.arena.objects;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.objects.teams.ArenaTeam;

/**
 * @author alkarin
 */
public class MatchResult {
    @Getter Set<ArenaTeam> victors = new HashSet<>();
    @Getter Set<ArenaTeam> losers = new HashSet<>();
    @Getter Set<ArenaTeam> drawers = new HashSet<>();
    @Getter WinLossDraw result = WinLossDraw.UNKNOWN;
    @Getter @Setter SortedMap<Integer, Collection<ArenaTeam>> ranking;

    public MatchResult(){}
    public MatchResult(MatchResult r) {
        result = r.getResult();
        victors.addAll(r.getVictors());
        losers.addAll(r.getLosers());
        drawers.addAll(r.getDrawers());
    }

    /**
     * Changes the outcome type of this match to the given type.
     * Example, adding winners to this match will not change the outcome,
     * unless this match is set to a WinLossDraw.WIN
     * @param _wld The WinLossDraw type.
     */
    public void setResult(WinLossDraw _wld){
        result = _wld;
    }
    public void setVictor(ArenaTeam vic) {
        victors.clear();
        victors.add(vic);
        result = WinLossDraw.WIN;
    }
    public void setVictors(Collection<ArenaTeam> victors) {
        victors.clear();
        victors.addAll(victors);
        result = WinLossDraw.WIN;
    }
    public void setDrawers(Collection<ArenaTeam> drawers) {
        drawers.clear();
        drawers.addAll(drawers);
        result = WinLossDraw.DRAW;
    }
    public void setLosers(Collection<ArenaTeam> losers) {
        losers.clear();
        losers.addAll(losers);
    }
    public void addLosers(Collection<ArenaTeam> losers) { losers.addAll(losers); }
    public void addLoser(ArenaTeam loser) { losers.add(loser); }
    public void removeLosers(Collection<ArenaTeam> teams) { losers.removeAll(teams); }
    public void removeDrawers(Collection<ArenaTeam> teams) { drawers.removeAll(teams); }
    public void removeVictors(Collection<ArenaTeam> teams) { victors.removeAll(teams); }
    
    @Override
    public String toString(){
        return "[" + result + ",victor=" + victors + ",losers=" + losers + ",drawers=" + drawers + "]" + toPrettyString();
    }

    public String toPrettyString() {
        if (victors.isEmpty()){
            return "&eThere are no victors yet";}
        StringBuilder sb = new StringBuilder();
        for (ArenaTeam t: victors){
            sb.append(t.getTeamSummary()).append(" ");}
        sb.append(" &ewins vs ");
        for (ArenaTeam t: losers){
            sb.append(t.getTeamSummary()).append(" ");}

        return sb.toString();
    }
    public boolean isUnknown() { return result == WinLossDraw.UNKNOWN; }
    public boolean isDraw() { return result == WinLossDraw.DRAW; }
    public boolean isWon(){ return hasVictor(); }
    public boolean isLost() { return result == WinLossDraw.LOSS; }
    public boolean isFinished(){ return result == WinLossDraw.WIN || result == WinLossDraw.DRAW; }
    public boolean hasVictor() { return result == WinLossDraw.WIN; }
}
