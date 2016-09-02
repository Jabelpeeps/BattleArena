package mc.alk.arena.objects.scoreboard;

import java.util.LinkedList;
import java.util.List;

import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.objects.teams.TeamFactory;
import mc.alk.arena.util.PlayerUtil;

public class CutoffScoreboard extends AbstractWaitingScoreBoard {
    
    public CutoffScoreboard( MatchParams params, List<ArenaTeam> teams ) {
        super( params );
  
        int maxTeams = params.getMaxTeams();
        int count = 0;
        int ppteam = 15;
        if (maxTeams < 16) {
            ppteam = 15 / maxTeams;
        }
        for ( int i = 0; i < maxTeams && count < 15; i++ ) {
            
            ArenaTeam team = i < teams.size() ? teams.get(i) 
                                              : TeamFactory.createCompositeTeam(i, params);
            team.setIDString(String.valueOf(team.getIndex()));
            STeam t = scoreboard.addTeam(team);
            for (int j = 0; j < team.getMaxPlayers() && count < 15 && j < ppteam; j++) {
                count++;
                addPlaceholder(team, t, i >= minTeams);
            }
        }
    }

    @Override
    protected void addPlaceholder(ArenaTeam team, STeam t, boolean optionalTeam) {
        String name;
        LinkedList<SEntry> r;
        int points;
        
        if (!optionalTeam && getReqSize(team.getIndex()) < team.getMinPlayers()) {
            r = reqPlaceHolderPlayers.get(team.getIndex());
            if (r == null) {
                r = new LinkedList<>();
                reqPlaceHolderPlayers.put(team.getIndex(), r);
            }
            name = "needed";
            points = 1;
        } 
        else {
            r = opPlaceHolderPlayers.get(team.getIndex());
            if (r == null) {
                r = new LinkedList<>();
                opPlaceHolderPlayers.put(team.getIndex(), r);
            }
            name = "open";
            points = 0;
        }

        String dis = "- " + name + " -" + team.getTeamChatColor();
        SEntry entry = scoreboard.getEntry(dis);
        if ( entry == null ) {
            entry = scoreboard.createEntry( name, dis );
            objective.addEntry(entry, points);
        } 
        else {
            objective.setPoints(entry, points);
        }

        r.addLast(entry);
        t.addPlayer( PlayerUtil.getOfflinePlayer( entry.getBaseDisplayName() ) );
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
