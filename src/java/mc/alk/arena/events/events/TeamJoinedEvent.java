package mc.alk.arena.events.events;

import org.bukkit.event.Cancellable;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.competition.AbstractComp;
import mc.alk.arena.objects.teams.ArenaTeam;

@Getter @Setter 
public class TeamJoinedEvent extends EventEvent implements Cancellable {
	final ArenaTeam team;
	boolean cancelled = false;

	public TeamJoinedEvent(AbstractComp event,ArenaTeam _team) {
		super(event);
		team = _team;
	}
}
