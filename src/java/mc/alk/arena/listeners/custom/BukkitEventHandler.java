package mc.alk.arena.listeners.custom;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.bukkit.event.Event;

import mc.alk.arena.Defaults;
import mc.alk.arena.objects.arenas.ArenaListener;
import mc.alk.arena.objects.events.ArenaEventMethod;
import mc.alk.util.Log;


/**
 *
 * @author alkarin
 *
 */
class BukkitEventHandler {
    BukkitEventListener bukkitEventListener;
    ArenaEventListener arenaEventListener;
    SpecificPlayerEventListener playerEventListener;
    SpecificArenaPlayerEventListener arenaPlayerEventListener;

    /**
     * Construct a listener to listen for the given bukkit event
     * @param bukkitEvent : which event we will listen for
     * @param aem : a method which when not null and invoked will return a Player
     */
    public BukkitEventHandler( final Class<? extends Event> bukkitEvent, ArenaEventMethod aem ) {
        if ( aem.getPlayerMethod() != null ) {
            
            if ( aem.hasSpecificArenaPlayerMethod() ) 
                arenaPlayerEventListener = new SpecificArenaPlayerEventListener(
                                bukkitEvent, aem.getBukkitPriority(), aem.getPlayerMethod() );
            else 
                playerEventListener = new SpecificPlayerEventListener(
                                bukkitEvent, aem.getBukkitPriority(), aem.getPlayerMethod() );
        } 
        else {
            if ( aem.isBAEvent() ) 
                arenaEventListener = new ArenaEventListener(bukkitEvent,aem.getBukkitPriority());
            else
                bukkitEventListener = new BukkitEventListener(bukkitEvent,aem.getBukkitPriority(), null);
        }
        if ( Defaults.DEBUG_EVENTS ) 
            Log.info( "Registering BaseEventListener for type &6" + bukkitEvent.getSimpleName() + 
                    " &fpm=" + (aem.getPlayerMethod() == null ? "null" 
                                                              : aem.getPlayerMethod().getName() ) );
    }

    /**
     * Does this event even have any listeners
     * @return true if there are listeners
     */
    public boolean hasListeners() {
        return     ( arenaEventListener != null && arenaEventListener.hasListeners() ) 
                || ( bukkitEventListener != null && bukkitEventListener.hasListeners() ) 
                || ( arenaPlayerEventListener != null && arenaPlayerEventListener.hasListeners() ) 
                || ( playerEventListener != null && playerEventListener.hasListeners() );
    }

    /**
     * Add a player listener to this bukkit event
     * @param rl RListener
     * @param players the players
     */
    public void addListener(RListener rl, Collection<UUID> players) {
        
        if ( players != null && rl.hasSpecificPlayerMethod() ) {
            
            if (rl.hasSpecificArenaPlayerMethod())
                arenaPlayerEventListener.addListener(rl, players);
            else 
                playerEventListener.addListener(rl, players);
        } 
        else {
            if (rl.getMethod().isBAEvent())
                arenaEventListener.addListener(rl);
            else
                bukkitEventListener.addListener(rl);
        }
    }

    /**
     * remove a player listener from this bukkit event
     * @param rl RListener
     * @param players the players
     */
    public void removeListener(RListener rl, Collection<UUID> players) {
        
        if (players != null && rl.hasSpecificPlayerMethod() ){
            
            if (rl.hasSpecificArenaPlayerMethod())
                arenaPlayerEventListener.removeListener(rl, players);
            else
                playerEventListener.removeListener(rl, players);
        } 
        else {
            if (rl.getMethod().isBAEvent())
                arenaEventListener.removeListener(rl);
            else
                bukkitEventListener.removeListener(rl);
        }
    }

    /**
     * Remove all listeners for this bukkit event
     * @param rl RListener
     */
    public void removeAllListener(RListener rl) {
        if (playerEventListener != null) playerEventListener.removeAllListeners(rl);
        if (arenaPlayerEventListener != null) arenaPlayerEventListener.removeAllListeners(rl);
        if (arenaEventListener != null) arenaEventListener.removeAllListeners(rl);
        if (bukkitEventListener != null) bukkitEventListener.removeAllListeners(rl);
    }

    public ArenaEventListener getMatchListener(){
        return arenaEventListener;
    }

    public SpecificPlayerEventListener getSpecificPlayerListener(){
        return playerEventListener;
    }

    public SpecificArenaPlayerEventListener getSpecificArenaPlayerListener(){
        return arenaPlayerEventListener;
    }

    public void invokeArenaEvent(Set<ArenaListener> listeners, Event event) {
        if (arenaEventListener != null) arenaEventListener.invokeEvent(listeners, event);
    }

}
