package mc.alk.arena.competition;

import mc.alk.arena.controllers.StateController;
import mc.alk.arena.objects.CompetitionTransition;

/**
 * @author alkarin
 */
enum TournamentTransition implements CompetitionTransition{
    FIRSTPLACE ("firstPlace"), PARTICIPANTS("participants");

    final String name;
    int globalOrdinal;
    TournamentTransition(String name){
        globalOrdinal = StateController.register(this.getClass());
        this.name = name;
    }

    @Override
    public int globalOrdinal() {
        return globalOrdinal;
    }
    @Override
    public String toString(){
        return name;
    }
}

