package mc.alk.arena.controllers.joining.scoreboard;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import lombok.NoArgsConstructor;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.scoreboard.SEntry;
import mc.alk.arena.objects.scoreboard.STeam;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.objects.teams.TeamFactory;
import mc.alk.arena.util.PlayerUtil;

public class FullScoreboard extends AbstractWaitingScoreBoard {

    public FullScoreboard(MatchParams params, List<ArenaTeam> teams) {
        super( params );
        
        int maxTeams = params.getMaxTeams();

        List<ArenaTeam> ateams = new ArrayList<>();
        List<STeam> steams = new ArrayList<>();
        
        for (int i = 0; i < maxTeams; i++) {
            ArenaTeam team = i < teams.size() ? teams.get(i) : TeamFactory.createCompositeTeam(i, params);

            team.setIDString(String.valueOf(team.getIndex()));
            STeam t = scoreboard.addTeam(team);

            steams.add(t);
            ateams.add(team);
        }
        addPlaceholders(ateams, steams, minTeams); 
    }

    private void addPlaceholders(List<ArenaTeam> ateams, List<STeam> steams, int _minTeams) {
        List<SEntry> es = new ArrayList<>();
        List<Integer> points = new ArrayList<>();
        for (int i=0;i < ateams.size();i++) {
            ArenaTeam at = ateams.get(i);
            STeam st = steams.get(i);
            for (int j = 0; j < ateams.get(i).getMaxPlayers(); j++) {
                TempEntry te = createEntry(at, st, j >= _minTeams);
                SEntry e = scoreboard.getEntry(te.name);
                if (e == null) {
                    e = scoreboard.createEntry( te.name, te.name);
                }
                te.r.addLast(e);
                es.add(e);
                points.add(te.points);
                st.addPlayer( PlayerUtil.getOfflinePlayer(te.name), Integer.MIN_VALUE );
            }
        }
        objective.initPoints( es, points );
    }

    @NoArgsConstructor
    private class TempEntry{
        String name;
        int points;
        LinkedList<SEntry> r;
    }

    private TempEntry createEntry(ArenaTeam team, STeam t, boolean optionalTeam){
        TempEntry te = new TempEntry();
        String name;
        if (!optionalTeam && getReqSize(team.getIndex()) < team.getMinPlayers()) {
            te.r = reqPlaceHolderPlayers.get(team.getIndex());
            if (te.r == null) {
                te.r = new LinkedList<>();
                reqPlaceHolderPlayers.put(team.getIndex(), te.r);
            }
            name = "needed";
            te.points = 1;
        } 
        else {
            te.r = opPlaceHolderPlayers.get(team.getIndex());
            if (te.r == null) {
                te.r = new LinkedList<>();
                opPlaceHolderPlayers.put(team.getIndex(), te.r);
            }
            name = "open";
            te.points = 0;
        }
        te.name = "- " + name + " -" + team.getTeamChatColor();
        return te;
    }

    @Override
    protected void addPlaceholder(ArenaTeam team, STeam t, boolean optionalTeam) {
        TempEntry te = createEntry(team, t, optionalTeam);
        SEntry e = scoreboard.getEntry(te.name);
        if (e == null) {
            e = scoreboard.createEntry(te.name, te.name);
            objective.addEntry(e, te.points);
        } 
        else {
            objective.setPoints(e, te.points);
        }
        te.r.addLast(e);
        t.addPlayer( PlayerUtil.getOfflinePlayer(te.name) );
    }

    @Override
    protected void removePlaceHolder(int teamIndex){
        LinkedList<SEntry> list = reqPlaceHolderPlayers.get(teamIndex);
        if (list == null || list.isEmpty()) {
            list = opPlaceHolderPlayers.get(teamIndex);
        }
        if (list == null || list.isEmpty()) {
            return;
        }
        SEntry e = list.removeLast();
        scoreboard.removeEntry(e);
    }
}
