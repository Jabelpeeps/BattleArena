package mc.alk.arena.events.events;

import java.util.Collection;

import lombok.Getter;
import mc.alk.arena.competition.AbstractComp;
import mc.alk.arena.objects.teams.ArenaTeam;

public class EventStartEvent extends EventEvent {
	@Getter final Collection<ArenaTeam> teams;
	public EventStartEvent(AbstractComp event, Collection<ArenaTeam> _teams) {
		super(event);
		teams = _teams;
	}
}
