package mc.alk.arena.events.matches;

import org.bukkit.event.Cancellable;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.competition.Match;
import mc.alk.arena.objects.MatchResult;

public class MatchResultEvent extends MatchEvent implements Cancellable{
    @Getter @Setter MatchResult matchResult;
	@Getter @Setter boolean cancelled;
	final boolean matchEnding;

	public MatchResultEvent(Match match, MatchResult _matchResult) {
		super(match);
		matchResult = _matchResult;
		matchEnding = !match.alwaysOpen();
	}
	
	public boolean isMatchEnding(){
		return matchEnding && !cancelled;
	}
}
