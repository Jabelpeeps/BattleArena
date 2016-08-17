package mc.alk.arena.listeners.custom;

import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import lombok.Getter;
import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.TimingUtil;
import mc.alk.arena.util.TimingUtil.TimingStat;


/**
 *
 * @author alkarin
 *
 */
public abstract class BaseEventListener implements Listener  {
    
    @Getter final Class<? extends Event> event;
    final EventPriority bukkitPriority;
    static long total = 0;
    static long count = 0;
    AtomicBoolean listening = new AtomicBoolean();
    AtomicBoolean registered = new AtomicBoolean();
    Integer timerid = null;
    EventExecutor executor = null;
    static TimingUtil timings;

    public BaseEventListener( final Class<? extends Event> bukkitEvent, EventPriority _bukkitPriority ) {
        if (Defaults.DEBUG_EVENTS) Log.info("Registering BAEventListener for type &5" + bukkitEvent.getSimpleName());
        event = bukkitEvent;
        bukkitPriority = _bukkitPriority;
    }


    public void stopListening(){
        listening.set(false);

        if (BattleArena.getSelf().isEnabled()){
            final BaseEventListener bel = this;
            
            if ( timerid != null )
                Bukkit.getScheduler().cancelTask(timerid);
            
            timerid = Bukkit.getScheduler().scheduleSyncDelayedTask(BattleArena.getSelf(), 
                    () -> { if (registered.getAndSet(false)) {
                                HandlerList.unregisterAll( bel );
                            }
                            timerid = null;
                
                    },600L);
        }
    }

    public boolean isListening(){
        return listening.get();
    }

    public void startListening(){
        if (isListening() || Defaults.TESTSERVER) return;

        listening.set(true);
        if (timerid != null){
            Bukkit.getScheduler().cancelTask(timerid);
            timerid= null;
        }

        if (executor == null){
            if (Bukkit.getPluginManager().useTimings() || Defaults.DEBUG_TIMINGS){
                if (timings == null) 
                    timings = new TimingUtil();
                
                executor = ( listener, _event ) -> {
                        long startTime = System.nanoTime();
                        if (    !listening.get() 
                                || !event.isAssignableFrom(_event.getClass()))
                            return;
                        TimingStat t = timings.getOrCreate(_event.getClass().getSimpleName());
                        try {
                            invokeEvent(_event);
                            
                        } catch (Throwable e) { Log.printStackTrace(e); }
                        
                        t.count += 1;
                        t.totalTime += System.nanoTime() - startTime;           
                };
            } 
            else {
                executor = ( listener, _event) -> {
                        if (    !listening.get() 
                                || !event.isAssignableFrom( _event.getClass() ) )
                            return;
                        try {
                            invokeEvent(_event);
                            
                        } catch (Throwable e) { Log.printStackTrace(e); }
                };
            }

        }

        if (Defaults.TESTSERVER) return;

        if (!registered.getAndSet(true)){
            Bukkit.getPluginManager().registerEvent(event, this, bukkitPriority, executor,BattleArena.getSelf());
        }
    }

    public abstract void invokeEvent(Event _event);

    public abstract boolean hasListeners();

    public abstract void removeAllListeners(RListener rl);
}
