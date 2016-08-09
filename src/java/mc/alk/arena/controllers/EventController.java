package mc.alk.arena.controllers;

import java.util.HashMap;

import mc.alk.arena.competition.AbstractComp;
import mc.alk.arena.executors.EventExecutor;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.teams.ArenaTeam;


public class EventController {
	static HashMap<String, AbstractComp> registeredEvents = new HashMap<>();
	static HashMap<String, EventExecutor> registeredExecutors = new HashMap<>();

	public static AbstractComp insideEvent(ArenaPlayer p) {
		for (AbstractComp evt : registeredEvents.values()){
			ArenaTeam t = evt.getTeam(p);
			if (t != null)
				return evt;
		}
		return null;
	}

	public static void addEvent(AbstractComp event){
		registeredEvents.put(event.getName().toLowerCase(),event);
		registeredEvents.put(event.getCommand().toLowerCase(),event);
	}

	public void cancelAll() {
		for (AbstractComp evt : registeredEvents.values()){
			if (evt.isClosed())
				continue;
			evt.cancelEvent();
		}
	}

	public static void addEventExecutor(String name, String command, EventExecutor executor) {
		registeredExecutors.put(name.toLowerCase(), executor);
		registeredExecutors.put(command.toLowerCase(),executor);
	}

	public static EventExecutor getEventExecutor(AbstractComp event){
		return registeredExecutors.get(event.getName().toLowerCase());
	}

	public static EventExecutor getEventExecutor(String eventType){
		return registeredExecutors.get(eventType.toLowerCase());
	}

	public static boolean isEventType(String name) {
		return ParamController.getEventParamCopy(name) != null;
	}
}
