package mc.alk.arena.events.matches;

import org.bukkit.event.Cancellable;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.competition.Match;

public class MatchOpenEvent extends MatchEvent implements Cancellable {
	@Getter @Setter boolean cancelled = false;

	public MatchOpenEvent(Match match){
		super(match);
	}
}
