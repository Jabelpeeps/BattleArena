package mc.alk.arena.controllers;

import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.competition.AbstractComp;
import mc.alk.arena.competition.Match;
import mc.alk.arena.events.events.EventFinishedEvent;
import mc.alk.arena.events.matches.MatchFinishedEvent;
import mc.alk.arena.executors.EventExecutor;
import mc.alk.arena.executors.TournamentExecutor;
import mc.alk.arena.objects.EventParams;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.objects.arenas.ArenaListener;
import mc.alk.arena.objects.events.ArenaEventHandler;
import mc.alk.arena.objects.exceptions.InvalidEventException;
import mc.alk.arena.objects.exceptions.InvalidOptionException;
import mc.alk.arena.objects.options.EventOpenOptions;
import mc.alk.arena.objects.pairs.EventPair;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.MessageUtil;
import mc.alk.arena.util.TimeUtil;

public class EventScheduler implements Runnable, ArenaListener{

	int curEvent = 0;
	boolean continuous= false;
	@Getter boolean running = false;
	boolean stop = false;
	Integer currentTimer = null;

	@Getter final CopyOnWriteArrayList<EventPair> events = new CopyOnWriteArrayList<>();

	@Override
	public void run() {
		if ( events.isEmpty() || stop ) return;
		
		running = true;
		int index = curEvent % events.size();
		curEvent++;
		currentTimer = Scheduler.scheduleSynchronousTask( new RunEvent( this, events.get( index ) ) );
	}

	@AllArgsConstructor
	public class RunEvent implements Runnable {
        final EventScheduler scheduler;
		final EventPair eventPair;
		
		@Override
		public void run() {
			if ( stop ) return;

			CommandSender sender = Bukkit.getConsoleSender();
			MatchParams params = eventPair.getEventParams();
			String args[] = eventPair.getArgs();
			boolean success = false;
			try {
				EventExecutor ee = EventController.getEventExecutor(eventPair.getEventParams().getName());
				if (ee != null && ee instanceof TournamentExecutor){
					TournamentExecutor exe = (TournamentExecutor) ee;
					AbstractComp event = exe.openIt(sender, (EventParams)params, args);
					if ( event != null ) {
						event.addArenaListener(scheduler);
						success = true;
					}
					if (Defaults.DEBUG_SCHEDULER) Log.info("[BattleArena debugging] Running event ee=" + ee  +
                            "  event" + event +"  args=" + Arrays.toString(args));
				} 
				else { /// normal match
					EventOpenOptions eoo = EventOpenOptions.parseOptions(args, null, params);
					Arena arena = eoo.getArena(params);
                    if ( arena != null ) {
                        Match m = BattleArena.getBAController().createAndAutoMatch(arena, eoo);
                        m.addArenaListener(scheduler);
                        success = true;
                    } 
                    else 
                        Log.warn("[BattleArena] scheduled command args=" + Arrays.toString(args) +
                                " can't be started. Arena is not there or in use");
				}
            } 
			catch (InvalidOptionException | InvalidEventException e) {
                Log.warn(e.getMessage());
			}

			if (!success && BattleArena.getSelf().isEnabled()){ /// wait then start up the scheduler again in x seconds
                Log.info("[BattleArena scheduler starting next command in " +
                        Defaults.TIME_BETWEEN_SCHEDULED_EVENTS + " seconds");
				currentTimer = Scheduler.scheduleAsynchronousTask(scheduler, 20L * Defaults.TIME_BETWEEN_SCHEDULED_EVENTS);
			}
		}
	}

	@ArenaEventHandler
	public void onEventFinished(EventFinishedEvent event){
		AbstractComp e = event.getEvent();
		e.removeArenaListener(this);
		if ( continuous ) {
			if (Defaults.DEBUG_SCHEDULER) Log.info( "[BattleArena debugging] finished event "+ e +
                    "  scheduling next event in "+ 20L * Defaults.TIME_BETWEEN_SCHEDULED_EVENTS + " ticks");

            if ( BattleArena.getSelf().isEnabled() )
			    Scheduler.scheduleAsynchronousTask( this, 20L * Defaults.TIME_BETWEEN_SCHEDULED_EVENTS );
			if ( Defaults.SCHEDULER_ANNOUNCE_TIMETILLNEXT )
                Bukkit.broadcastMessage(
						MessageUtil.colorChat(
								ChatColor.YELLOW + "Next event will start in " +
										TimeUtil.convertSecondsToString( Defaults.TIME_BETWEEN_SCHEDULED_EVENTS ) ) );
		} 
		else running = false;
	}

	@ArenaEventHandler
	public void onMatchFinished(MatchFinishedEvent event){
		if ( continuous ) {
			if ( Defaults.DEBUG_SCHEDULER ) 
			    Log.info( "[BattleArena debugging] finished event " + event.getEventName() + 
			            "  scheduling next event in " + 20L * Defaults.TIME_BETWEEN_SCHEDULED_EVENTS + " ticks" );

            if ( BattleArena.getSelf().isEnabled() )
                Scheduler.scheduleAsynchronousTask( this, 20L * Defaults.TIME_BETWEEN_SCHEDULED_EVENTS );
			if ( Defaults.SCHEDULER_ANNOUNCE_TIMETILLNEXT )
                Bukkit.broadcastMessage(
						MessageUtil.colorChat(
								ChatColor.YELLOW + "Next event will start in " +
										TimeUtil.convertSecondsToString( Defaults.TIME_BETWEEN_SCHEDULED_EVENTS ) ) );
		} else running = false;
	}

	public void stop() {
		stop = true;
		running = false;
		continuous = false;
	}

	public void start() {
		continuous = true;
		stop = false;
		new Thread( this ).start();
	}

	public void startNext() {
		continuous = false;
		if ( currentTimer != null )
			Bukkit.getScheduler().cancelTask( currentTimer );
		stop = false;
		new Thread( this ).start();
	}

	public boolean scheduleEvent( MatchParams eventParams, String[] args ) {
		return events.add( new EventPair( eventParams, args ) );
	}
	public EventPair deleteEvent(int i) { return events.remove(i); }
}
