package mc.alk.arena.objects.victoryconditions;

import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import org.bukkit.configuration.ConfigurationSection;

import mc.alk.arena.competition.Match;
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
import mc.alk.arena.tracker.TrackerInterface;

public class KillLimit extends VictoryCondition implements ScoreTracker{
    final ArenaObjective kills;
    final TrackerInterface sc;
    final int numKills;
    final int playerKillPoints;

    public KillLimit(Match _match, ConfigurationSection section) {
        super(_match);
        numKills = section.getInt("numKills", 50);
        playerKillPoints = section.getInt("points.player", 1);
        
        kills = new ArenaObjective( getClass().getSimpleName(),
                                    section.getString( "displayName","&4Kill Limit" ), 
                                    section.getString( "criteria", "&eFirst to &4" + numKills ),
                                    SAPIDisplaySlot.SIDEBAR, 
                                    60 );
        boolean isRated = _match.getParams().isRated();
        boolean soloRating = !_match.getParams().isTeamRating();
        sc = (isRated && soloRating) ? Tracker.getInterface( _match.getParams() ) : null;
    }

    @ArenaEventHandler(priority=ArenaEventPriority.LOW)
    public void playerKillEvent(ArenaPlayerKillEvent event) {
        kills.addPoints(event.getPlayer(), playerKillPoints);
        Integer points = kills.addPoints(event.getTeam(), playerKillPoints);
        if (sc != null)
            sc.addPlayerRecord(event.getPlayer().getName(),event.getTarget().getName(), WLT.WIN);
        if (points >= numKills){
            this.match.setVictor(event.getTeam());
        }

    }

    @ArenaEventHandler(priority = ArenaEventPriority.LOW)
    public void onFindCurrentLeader(MatchFindCurrentLeaderEvent event) {
        if (event.isMatchEnding()){
            event.setResult(kills.getMatchResult(match));
        } 
        else {
            Collection<ArenaTeam> leaders = kills.getLeaders();
            if (leaders.size() > 1)
                event.setCurrentDrawers(leaders);
            else
                event.setCurrentLeaders(leaders);
        }
    }
    @Override
    public List<ArenaTeam> getLeaders() { return kills.getLeaders(); }
    @Override
    public TreeMap<Integer, Collection<ArenaTeam>> getRanks() { return kills.getRanks(); }
    @Override
    public void setScoreboard(ArenaScoreboard scoreboard) {
        kills.setScoreboard(scoreboard);
        scoreboard.addObjective(kills);
    }
    @Override
    public void setDisplayTeams(boolean display) { kills.setDisplayPlayers(display); }
}
