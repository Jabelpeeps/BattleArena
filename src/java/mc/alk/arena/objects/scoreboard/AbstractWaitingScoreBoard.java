package mc.alk.arena.objects.scoreboard;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import lombok.Getter;
import mc.alk.arena.BattleArena;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.ArenaSize;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.util.Countdown;
import mc.alk.arena.util.Countdown.CountdownCallback;


public abstract class AbstractWaitingScoreBoard implements WaitingScoreboard, CountdownCallback {

    Map<Integer, LinkedList<SEntry>> reqPlaceHolderPlayers = new HashMap<>();
    Map<Integer, LinkedList<SEntry>> opPlaceHolderPlayers = new HashMap<>();

    @Getter ArenaScoreboard scoreboard;
    ArenaObjective objective;
    final int minTeams;
    Countdown countdown;

    public AbstractWaitingScoreBoard( MatchParams params ) {
        scoreboard = new ArenaScoreboard( String.valueOf( hashCode()), params);
        objective = scoreboard.createObjective( "waiting", "Queue Players", "&6Waiting Players", SAPIDisplaySlot.SIDEBAR, 100);
        objective.setDisplayTeams(false);
        minTeams = params.getMinTeams();
        
        if (    params.getForceStartTime() >0 
                && params.getForceStartTime() != ArenaSize.MAX
                && params.getMaxPlayers() != params.getMinPlayers() ) {
            countdown = new Countdown( BattleArena.getSelf(), params.getForceStartTime(), 1, this );
        }
    }
    protected abstract void removePlaceHolder( int teamIndex );
    protected abstract void addPlaceholder( ArenaTeam team, STeam t, boolean optionalTeam );
    
    protected int getReqSize(int teamIndex) {
        return reqPlaceHolderPlayers.containsKey(teamIndex) ? reqPlaceHolderPlayers.get(teamIndex).size()                                                            : 0;
    }
    
    @Override
    public boolean addedTeam( ArenaTeam team ) {
        scoreboard.createTeamEntry( String.valueOf( team.getIndex() ), "");
        for ( ArenaPlayer ap : team.getPlayers() ) {
            addedToTeam( team, ap );
        }
        return true;
    }

    @Override
    public boolean removedTeam(ArenaTeam team) {
        STeam t = scoreboard.getTeam(String.valueOf(team.getIndex()));
        scoreboard.removeEntry(t);
        return false;
    }

    @Override
    public void addedToTeam( ArenaTeam team, Collection<ArenaPlayer> players ) {
        for ( ArenaPlayer player : players ) {
            addedToTeam( team, player );
        }
    }

    @Override
    public void addedToTeam( ArenaTeam team, ArenaPlayer player ) {
        removePlaceHolder( team.getIndex() );
        STeam t = scoreboard.getTeam( String.valueOf( team.getIndex() ) );
        scoreboard.addedToTeam( t, player );
        objective.setPoints( player, 10 );
    }

    @Override
    public void removedFromTeam(ArenaTeam team, Collection<ArenaPlayer> players) {
        STeam t = scoreboard.getTeam( String.valueOf( team.getIndex() ) );
        for ( ArenaPlayer player : players ) {
            scoreboard.removeFromTeam( team, player );
            addPlaceholder(team, t, team.getIndex() >= minTeams );
        }
    }

    @Override
    public void removedFromTeam( ArenaTeam team, ArenaPlayer player ) {
        STeam t = scoreboard.getTeam( String.valueOf( team.getIndex() ) );
        scoreboard.removeFromTeam( t, player );
        addPlaceholder( team, t, team.getIndex() >= minTeams );
    }

    @Override
    public void setRemainingSeconds(int seconds) {
        if ( countdown != null )
            countdown.stop();
        
        countdown = new Countdown( BattleArena.getSelf(), seconds, 1, this );
    }

    @Override
    public boolean intervalTick( int secondsRemaining ) {
        if (secondsRemaining == 0) {
            objective.setDisplayNameSuffix( "" );
        } else {
            objective.setDisplayNameSuffix(" &e(" + secondsRemaining + ")" );
        }
        return true;
    }
}
