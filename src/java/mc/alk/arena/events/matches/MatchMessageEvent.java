package mc.alk.arena.events.matches;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.competition.Match;
import mc.alk.arena.objects.MatchState;
import mc.alk.arena.objects.messaging.Channel;
import mc.alk.arena.objects.messaging.Channels;


public class MatchMessageEvent extends MatchEvent {
	@Getter final MatchState state;
	@Getter @Setter String serverMessage;
	@Getter @Setter String matchMessage;
	@Setter Channel serverChannel;

	public MatchMessageEvent( Match match, MatchState _state, Channel _serverChannel, 
	                          String _serverMessage, String _matchMessage ) {
		super( match );
		serverChannel = _serverChannel;
		serverMessage = _serverMessage;
		matchMessage = _matchMessage;
		state = _state;
	}
    public Channel getServerChannel() {
        return serverChannel == null ? Channels.NullChannel 
                                     : serverChannel;
    }
}
