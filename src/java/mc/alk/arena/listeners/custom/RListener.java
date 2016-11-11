package mc.alk.arena.listeners.custom;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mc.alk.arena.objects.arenas.ArenaListener;
import mc.alk.arena.objects.events.ArenaEventHandler.ArenaEventPriority;
import mc.alk.arena.objects.events.ArenaEventMethod;

@AllArgsConstructor
class RListener implements Comparable<RListener> {
    @Getter final private ArenaListener listener;
    @Getter final private ArenaEventMethod method;

	public boolean hasSpecificPlayerMethod() { return method.hasSpecificPlayerMethod(); }
	public boolean hasSpecificArenaPlayerMethod() { return method.hasSpecificArenaPlayerMethod(); }
	public ArenaEventPriority getPriority() { return method.getPriority(); }

	@Override
	public String toString() {
		return "[" + listener.getClass().getSimpleName() + " : " + method + "]";
	}

	@Override
    public int compareTo( RListener o2 ) {
		int c = method.getPriority().compareTo( o2.method.getPriority() );
		if ( c != 0 )
			return c;
		
		if ( listener == o2.listener )
			return method.getCallMethod().getName().compareTo( o2.method.getCallMethod().getName() );
		
		return listener.getClass().toString().compareTo( o2.listener.getClass().toString() );
	}
}
