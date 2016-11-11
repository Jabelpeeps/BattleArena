package mc.alk.arena.objects.messaging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import lombok.Setter;
import mc.alk.arena.competition.AbstractComp;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.MatchState;
import mc.alk.arena.objects.messaging.MessageOptions.MessageOption;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.serializers.MessageSerializer;
import mc.alk.arena.util.MessageUtil;


public class EventMessenger {
	@Setter boolean silent = false;
    final AbstractComp event;
    protected AnnouncementOptions bos;
    private MatchParams params;
    private MessageSerializer messages;

	public EventMessenger( AbstractComp e ) {
        params = e.getParams();
        event = e;
		bos = params.getAnnouncementOptions();
		messages = MessageSerializer.getMessageSerializer( params.getName() );
	}

	protected Channel getChannel( MatchState state ) {
		if ( silent ) return Channels.NullChannel;
		return bos != null && bos.hasOption( false, state ) ? bos.getChannel(false,state) 
		                                                    : AnnouncementOptions.getDefaultChannel( false, state );
	}

    public void sendTeamJoinedEvent( ArenaTeam team ) {
        Channel serverChannel = getChannel( MatchState.ONJOIN );
        Message message = messages.getNodeMessage("common.onjoin");
        Message serverMessage = messages.getNodeMessage("common.onjoin_server");
        
        Set<MessageOption> ops = message.getOptions();
        if ( serverChannel != Channels.NullChannel ) {
            ops.addAll( serverMessage.getOptions() );
        }

        List<ArenaTeam> teams = new ArrayList<>();
        teams.add(team);
        MessageFormatter msgf = new MessageFormatter( messages, params, teams.size(), message, ops );
        msgf.formatCommonOptions( teams );
        
        for ( ArenaTeam t : teams ) {
            msgf.formatTeamOptions( t, false );
            msgf.formatTeams( teams );
            String newmsg = msgf.getFormattedMessage( message );
            t.sendMessage(newmsg);
        }

        if ( serverChannel != Channels.NullChannel ) {
            String msg = msgf.getFormattedMessage( serverMessage );
            serverChannel.broadcast( msg );
        }
    }
    
    public void sendCountdownTillEvent(int seconds) {
        
        Channel serverChannel = getChannel(MatchState.ONCOUNTDOWNTOEVENT);
        Message message = messages.getNodeMessage("event.countdownTillEvent");
        Message serverMessage = messages.getNodeMessage("event.server_countdownTillEvent");
        Set<MessageOption> ops = message.getOptions();
        
        if ( serverChannel != Channels.NullChannel ) {
            ops.addAll( serverMessage.getOptions() );
        }
        MessageFormatter msgf = new MessageFormatter( messages, params, 0, message, ops );
        msgf.formatCommonOptions( null, seconds );

        if ( serverChannel != Channels.NullChannel ) {
            String msg = msgf.getFormattedMessage( serverMessage );
            serverChannel.broadcast( msg );
        }
    }

    public void sendEventStarting( Collection<ArenaTeam> teams ) {
        String nTeamPath = messages.getStringPathFromSize( teams.size() );
        
        formatAndSend( getChannel( MatchState.ONSTART ), 
                       teams, 
                       messages.getNodeMessage( "event." + nTeamPath + ".start" ), 
                       messages.getNodeMessage( "event." + nTeamPath + ".server_start" ) );
    }

    public void sendEventVictory( Collection<ArenaTeam> victors, Collection<ArenaTeam> losers ) {
        String nTeamPath = messages.getStringPathFromSize( losers.size() + 1 );
        messages.sendVictory( getChannel( MatchState.ONVICTORY ), victors, losers,
                              "event." + nTeamPath + ".victory", 
                              "event." + nTeamPath + ".loss",
                              "event." + nTeamPath + ".server_victory",
                              params );
    }

    public void sendEventOpenMsg() {
        Channel serverChannel = getChannel( MatchState.ONOPEN );
        
        if ( serverChannel == Channels.NullChannel ) return;
        
        String nTeamPath = messages.getStringPathFromSize( params.getMinTeams() );
        Message serverMessage;
        
        if ( params.getMinTeamSize() > 1 )
            serverMessage = messages.getNodeMessage( "event." + nTeamPath + ".server_open_teamSizeGreaterThanOne" );
        else
            serverMessage = messages.getNodeMessage( "event." + nTeamPath + ".server_open" );
        
        Set<MessageOption> ops = serverMessage.getOptions();
        MessageFormatter msgf = new MessageFormatter( messages, params, 0, serverMessage, ops );
        msgf.formatCommonOptions( null );
        String msg = msgf.getFormattedMessage( serverMessage );
        serverChannel.broadcast( msg );
    }

    public void sendEventCancelledDueToLackOfPlayers( Set<ArenaPlayer> competingPlayers ) {
        MessageUtil.sendMessage( competingPlayers, 
                            params.getPrefix() + "&e The Event has been cancelled b/c there weren't enough players" );
    }

    public void sendEventCancelled( Collection<ArenaTeam> teams ) {
        formatAndSend( getChannel( MatchState.ONCANCEL ), 
                       teams, 
                       messages.getNodeMessage( "event.team_cancelled" ), 
                       messages.getNodeMessage( "event.server_cancelled" ) );
    }

    private void formatAndSend(Channel serverChannel, Collection<ArenaTeam> teams, Message message, Message serverMessage) {
        Set<MessageOption> ops = message.getOptions();
        if ( serverChannel != Channels.NullChannel ) {
            ops.addAll( serverMessage.getOptions() );
        }

        MessageFormatter msgf = new MessageFormatter( messages, params, teams.size(), message, ops );
        msgf.formatCommonOptions( teams );
        for ( ArenaTeam t : teams ) {
            msgf.formatTeamOptions( t, false );
            msgf.formatTeams( teams );
            String newmsg = msgf.getFormattedMessage( message );
            t.sendMessage( newmsg );
        }

        if ( serverChannel != Channels.NullChannel ) {
            String msg = msgf.getFormattedMessage( serverMessage );
            serverChannel.broadcast( msg );
        }
    }

    public void sendCantFitTeam( ArenaTeam t ) {
        t.sendMessage( "&cThe &6" + event.getDisplayName() + "&c is full" );
    }

    public void sendWaitingForMorePlayers( ArenaTeam team, int remaining ) {
        team.sendMessage( "&eYou have joined the &6" + event.getDisplayName() );
        team.sendMessage( "&eYou will enter the Event when &6" + remaining + "&e more " + 
                            MessageUtil.playerOrPlayers( remaining ) + "&e have joined to make your team" );
    }

    public void sendEventDraw( Collection<ArenaTeam> participants, Collection<ArenaTeam> losers ) {
        String nTeamPath = messages.getStringPathFromSize( participants.size() );
        messages.sendVictory( getChannel( MatchState.ONVICTORY ), null, participants,
                             "event." + nTeamPath + ".draw", 
                             "event." + nTeamPath + ".draw",
                             "event." + nTeamPath + ".server_draw",
                             params );
    }
}
