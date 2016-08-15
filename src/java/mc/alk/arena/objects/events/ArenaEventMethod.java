package mc.alk.arena.objects.events;

import java.lang.reflect.Method;

import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;

import lombok.Getter;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchState;

public class ArenaEventMethod {
	@Getter final Method callMethod;
	@Getter final Class<? extends Event> event;
	@Getter final Method PlayerMethod;
	@Getter final MatchState beginState, endState;
	@Getter final ArenaEventPriority priority;
	@Getter final EventPriority bukkitPriority;
	final boolean specificArenaPlayer;
    @Getter final boolean bAEvent; /// Whether this is a BAevent or a normal bukkit event

	public ArenaEventMethod( Method _callMethod, Class<? extends Event> _event, 
	                         MatchState begin, MatchState end, MatchState cancel, ArenaEventPriority _priority,
	                         EventPriority _bukkitPriority, boolean _isBAEvent) {
		this( _callMethod, _event, null, begin, end, cancel, _priority, _bukkitPriority, _isBAEvent );
	}

	public ArenaEventMethod( Method _callMethod, Class<? extends Event> _event, Method _getPlayerMethod,
			                 MatchState begin, MatchState end, MatchState cancel, ArenaEventPriority _priority,
			                 EventPriority _bukkitPriority, boolean _isBAEvent ) {
		callMethod = _callMethod;
		event = _event;
		PlayerMethod = _getPlayerMethod;
		beginState = begin;
		endState = end;
		priority = _priority;
		bukkitPriority = _bukkitPriority;
		specificArenaPlayer = _getPlayerMethod != null &&
				ArenaPlayer.class.isAssignableFrom( _getPlayerMethod.getReturnType() );
        bAEvent = _isBAEvent;
    }

	public boolean hasSpecificPlayerMethod() { return PlayerMethod != null; }
	public boolean hasSpecificArenaPlayerMethod() { return specificArenaPlayer; }

    @Override
	public String toString(){
		return "[MEM " + callMethod.getName() + ", " + ( event != null ? event.getSimpleName() 
		                                                               : "null" ) +
				" p=" + bukkitPriority + " "  + beginState + ":" + endState + "   playerMethod=" + PlayerMethod + "]";
	}

}
