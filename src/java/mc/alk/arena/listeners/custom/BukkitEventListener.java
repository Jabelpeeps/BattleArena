package mc.alk.arena.listeners.custom;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;

import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;

import mc.alk.arena.Defaults;
import mc.alk.arena.util.Log;


/**
 *
 * Bake and handling based on Bukkit and lahwran's fevents.
 * @author alkarin
 *
 */
class BukkitEventListener extends GeneralEventListener {


	/**
	 * Construct a listener to listen for the given bukkit event
	 * @param _bukkitEvent : which event we will listen for
	 * @param getPlayerMethod : a method which when not null and invoked will return a Player
	 */
	public BukkitEventListener(final Class<? extends Event> _bukkitEvent,
                                            EventPriority _bukkitPriority, Method getPlayerMethod) {
		super( _bukkitEvent, _bukkitPriority );
		if ( Defaults.DEBUG_EVENTS ) Log.info( "Registering BukkitEventListener for type " +
                _bukkitEvent.getSimpleName() +" pm="+(getPlayerMethod == null ? "null" : getPlayerMethod.getName()));
	}

    @Override
    protected synchronized void addMatchListener(RListener rl) {
        if (!isListening())
            startListening();

        Map<RListener,Integer> l = listeners.get(rl.getPriority());
        if (l == null){
            l = new TreeMap<>( ( o1, o2 ) -> {
                    return o1.getListener().equals( o2.getListener() ) ? 0 
                                                                       : new Integer(o1.hashCode()).compareTo(o2.hashCode());               
                                             } );
            listeners.put(rl.getPriority(), l);
        }
        Integer lcount = l.get(rl);
        
        if ( lcount == null ){
            l.put( rl, 1 );
            handlers = null;
            bake();
        } 
        else {
            l.put( rl,lcount + 1 );
        }
    }

    /**
     * remove an arena listener to this bukkit event
     * @param listener RListener
     * @return whether listener was found and removed
     */
    @Override
    protected boolean removeMatchListener( RListener listener ) {
        final Map<RListener,Integer> map = listeners.get(listener.getPriority());
        if ( map == null )
            return false;
        Integer rCount = map.get( listener );
        boolean removed;
        if ( rCount == null || rCount == 1 ) {
            map.remove( listener );
            handlers = null;
            removed = true;
        } else {
            map.put( listener, rCount - 1 );
            removed = false;
        }
        if ( removed && !hasListeners() && isListening() )
            stopListening();
        return removed;
    }

	@Override
	public void invokeEvent( Event _event ) {

		for ( RListener rl : getRegisteredListeners() ) {
			try {
                rl.getMethod().getCallMethod().invoke( rl.getListener(), _event ); 
			} 
			catch (Exception e){
				Log.printStackTrace(e);
			}
		}
	}
}
