package mc.alk.arena.events.events;

import lombok.Getter;
import mc.alk.arena.competition.AbstractComp;

/**
 * @author alkarin
 */
public class TournamentRoundEvent extends EventEvent {

    @Getter final int round;

    public TournamentRoundEvent(AbstractComp event, int _round){
        super(event);
        round = _round;
    }
}
