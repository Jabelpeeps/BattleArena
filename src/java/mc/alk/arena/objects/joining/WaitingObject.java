package mc.alk.arena.objects.joining;

import java.util.Collection;

import lombok.Getter;
import mc.alk.arena.controllers.joining.AbstractJoinHandler;
import mc.alk.arena.controllers.joining.AbstractJoinHandler.TeamJoinResult;
import mc.alk.arena.controllers.joining.TeamJoinFactory;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.MatchState;
import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.objects.arenas.ArenaListener;
import mc.alk.arena.objects.exceptions.NeverWouldJoinException;
import mc.alk.arena.objects.options.JoinOptions;
import mc.alk.arena.objects.options.TransitionOption;

public class WaitingObject {
    protected boolean joinable = true;
    protected final AbstractJoinHandler joinHandler;
    @Getter protected final MatchParams params;
    @Getter protected final QueueObject originalQueuedObject;
    @Getter protected final Arena arena;

    public WaitingObject( QueueObject queue ) throws NeverWouldJoinException {
        params = queue.getMatchParams();
        originalQueuedObject = queue;
        arena = queue.getJoinOptions().getArena();
        
        if ( queue instanceof MatchTeamQObject ) {
            joinHandler = TeamJoinFactory.createTeamJoinHandler(queue.getMatchParams(), queue.getTeams());
            joinable = false;
        } 
        else {
            joinHandler = TeamJoinFactory.createTeamJoinHandler(queue.getMatchParams());
            joinable = true;
        }
    }

    public boolean matches(QueueObject qo) {
        return joinable && (arena != null ? arena.matches(qo.getJoinOptions()) 
                                          : params.matches(qo.getJoinOptions()));
    }
    
    public TeamJoinResult join( TeamJoinObject qo ) { return joinHandler.joiningTeam( qo ); }
    public boolean hasEnough() { return joinHandler.hasEnough( params.getAllowedTeamSizeDifference() ); }
    public boolean isFull() { return joinHandler.isFull(); }
    public Collection<ArenaPlayer> getPlayers() { return joinHandler.getPlayers(); }
    public JoinOptions getJoinOptions() { return originalQueuedObject.getJoinOptions(); }
    public Collection<ArenaListener> getArenaListeners() { return originalQueuedObject.getListeners(); }

    @Override
    public String toString() {
        return "[WO " + (arena != null ? arena.getName() : "") + " " + params.getDisplayName() + "]";
    }

    public boolean createsOnJoin() {
        Arena a = originalQueuedObject.getArena();
        if (a != null) {
            return a.getParams().hasOptionAt(MatchState.ONJOIN, TransitionOption.TELEPORTIN) ||
                    params.hasOptionAt(MatchState.ONJOIN, TransitionOption.TELEPORTIN);
        }
        return params.hasOptionAt(MatchState.ONJOIN, TransitionOption.TELEPORTIN);
    }
}
