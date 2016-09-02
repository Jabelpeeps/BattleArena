package mc.alk.arena.controllers.joining;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.Defaults;
import mc.alk.arena.competition.Competition;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.CompetitionSize;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.joining.JoinHandler;
import mc.alk.arena.objects.joining.TeamJoinObject;
import mc.alk.arena.objects.scoreboard.CutoffScoreboard;
import mc.alk.arena.objects.scoreboard.FullScoreboard;
import mc.alk.arena.objects.scoreboard.WaitingScoreboard;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.objects.teams.TeamFactory;
import mc.alk.arena.objects.teams.TeamHandler;

public abstract class AbstractJoinHandler implements JoinHandler, TeamHandler {
    
    public static final TeamJoinResult CANTFIT = new TeamJoinResult( TeamJoinStatus.CANT_FIT, -1 );
    final MatchParams matchParams;
    @Getter final List<ArenaTeam> teams = new CopyOnWriteArrayList<>();
    final int minTeams, maxTeams;
    @Setter Competition competition;
    WaitingScoreboard scoreboard;
    @Getter int nPlayers;

    public static enum TeamJoinStatus {
        ADDED, CANT_FIT, ADDED_TO_EXISTING, ADDED_STILL_NEEDS_PLAYERS
    }

    @AllArgsConstructor
    public static class TeamJoinResult {
        @Getter final public TeamJoinStatus joinStatus;
        @Getter final public int remaining;
    }

    public AbstractJoinHandler( MatchParams params, Competition _competition, List<ArenaTeam> _teams ) {
        matchParams = params;
        minTeams = params.getMinTeams();
        maxTeams = params.getMaxTeams();
        competition = _competition;
        if ( Defaults.USE_SCOREBOARD )
            initWaitingScoreboard(_teams);
    }

    private void initWaitingScoreboard( List<ArenaTeam> startingTeams ) {
        List<ArenaTeam> tems = new ArrayList<>();

        int needed = 0;
        int optional = 0;
        
        if ( startingTeams.size() < maxTeams )
            tems.addAll( startingTeams );
        
        if ( maxTeams <= 16 ) {
            
            int index = 0;
            while ( tems.size() < maxTeams )
                tems.add( TeamFactory.createCompositeTeam( index++, matchParams ) );
            
            for ( ArenaTeam team : tems ) {
                
                if ( team.getMinPlayers() < 16 ) {
                    needed += team.getMinPlayers();
                    
                    if ( team.getMinPlayers() != team.getMaxPlayers() ) {
                        optional += team.getMaxPlayers() < 1000 ? team.getMaxPlayers() - team.getMinPlayers() 
                                                                : 1000;
                    }
                }
            }
        }
             
        if ( needed + optional <= 16 ) 
            scoreboard = new FullScoreboard( matchParams, tems );
        else
            scoreboard = new CutoffScoreboard( matchParams, tems );
    }

    public abstract boolean switchTeams(ArenaPlayer player, Integer toTeamIndex, boolean checkSizes);

    protected ArenaTeam addToPreviouslyLeftTeam(ArenaPlayer player) {
        for ( ArenaTeam t : teams ) {
            if ( t.hasLeft( player ) ) {
                t.addPlayer( player );
                nPlayers++;
                
                if ( competition != null )
                    competition.addedToTeam( t, player );
                
                if ( scoreboard != null )
                    scoreboard.addedToTeam( t, player );
                return t;
            }
        }
        return null;
    }
    
    public Collection<ArenaPlayer> getPlayers() {
        List<ArenaPlayer> players = new ArrayList<>();
        for (ArenaTeam at: teams) {
            players.addAll(at.getPlayers());
        }
        return players;
    }

    public void joiningPlayer(ArenaPlayer player) {
        ArenaTeam ct = TeamFactory.createCompositeTeam(teams.size(), matchParams);
        addTeam(ct);
        ct.addPlayer(player);
        addToTeam(ct, player);
    }

    public void useWaitingScoreboard(){
        if (scoreboard==null) return;
        
        for (ArenaTeam at: teams){
            for (ArenaPlayer ap: at.getPlayers()){
                scoreboard.getScoreboard().setScoreboard(ap.getPlayer());
            }
        }
    }

    public void setWaitingScoreboardTime(int seconds) {
        if ( scoreboard == null ) return;
        scoreboard.setRemainingSeconds(seconds);
    }

