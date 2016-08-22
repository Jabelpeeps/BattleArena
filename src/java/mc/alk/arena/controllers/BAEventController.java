package mc.alk.arena.controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import mc.alk.arena.competition.AbstractComp;
import mc.alk.arena.events.events.EventFinishedEvent;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.EventParams;
import mc.alk.arena.objects.EventState;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.exceptions.InvalidEventException;


public class BAEventController implements Listener {
	private Map<String, Map<EventState,List<AbstractComp>>> allEvents = Collections.synchronizedMap(new HashMap<>());

	public static class SizeEventPair{
		public Integer nEvents = 0;
		public AbstractComp event = null;
	}

	public SizeEventPair getUniqueEvent(MatchParams eventParams) {
		final String key = getKey(eventParams);
		Map<EventState,List<AbstractComp>> events = allEvents.get(key);
		SizeEventPair result = new SizeEventPair();
		if (events == null || events.isEmpty())
			return result;
		result.nEvents = 0;
		AbstractComp event = null;
		for (List<AbstractComp> list: events.values()){
			result.nEvents += list.size();
			for (AbstractComp evt: list){
				if (evt != null){
					if (event != null){
						result.event = null;
						return result;
					}
					event = evt;
					result.event = evt;
				}
			}
		}
		return result;
	}

	public AbstractComp getEvent(ArenaPlayer p) {
	    
		/// maybe ArenaPlayers can have a sense of which event has them...
		for (Map<EventState,List<AbstractComp>> map : allEvents.values()){
			for (List<AbstractComp> list: map.values()){
				for (AbstractComp event: list){
					if (event.hasPlayer(p)){
						return event;}
				}
			}
		}
		return null;
	}

	public boolean hasOpenEvent() {
		for (Map<EventState, List<AbstractComp>> map : allEvents.values()){
			for (EventState es: map.keySet()){
				switch (es){
				case CLOSED:
				case FINISHED:
					continue;
				case OPEN:
				case RUNNING:
				default:
					if (!map.get(es).isEmpty())
						return true;
					break;
				}
			}
		}
		return false;
	}

	public boolean hasOpenEvent(EventParams eventParam) {
		final String key = getKey(eventParam);
		Map<EventState,List<AbstractComp>> events = allEvents.get(key);
        return events != null && events.get(EventState.OPEN) != null;
    }

	private String getKey(final AbstractComp event){
		return getKey(event.getParams());
	}

	private String getKey(final MatchParams eventParams){
		return eventParams.getCommand().toUpperCase();
	}

	public void addOpenEvent(AbstractComp event) throws InvalidEventException {
		final String key = getKey(event);
		Map<EventState, List<AbstractComp>> map = allEvents.get(key);
		if (map == null){
			map = Collections.synchronizedMap(new EnumMap<EventState,List<AbstractComp>>(EventState.class));
			allEvents.put(key, map);
		}
		List<AbstractComp> events = map.get(EventState.OPEN);
		if (events == null){
			events = Collections.synchronizedList(new ArrayList<AbstractComp>());
			map.put(EventState.OPEN, events);
		}
		if (!events.isEmpty()){
			throw new InvalidEventException("There is already an open event of this type!");}
		events.add(event);
	}

	public AbstractComp getOpenEvent(EventParams eventParams) {
		final String key = getKey(eventParams);
		Map<EventState,List<AbstractComp>> events = allEvents.get(key);
		if (events == null)
			return null;
		List<AbstractComp> es = events.get(EventState.OPEN);
		return (es != null && !es.isEmpty()) ? es.get(0) : null;
	}

	public void startEvent(AbstractComp event) throws Exception {
		if (event.getState() != EventState.OPEN)
			throw new Exception("Event was not open!");
		final String key = getKey(event);
		AbstractComp evt = getOpenEvent(event.getParams());
		if (evt != event){
			throw new Exception("Trying to start the wrong open event!");}
		Map<EventState, List<AbstractComp>> map = allEvents.get(key);
		if (map == null){
			map = Collections.synchronizedMap(new EnumMap<EventState,List<AbstractComp>>(EventState.class));
			allEvents.put(key, map);
		}
		/// Remove the open event
		List<AbstractComp> events = map.get(EventState.OPEN);
		events.remove(event);
		/// Add to running events and start
		events = map.get(EventState.RUNNING);
		if (events == null){
			events = Collections.synchronizedList(new ArrayList<AbstractComp>());
			map.put(EventState.RUNNING, events);
		}
		events.add(event);
		event.startEvent();
	}

	public Map<EventState,List<AbstractComp>> getCurrentEvents(EventParams eventParams) {
		final String key = getKey(eventParams);
		Map<EventState,List<AbstractComp>> events = allEvents.get(key);
		return events != null ? new EnumMap<>(events) : null;
	}

	public boolean removeEvent(AbstractComp event){
		for (Map<EventState,List<AbstractComp>> map : allEvents.values()){
			for (List<AbstractComp> list: map.values()){
				Iterator<AbstractComp> iter = list.iterator();
				while(iter.hasNext()){
					AbstractComp evt = iter.next();
					if (evt.equals(event)){
						iter.remove();}
				}
			}
		}
		return false;
	}

	public boolean cancelEvent(AbstractComp event) {
		event.cancelEvent();
		return removeEvent(event);
	}

	@EventHandler
	public void onEventFinished(EventFinishedEvent event){
		removeEvent(event.getEvent());
	}
}
