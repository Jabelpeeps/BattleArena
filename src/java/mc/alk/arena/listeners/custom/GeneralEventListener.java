package mc.alk.arena.listeners.custom;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;

import lombok.Getter;
import mc.alk.arena.Defaults;
import mc.alk.arena.objects.events.ArenaEventHandler.ArenaEventPriority;
import mc.alk.arena.util.Log;


abstract class GeneralEventListener extends BaseEventListener  {

    @Getter final public EnumMap<ArenaEventPriority, Map<RListener, Integer>> listeners = 
                                                                    new EnumMap<>(ArenaEventPriority.class);
    protected volatile RListener[] handlers = null;

    public GeneralEventListener(Class<? extends Event> _bukkitEvent, EventPriority _bukkitPriority) {
        super(_bukkitEvent, _bukkitPriority);
    }

    @Override
    public boolean hasListeners(){ return !listeners.isEmpty(); }
    public void addListener(RListener rl) { addMatchListener(rl); }

    public synchronized void removeListener(RListener rl) {
        if (Defaults.DEBUG_EVENTS) Log.info( "    removing listener listener=" + rl );
        removeMatchListener(rl);
    }

    @Override
    public synchronized void removeAllListeners(RListener rl) {
        if (Defaults.DEBUG_EVENTS) Log.info( "    removing all listeners  listener=" + rl );
        removeMatchListener(rl);
    }

    protected abstract void addMatchListener(RListener listener);
    protected abstract boolean removeMatchListener(RListener listener);

    protected synchronized void bake() {
        
        if ( handlers != null ) return;
        
        List<RListener> entries = new ArrayList<>();
        
        for ( Map.Entry<ArenaEventPriority, Map<RListener,Integer>> entry : listeners.entrySet() ) {
            entries.addAll( entry.getValue().keySet() ); 
        }
        handlers = entries.toArray( new RListener[entries.size()] );
    }

    public RListener[] getRegisteredListeners() {
        RListener[] handlersArray;
        while (( handlersArray = handlers ) == null ) bake(); // This prevents fringe cases of returning null
        return handlersArray;
    }
}
