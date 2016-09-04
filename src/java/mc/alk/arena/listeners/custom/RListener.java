package mc.alk.arena.listeners.custom;

import java.util.Comparator;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mc.alk.arena.objects.arenas.ArenaListener;
import mc.alk.arena.objects.events.ArenaEventMethod;
import mc.alk.arena.objects.events.ArenaEventPriority;

@AllArgsConstructor
class RListener implements Comparator<RListener> {
    @Getter final private ArenaListener listener;
    @Getter final private ArenaEventMethod method;

	public boolean hasSpecificPlayerMethod() { return method.hasSpecificPlayerMethod(); }
	public boolean hasSpecificArenaPlayerMethod() { return method.hasSpecificArenaPlayerMethod(); }
	public ArenaEventPriority getPriority() { return method.getPriority(); }

	@Override
	public String toString(){
		return "[" + listener.getClass().getSimpleName() + " : " + method + "]";
	}

	@Override
    public int compare( RListener o1, RListener o2 ) {
		int c = o1.getMethod().getPriority().compareTo(o2.getMethod().getPriority());
		if (c != 0)
			return c;
		if (o1.getListener() == o2.getListener()){
			return o1.getMethod().getCallMethod().getName().compareTo(o2.getMethod().getCallMethod().getName());}
		return o1.getListener().getClass().toString().compareTo(o2.getListener().getClass().toString());
	}
}
