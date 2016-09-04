package mc.alk.arena.objects.victoryconditions;

import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.competition.Match;
import mc.alk.arena.events.matches.MatchFinishedEvent;
import mc.alk.arena.objects.MatchResult;
import mc.alk.arena.objects.events.ArenaEventHandler;
import mc.alk.arena.objects.events.ArenaEventPriority;
import mc.alk.arena.objects.scoreboard.ArenaObjective;
import mc.alk.arena.objects.scoreboard.ArenaScoreboard;
import mc.alk.arena.objects.scoreboard.SAPIDisplaySlot;
import mc.alk.arena.objects.scoreboard.STeam;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.objects.tracker.WLTRecord.WLT;
import mc.alk.arena.objects.victoryconditions.interfaces.DefinesTimeLimit;
import mc.alk.arena.util.Countdown;
import mc.alk.arena.util.Countdown.CountdownCallback;
import mc.alk.arena.util.MessageUtil;

public class TeamTimeLimit extends VictoryCondition implements DefinesTimeLimit, CountdownCallback {

    Countdown timer;
    int announceInterval;
    final ArenaTeam team;

    public TeamTimeLimit( Match _match, ArenaTeam _team ) {
        super(_match);
        team = _team;
    }
    public void startCountdown() {
        timer = new Countdown( BattleArena.getSelf(), match.getParams().getMatchTime(), 1, this );
    }
    public void stopCountdown() { cancelTimers(); }

    @ArenaEventHandler( priority = ArenaEventPriority.LOW )
    public void onFinished(MatchFinishedEvent event) {
        cancelTimers();
    }

    private void cancelTimers() {
        if ( timer != null ) {
            timer.stop();
            timer = null;
        }
    }

    @Override
    public boolean intervalTick(int remaining){
        if (match.isEnding())
            return false;
        if (remaining <= 0) {
            MatchResult cr = new MatchResult();
            cr.setResult(WLT.LOSS);
            cr.addLoser(team);
            match.endMatchWithResult(cr);
        }
        if (!Defaults.USE_SCOREBOARD)
            return true;
        ArenaScoreboard as = match.getScoreboard();
        if (as == null) {
            return true;}
        STeam t = as.getTeam(team.getIDString());
        ArenaObjective ao = as.getObjective(SAPIDisplaySlot.SIDEBAR);
        
        if (t!=null && ao != null ){
            ao.setDisplayName(String.join( "", ao.getDisplayNamePrefix(), ao.getBaseDisplayName(),
                    MessageUtil.colorChat("&e(" + remaining + ")") ) );
        }
        return true;
    }
    @Override
    public int getTime() { return match.getParams().getMatchTime(); }
}
