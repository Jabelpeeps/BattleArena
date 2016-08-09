package mc.alk.arena.events.matches;

import org.bukkit.event.Cancellable;

import mc.alk.arena.competition.Match;

public class MatchOpenEvent extends MatchEvent implements Cancellable {
	/// Cancel status
	boolean cancelled = false;

	public MatchOpenEvent(Match match){
		super(match);
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

}
