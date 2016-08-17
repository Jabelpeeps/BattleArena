package mc.alk.arena.listeners.competition;

import mc.alk.arena.competition.Competition;
import mc.alk.arena.competition.TransitionController;
import mc.alk.arena.events.players.ArenaPlayerKillEvent;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchState;
import mc.alk.arena.objects.arenas.ArenaListener;
import mc.alk.arena.objects.events.ArenaEventHandler;
import mc.alk.tracker.Tracker;
import mc.alk.tracker.objects.WLTRecord.WLT;

public class ArenaPlayerKillListener implements ArenaListener {

    // @ArenaEventHandler
    public void onArenaPlayerKillEvent(ArenaPlayerKillEvent event) {
        Tracker.getPVPInterface().addPlayerRecord(event.getPlayer().getName(), event.getTarget().getName(), WLT.WIN );
    }

    @ArenaEventHandler
    public void onKill(ArenaPlayerKillEvent event) {
        ArenaPlayer ap = event.getPlayer();
        Competition comp = event.getPlayer().getCompetition();
        
        TransitionController.transition(comp, MatchState.ONKILL, ap, ap.getTeam(), true);
    }
}
