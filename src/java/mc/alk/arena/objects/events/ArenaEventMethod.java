package mc.alk.arena.objects.events;

import java.lang.reflect.Method;

import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;

import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchState;

public class ArenaEventMethod {
	final Method callMethod;
	final Class<? extends Event> bukkitEvent;
	final Method getPlayerMethod;
	final MatchState beginState, endState;
	final ArenaEventPriority priority;
	final EventPriority bukkitPriority;
	final boolean specificArenaPlayer;
    final boolean isBAEvent; /// Whether this is a BAevent or a normal bukkit event

	public ArenaEventMethod( Method _callMethod, Class<? extends Event> event, MatchState begin, 
	                         MatchState end, MatchState cancel, ArenaEventPriority _priority,
	                         EventPriority _bukkitPriority, boolean _isBAEvent) {
		this( _callMethod, event, null, begin, end, cancel, _priority, _bukkitPriority, _isBAEvent );
	}

	public ArenaEventMethod( Method _callMethod, Class<? extends Event> event, Method _getPlayerMethod,
			                 MatchState begin, MatchState end, MatchState cancel, ArenaEventPriority _priority,
			                 EventPriority _bukkitPriority, boolean _isBAEvent ) {
		callMethod = _callMethod;
		bukkitEvent = event;
		getPlayerMethod = _getPlayerMethod;
		beginState = begin;
		endState = end;
		priority = _priority;
		bukkitPriority = _bukkitPriority;
		specificArenaPlayer =	_getPlayerMethod != null &&
				ArenaPlayer.class.isAssignableFrom(getPlayerMethod().getReturnType());
        isBAEvent = _isBAEvent;
    }

	public boolean isSpecificPlayerMethod() { return getPlayerMethod != null; }
	public boolean isSpecificArenaPlayerMethod() { return specificArenaPlayer; }
	public ArenaEventPriority getPriority() { return priority; }
	public Method getMethod() { return callMethod; }
	public Method getPlayerMethod() { return getPlayerMethod; }
	public Class<? extends Event> getBAEvent() { return bukkitEvent; }
	public MatchState getBeginState() { return beginState; }
	public MatchState getEndState() { return endState; }
    public boolean isBAEvent() { return isBAEvent; }

    @Override
	public String toString(){
		return "[MEM "+callMethod.getName()+", " + (bukkitEvent != null ? bukkitEvent.getSimpleName():"null")+
				" p="+bukkitPriority+" "  + beginState+":"+endState+"   playerMethod=" + getPlayerMethod+"]";
	}

	public EventPriority getBukkitPriority() { return bukkitPriority; }
}
