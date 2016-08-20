package mc.alk.arena.events.tracker;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import lombok.AllArgsConstructor;
import mc.alk.arena.controllers.Scheduler;
import mc.alk.arena.controllers.tracker.TrackerInterface;

@AllArgsConstructor
public class TrackerEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	final TrackerInterface trackerInterface;

	/**
	 * Alias for getInterfaceName
	 * @return
	 */
	public String getDBName(){
		return trackerInterface.getInterfaceName();
	}
	/**
	 * Returns the name of the interface this event was called from
	 * @return
	 */
	public String getInterfaceName(){
		return trackerInterface.getInterfaceName();
	}
	public void callEvent(){
		Bukkit.getPluginManager().callEvent(this);
	}
	public void callSyncEvent() {
		Scheduler.scheduleSynchronousTask( () -> Bukkit.getPluginManager().callEvent( this ) );
	}

	@Override
	public HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
