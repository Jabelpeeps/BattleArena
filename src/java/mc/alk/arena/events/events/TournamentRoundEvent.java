package mc.alk.arena.events.events;

import mc.alk.arena.competition.AbstractComp;

/**
 * @author alkarin
 */
public class TournamentRoundEvent extends EventEvent {

    final int round;

    public TournamentRoundEvent(AbstractComp event, int round){
        super(event);
        this.round = round;
    }

    public int getRound() {
        return round;
    }

}