    @Override
    public void addToTeam(ArenaTeam team, Collection<ArenaPlayer> players) {
        team.addPlayers(players);
        for (ArenaPlayer ap : players){
            ap.setTeam(team);
        }
        nPlayers+=players.size();
        if (competition!=null)
            competition.addedToTeam(team,players);
        if (scoreboard!=null)
            scoreboard.addedToTeam(team,players);
    }

    @Override
    public boolean addToTeam(ArenaTeam team, ArenaPlayer player) {
        team.addPlayer(player);
        player.setTeam(team);
        nPlayers++;
        if (competition!=null)
            competition.addedToTeam(team,player);
        if (scoreboard!=null)
            scoreboard.addedToTeam(team,player);
        return true;
    }

    @Override
    public boolean removeFromTeam(ArenaTeam team, ArenaPlayer player) {
        team.removePlayer(player);
        player.setTeam(null);
        nPlayers--;
        if (competition!=null)
            competition.removedFromTeam(team, player);
        if (scoreboard!=null)
            scoreboard.removedFromTeam(team,player);
        return true;
    }

    @Override
    public void removeFromTeam(ArenaTeam team, Collection<ArenaPlayer> players) {
        for (ArenaPlayer ap: players){
            removeFromTeam(team, ap);
        }
    }

    @Override
    public boolean removeTeam(ArenaTeam team) {
        return true;
    }

    @Override
    public boolean addTeam(ArenaTeam team){
        nPlayers += team.size();
        team.setIndex( teams.size() );
        teams.add( team );
        
        if ( competition != null )
            competition.addedTeam( team );
        
        if ( scoreboard != null )
            scoreboard.addedTeam( team );
        return true;
    }

    public abstract TeamJoinResult joiningTeam(TeamJoinObject tqo);

    @Override
    public boolean canLeave(ArenaPlayer p) {
        return true;
    }

    @Override
    public boolean leave(ArenaPlayer p) {
        for (ArenaTeam t: teams){
            if (t.hasMember(p)) {
                nPlayers--;
                t.removePlayer(p);
                if (competition!=null) {
                    competition.removedFromTeam(t,p);
                }
                if (scoreboard != null ) {
                    scoreboard.removedFromTeam(t,p);
                }
                return true;
            }
        }
        return false;
    }

    public Set<ArenaPlayer> getExcludedPlayers() {
        Set<ArenaPlayer> tplayers = new HashSet<>();
        for (ArenaTeam t : teams) {
            if (t.size() < t.getMinPlayers()) {
                tplayers.addAll(t.getPlayers());
            }
            /// greater should never happen

        }
        return tplayers;
    }

    public List<ArenaTeam> removeImproperTeams(){
        List<ArenaTeam> improper = new ArrayList<>();
        for (ArenaTeam t : teams) {
            if (t.size() < t.getMinPlayers()|| t.size() > t.getMaxPlayers()) {
                improper.add(t);
                nPlayers -= t.size();
            }
        }
        teams.removeAll(improper);
        return improper;
    }

    public boolean hasEnough(int allowedTeamSizeDifference){
        final int teamssize = teams.size();
        if (teamssize < minTeams)
            return false;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int valid = 0;
        for (ArenaTeam t: teams){
            final int tsize = t.size();
            if (tsize ==0)
                continue;
            if (tsize < min) min = tsize;
            if (tsize > max) max = tsize;

            if (max - min > allowedTeamSizeDifference)
                return false;

            if (tsize < t.getMinPlayers() || tsize > t.getMaxPlayers())
                continue;
            valid++;
        }
        return valid >= minTeams && valid <= maxTeams;
    }

    public boolean isFull() {
        if (maxTeams == CompetitionSize.MAX )
            return false;
        /// Check to see if we have filled up our number of teams
        if ( maxTeams > teams.size()){
            return false;}
        /// Check to see if there is any space left on the team
        for (ArenaTeam t: teams){
            if (t.size() < t.getMaxPlayers()){
                return false;}
        }
        /// we can't add a team.. and all teams are full
        return true;
    }

    public boolean isEmpty() {
        if (teams.isEmpty())
            return true;
        for (ArenaTeam t: teams){
            if (t.size() != 0){
                return false;}
        }
        return true;
    }

    @Override
    public String toString() {
        return "[TJH " + this.hashCode() + "]";
    }
}
