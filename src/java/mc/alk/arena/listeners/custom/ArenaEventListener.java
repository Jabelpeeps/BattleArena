package mc.alk.arena.listeners.custom;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;

import mc.alk.arena.Defaults;
import mc.alk.arena.objects.arenas.ArenaListener;
import mc.alk.arena.util.Log;


/**
 *
 * Bake and handling based on Bukkit and lahwran's fevents.
 * @author alkarin
 *
 */
class ArenaEventListener extends GeneralEventListener {

    /**
     * Construct a listener to listen for the given bukkit event
     * @param bukkitEvent : which event we will listen for
     */
    public ArenaEventListener( final Class<? extends Event> bukkitEvent, EventPriority _bukkitPriority ) {
        super( bukkitEvent, _bukkitPriority );
        if ( Defaults.DEBUG_EVENTS) Log.info( "Registering ArenaEventListener for type " +
                                                    bukkitEvent.getSimpleName() );
    }

    /**
     * add an arena listener to this bukkit event
     * @param rl RListener
     */
    @Override
    protected synchronized void addMatchListener(RListener rl) {
        Map<RListener,Integer> l = listeners.get( rl.getPriority() );
        if ( l == null ) {
            l = new TreeMap<>();
            listeners.put( rl.getPriority(), l );
        }

        Integer  rCount = l.get(rl);
        if ( rCount == null ) {
            l.put( rl, 1 );
            handlers = null;
            bake();
        } else {
            l.put(rl, rCount+1);
        }
    }

    /**
     * remove an arena listener to this bukkit event
     * @param listener RListener
     * @return whether listener was found and removed
     */
    @Override
    protected boolean removeMatchListener(RListener listener) {
        final Map<RListener,Integer> map = listeners.get(listener.getPriority());
        if ( map == null )
            return false;
        Integer rCount = map.get(listener);
        boolean removed;
        if (rCount == null || rCount == 1){
            map.remove(listener);
            handlers = null;
            removed = true;
        } else {
            map.put(listener, rCount - 1);
            removed = false;
        }
        return removed;
    }

    public void invokeEvent( Set<ArenaListener> listeners, Event event) {
        for ( RListener rl: getRegisteredListeners() ) {
            if ( !listeners.contains( rl.getListener() ) )
                continue;
            try {
                rl.getMethod().getCallMethod().invoke(rl.getListener(), event); 
            } catch (Exception e){
                Log.printStackTrace(e);
            }
        }
    }

    @Override
    public void invokeEvent( Event event ) {
        for ( RListener rl: getRegisteredListeners() ) {
            try {
                rl.getMethod().getCallMethod().invoke(rl.getListener(), event);
            } catch (Exception e){
                Log.printStackTrace(e);
            }
        }
    }
}
