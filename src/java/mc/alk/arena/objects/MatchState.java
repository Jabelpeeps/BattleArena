package mc.alk.arena.objects;

import mc.alk.arena.controllers.StateController;
import mc.alk.arena.objects.options.TransitionOption;

/**
 * @author alkarin
 *
 * Enum of StateGraph, and MatchStates
 */
public enum MatchState implements CompetitionTransition {

    NONE("None"), 
    DEFAULTS("defaults"),
    ONENTER("onEnter"), 
    ONLEAVE("onLeave"), 
//  ONENTERWAITROOM("onEnterWaitRoom"),
    ONENTERARENA("onEnterArena"), 
    ONLEAVEARENA("onLeaveArena"), 
//  ONENTERWAITROOM("onEnterWaitRoom"),
    INQUEUE("inQueue"), 
    INCOURTYARD("inCourtyard"),
    INLOBBY("inLobby"), 
    INWAITROOM("inWaitroom"), 
    INSPECTATE("inSpectate"),
    INARENA("inArena"),
    ONCREATE("onCreate"),
    PREREQS("preReqs"), 
    ONJOIN("onJoin"), 
    INJOIN("inJoin"),
    ONOPEN("onOpen"), 
    INOPEN("inOpen"), 
    ONBEGIN("onBegin"),
    ONPRESTART("onPreStart"), 
    INPRESTART("inPrestart"),
    ONSTART("onStart"), 
    INGAME("inGame"),
    ONVICTORY("onVictory"), 
    INVICTORY("inVictory"),
    ONCOMPLETE("onComplete"), 
    ONCANCEL("onCancel"), 
    ONFINISH("onFinish"),
    ONSPAWN("onSpawn"), 
    ONDEATH("onDeath"), 
    ONKILL("onKill"),
    WINNERS("winners"), 
    DRAWERS("drawers"), 
    LOSERS("losers"),
    ONMATCHINTERVAL("onMatchInterval"), 
    ONMATCHTIMEEXPIRED("onMatchTimeExpired"),
    ONCOUNTDOWNTOEVENT("onCountdownToEvent"),
    ONENTERQUEUE("onEnterQueue");

    final String name;
    final int globalOrdinal;

    MatchState( String _name ) {
        name = _name;
        globalOrdinal = StateController.register( this.getClass() );
    }
    @Override
    public String toString() { return name; }
    @Override
    public int globalOrdinal() { return globalOrdinal; }

    public static MatchState fromString(String str) {
        str = str.toUpperCase();
        try {
            return MatchState.valueOf(str);
        } 
        catch (Exception e) {
            if ( str.equals( "ONCOUNTDOWNTOEVENT" ) )  return ONCOUNTDOWNTOEVENT;
            else if ( str.equals( "WINNER" ) ) return WINNERS;
            else if ( str.equals( "INSTART" ) ) return INGAME;
            return null;
        }
    }

    public MatchState getCorrectState( TransitionOption option ) {
        if ( option == null ) return this;
            
        switch ( this ) {          
            case ONJOIN:
            case ONOPEN:
                if (option.isState()) return INOPEN;
                break;
            case INOPEN:
                if (option.isTransition()) return INOPEN;
                break;
            case ONPRESTART:
                if (option.isState()) return INPRESTART;
                break;
            case INPRESTART:
                if (option.isTransition()) return ONPRESTART;
                break;
            case ONSTART:
                if (option.isState()) return INGAME;
                break;
            case INGAME:
                if (option.isTransition()) return ONSTART;
                break;
            case ONVICTORY:
                if (option.isState()) return INVICTORY;
                break;
            case INVICTORY:
                if (option.isTransition()) return ONVICTORY;
                break;
            
            default:
        }
        return this;
    }
}
