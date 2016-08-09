package mc.alk.arena.events.matches;

import mc.alk.arena.competition.Match;
import mc.alk.arena.objects.MatchState;
import mc.alk.arena.objects.messaging.Channel;
import mc.alk.arena.objects.messaging.Channels;


public class MatchMessageEvent extends MatchEvent {
	final MatchState state;
	String serverMessage;
	String matchMessage;
	Channel serverChannel;

	public MatchMessageEvent( Match match, 
	                          MatchState _state, 
	                          Channel _serverChannel, 
	                          String _serverMessage, 
	                          String _matchMessage) {
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
    
	public String getServerMessage() { return serverMessage; }
	public void setServerMessage( String _serverMessage ) { serverMessage = _serverMessage; }
	public String getMatchMessage() { return matchMessage; }
	public void setMatchMessage( String _matchMessage ) { matchMessage = _matchMessage; }
	public void setServerChannel( Channel _serverChannel ) { serverChannel = _serverChannel; }
	public MatchState getState() { return state; }
}
