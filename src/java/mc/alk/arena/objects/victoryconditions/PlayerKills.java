package mc.alk.arena.objects.victoryconditions;

import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import org.bukkit.configuration.ConfigurationSection;

import mc.alk.arena.competition.Match;
import mc.alk.arena.controllers.tracker.TrackerInterface;
import mc.alk.arena.events.matches.MatchFindCurrentLeaderEvent;
import mc.alk.arena.events.players.ArenaPlayerKillEvent;
import mc.alk.arena.objects.events.ArenaEventHandler;
import mc.alk.arena.objects.events.ArenaEventPriority;
import mc.alk.arena.objects.scoreboard.ArenaObjective;
import mc.alk.arena.objects.scoreboard.ArenaScoreboard;
import mc.alk.arena.objects.scoreboard.SAPIDisplaySlot;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.objects.tracker.WLTRecord.WLT;
import mc.alk.arena.objects.victoryconditions.interfaces.ScoreTracker;
import mc.alk.arena.tracker.Tracker;

public class PlayerKills extends VictoryCondition implements ScoreTracker{
    final ArenaObjective kills;
    final TrackerInterface sc;
    final int points;

    public PlayerKills( Match _match, ConfigurationSection section ) {
        super(_match);
        points = section.getInt("points.player", 1);
        kills = new ArenaObjective( getClass().getSimpleName(), 
                                    section.getString( "displayName", "Player Kills" ), 
                                    section.getString("criteria", "Kill Players"),
                                    SAPIDisplaySlot.SIDEBAR, 
                                    60 );

        sc = (  _match.getParams().isRated() 
                && !_match.getParams().isTeamRating() ) ? Tracker.getInterface( _match.getParams() ) 
                                                        : null;
    }
    @ArenaEventHandler( priority = ArenaEventPriority.LOW )
    public void playerKillEvent(ArenaPlayerKillEvent event) {
        kills.addPoints(event.getPlayer(), points);
        kills.addPoints(event.getTeam(), points);
        if (sc != null)
            sc.addPlayerRecord( event.getPlayer().getName(), event.getTarget().getName(), WLT.WIN);
    }
    @ArenaEventHandler( priority = ArenaEventPriority.LOW )
    public void onFindCurrentLeader(MatchFindCurrentLeaderEvent event) {
        event.setResult( kills.getMatchResult(match) );
    }

    @Override
    public List<ArenaTeam> getLeaders() { return kills.getLeaders(); }
    @Override
    public TreeMap<Integer,Collection<ArenaTeam>> getRanks() { return kills.getRanks(); }
    @Override
    public void setDisplayTeams(boolean display) { kills.setDisplayTeams(display); }

    @Override
    public void setScoreboard(ArenaScoreboard scoreboard) {
        kills.setScoreboard(scoreboard);
        scoreboard.addObjective(kills);
    }
}
