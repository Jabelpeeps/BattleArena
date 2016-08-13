package mc.alk.arena.events.matches;

import lombok.Getter;
import mc.alk.arena.competition.Match;
import mc.alk.arena.objects.MatchState;
import mc.alk.arena.objects.messaging.Channel;

public class MatchIntervalMessageEvent extends MatchMessageEvent{
	@Getter final int timeRemaining;

	public MatchIntervalMessageEvent(  Match match, MatchState _state, Channel _serverChannel,
	                                    String _serverMessage, String _matchMessage, int remainingTime ) {
		super(match, _state, _serverChannel, _serverMessage, _matchMessage);
		timeRemaining = remainingTime;
	}
}
