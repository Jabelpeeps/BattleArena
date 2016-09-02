package mc.alk.arena.objects.scoreboard;

import java.util.Collection;

import lombok.Getter;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.teams.ArenaTeam;

public class AbridgedScoreboard implements WaitingScoreboard {
    @Getter final ArenaScoreboard scoreboard;
    final ArenaObjective objective;

    public AbridgedScoreboard(MatchParams params) {
        scoreboard = new ArenaScoreboard( String.valueOf(this.hashCode()), params);
        objective = scoreboard.createObjective("waiting",
                "Queue Players", "&6Waiting Players", SAPIDisplaySlot.SIDEBAR, 100);
        objective.setDisplayPlayers(false);
    }

    @Override
    public void addedToTeam(ArenaTeam team, ArenaPlayer player) {
        STeam t = scoreboard.addTeam( team );
        scoreboard.addedToTeam( t, player);
        scoreboard.setScoreboard(player.getPlayer());
        setTeamSuffix( team, t );
        objective.setTeamPoints(t, team.size());
    }

    @Override
    public void addedToTeam(ArenaTeam team, Collection<ArenaPlayer> players) {
        STeam t = scoreboard.getTeam(team.getIDString());
        for (ArenaPlayer player : players) {
            scoreboard.addedToTeam(team, player);
            scoreboard.setScoreboard(player.getPlayer());
            setTeamSuffix(team, t);
        }
        objective.setTeamPoints(t, team.size());
    }

    @Override
    public void removedFromTeam(ArenaTeam team, ArenaPlayer player) {
        STeam t = scoreboard.getTeam(team.getIDString());
        scoreboard.removeFromTeam(team,player);
        setTeamSuffix(team, t);
        objective.setTeamPoints(t, team.size());
    }

    @Override
    public void removedFromTeam(ArenaTeam team, Collection<ArenaPlayer> players) {
        STeam t = scoreboard.getTeam(team.getIDString());
        for (ArenaPlayer player : players) {
            scoreboard.removeFromTeam(team,player);
            setTeamSuffix(team, t);
        }
        objective.setTeamPoints(t, team.size());
    }

    private void setTeamSuffix(ArenaTeam team, STeam t) {
        String s;
        if (team.getMinPlayers() == team.getMaxPlayers()) {
            s = " " + team.size() + "/" + team.getMinPlayers();
        } else {
            s = " " + team.size() + "/" + team.getMinPlayers() + "/" + team.getMaxPlayers();
        }
        scoreboard.setEntryNameSuffix(t, s);
    }

    @Override
    public boolean addedTeam(ArenaTeam team) {
        STeam t = scoreboard.addTeam(team);
        setTeamSuffix(team, t);
        return true;
    }

    @Override
    public boolean removedTeam(ArenaTeam team) {
        STeam t = scoreboard.getTeam(team.getIDString());
        scoreboard.removeEntry(t);
        return false;
    }

    @Override
    public void setRemainingSeconds(int seconds) { }
}
