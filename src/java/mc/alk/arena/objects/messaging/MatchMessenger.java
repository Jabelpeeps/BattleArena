package mc.alk.arena.objects.messaging;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import lombok.Setter;
import mc.alk.arena.Defaults;
import mc.alk.arena.competition.Match;
import mc.alk.arena.events.matches.MatchIntervalMessageEvent;
import mc.alk.arena.events.matches.MatchMessageEvent;
import mc.alk.arena.events.matches.MatchTimeExpiredMessageEvent;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.MatchState;
import mc.alk.arena.objects.messaging.MessageOptions.MessageOption;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.objects.victoryconditions.VictoryCondition;
import mc.alk.arena.objects.victoryconditions.interfaces.DefinesLeaderRanking;
import mc.alk.arena.serializers.MessageSerializer;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.MessageUtil;
import mc.alk.arena.util.TeamUtil;


public class MatchMessenger {
    final Match match;
    final String typedot = "match.";
	@Setter boolean silent = false;
    protected AnnouncementOptions bos;
    private MatchParams params;
    private MessageSerializer messages;

	public MatchMessenger( Match m ) {
		bos = m.getParams().getAnnouncementOptions();     
		match = m;
		params = m.getParams();
        messages = MessageSerializer.getMessageSerializer( params.getName() );
	}

	private Channel getChannel(MatchState state) {
		if (silent) return Channels.NullChannel;
		return bos != null && bos.hasOption( true, state ) ? bos.getChannel(true,state) 
		                                                 : AnnouncementOptions.getDefaultChannel(true,state);
	}

    public void sendOnPreStartMsg(List<ArenaTeam> teams) {
        sendOnPreStartMsg( teams, getChannel( MatchState.ONPRESTART ) );
    }
    
    public void sendOnPreStartMsg( Collection<ArenaTeam> teams, Channel serverChannel ) {
        sendMessageToTeams( serverChannel, teams, "prestart", "server_prestart", params.getSecondsTillMatch() );
    }
    
    public void sendOnStartMsg( List<ArenaTeam> teams ) {
        sendMessageToTeams( getChannel( MatchState.ONSTART ), teams, "start", "server_start", null );
    }

    private void sendMessageToTeams( Channel serverChannel, Collection<ArenaTeam> teams, 
                                        String path, String serverpath, Integer seconds ) {
        
        String nTeamPath = messages.getStringPathFromSize(teams.size());
        Message message = messages.getNodeMessage( typedot + nTeamPath + "." + path );
        Message serverMessage = messages.getNodeMessage( typedot + nTeamPath + "." + serverpath );
        
        if ( Defaults.DEBUG ) 
            Log.info( "sendMessageToTeams():-  path=" + path + "  serverpath=" + serverpath + "  nTeamPath='" + nTeamPath + 
                        "'  message='" + message.getMessage() + "'  serverMessage='" + serverMessage.getMessage() + "'" );  

        Set<MessageOption> ops = message.getOptions();
        
        if ( serverChannel != Channels.NullChannel ) {
            ops.addAll( serverMessage.getOptions() );
        }
        MessageFormatter msgf = new MessageFormatter( messages, params, teams.size(), message, ops );

        msgf.formatCommonOptions(teams,seconds);
        for (ArenaTeam t: teams){
            msgf.formatTeamOptions(t,false);
            msgf.formatTwoTeamsOptions(t, teams);
            msgf.formatTeams(teams);
            String newmsg = msgf.getFormattedMessage(message);
            t.sendMessage(newmsg);
        }

        if (serverChannel != Channels.NullChannel){
            String msg = msgf.getFormattedMessage(serverMessage);
            serverChannel.broadcast(msg);
        }
    }

    public void sendOnVictoryMsg(Collection<ArenaTeam> victors, Collection<ArenaTeam> losers) {
        
        int size = (victors != null ? victors.size() : 0) + (losers != null ? losers.size() : 0);
        String nTeamPath = messages.getStringPathFromSize(size);
        
        for ( VictoryCondition vc : match.getVictoryConditions() ) {
            
            if (vc instanceof DefinesLeaderRanking) {
                List<ArenaTeam> leaders = ((DefinesLeaderRanking)vc).getLeaders();
                if (leaders==null)
                    continue;
                int max = Math.min(leaders.size(), 4);
                StringBuilder sb = new StringBuilder();
                for (int i = 0;i<max;i++){
                    sb.append("&6").append(i + 1).append("&e : ").
                            append(TeamUtil.formatName(leaders.get(i))).append("\n");
                }
                String leaderStr = sb.toString();
                if (victors != null){
                    for (ArenaTeam t: victors){
                        t.sendMessage(leaderStr);}
                }
                if (losers != null){
                    for (ArenaTeam t: losers){
                        t.sendMessage(leaderStr);}
                }
                break;
            }
        }
        messages.sendVictory( getChannel( MatchState.ONVICTORY ), victors, losers,
                              typedot + nTeamPath + ".victory", 
                              typedot + nTeamPath + ".loss",
                              typedot + nTeamPath + ".server_victory",
                              params );
    } 
    
    public void sendOnDrawMessage(Collection<ArenaTeam> drawers, Collection<ArenaTeam> losers) {

        int size = ( drawers != null ? drawers.size() 
                                     : 0 ) + 
                   ( losers != null ? losers.size() 
                                    : 0 );
        String nTeamPath = messages.getStringPathFromSize(size);
        
        messages.sendVictory( getChannel( MatchState.ONVICTORY ), null, drawers,
                              typedot + nTeamPath + ".draw", 
                              typedot + nTeamPath + ".draw",
                              typedot + nTeamPath + ".server_draw",
                              params );
    } 

    public void sendYourTeamNotReadyMsg(ArenaTeam t1) {
        Message message = messages.getNodeMessage( "match" + params.getName() + ".your_team_not_ready" );
        Set<MessageOption> ops = message.getOptions();

        MessageFormatter msgf = new MessageFormatter( messages, params, 1, message, ops);
        msgf.formatTeamOptions(t1, false);
        t1.sendMessage(msgf.getFormattedMessage(message));
    }

    public void sendOtherTeamNotReadyMsg(ArenaTeam t1) {
        Message message = messages.getNodeMessage( typedot + params.getName() + ".other_team_not_ready" );
        Set<MessageOption> ops = message.getOptions();

        MessageFormatter msgf = new MessageFormatter( messages, params, 1, message, ops);
        msgf.formatTeamOptions(t1, false);
        t1.sendMessage(msgf.getFormattedMessage(message));
    }

    public void sendAddedToTeam(ArenaTeam team, ArenaPlayer player) {
        Message message = messages.getNodeMessage( "common.added_to_team" );
        Set<MessageOption> ops = message.getOptions();
        MessageFormatter msgf = new MessageFormatter( messages, params, 1, message, ops );
        msgf.formatTeamOptions( team, false );
        msgf.formatPlayerOptions( player );
        team.sendToOtherMembers( player, msgf.getFormattedMessage( message ) );
    }

    public void sendOnIntervalMsg( int remaining, Collection<ArenaTeam> currentLeaders ) {

        Message message = messages.getNodeMessage("match.interval_update");
        Set<MessageOption> ops = message.getOptions();
        MessageFormatter msgf = new MessageFormatter( messages, params, currentLeaders.size(), message, ops);
        msgf.formatCommonOptions(currentLeaders, remaining);
        String msg = msgf.getFormattedMessage(message);
        
        if ( currentLeaders.isEmpty()) {
//          msg = match.getParams().getPrefix()+"&e ends in &4" +timeStr;
        } 
        else if (currentLeaders.size() == 1){
            ArenaTeam currentLeader = currentLeaders.iterator().next();
            Message message2 = messages.getNodeMessage("match.interval_update_winning");
            ops = message2.getOptions();
            msgf = new MessageFormatter( messages, params, currentLeaders.size(), message2, ops);
            msgf.formatCommonOptions(currentLeaders, remaining);
            msgf.formatTeamOptions(currentLeader, true);
            msg += msgf.getFormattedMessage(message2);
            
            if (msg.contains("{winnerpointsfor}"))
                msg = msg.replaceAll("\\{winnerpointsfor\\}", currentLeader.getNKills()+"");
            
            if (msg.contains("{winnerpointsagainst}"))
                msg = msg.replaceAll("\\{winnerpointsagainst\\}", currentLeader.getNDeaths()+"");
        } 
        else {
            String teamStr = MessageUtil.joinTeams(currentLeaders,"&e and ");
            Message message2 = messages.getNodeMessage("match.interval_update_tied");
            msgf = new MessageFormatter( messages, params, currentLeaders.size(), message2, ops);
            msgf.formatCommonOptions(currentLeaders, remaining);
            msg += msgf.getFormattedMessage(message2);
            if (msg.contains("{teams}"))
                    msg = msg.replaceAll("\\{teams\\}", teamStr);       
        }
        MatchMessageEvent event = new MatchIntervalMessageEvent( match, MatchState.ONMATCHINTERVAL, 
                                                                 getChannel( MatchState.ONMATCHINTERVAL ), "", msg, remaining );
        match.callEvent(event);
        String emessage = event.getMatchMessage();
        if (emessage != null && !emessage.isEmpty())
            match.sendMessage(emessage);
        
        emessage = event.getServerMessage();
        if (event.getServerChannel() != Channels.NullChannel && emessage != null && !emessage.isEmpty())
            event.getServerChannel().broadcast(emessage);
    }

    public void sendTimeExpired() {

        MatchMessageEvent event = new MatchTimeExpiredMessageEvent( match, 
                                                                    MatchState.ONMATCHTIMEEXPIRED,
                                                                    getChannel( MatchState.ONMATCHTIMEEXPIRED ), "", "" );
        match.callEvent(event);
        String message = event.getMatchMessage();
        if (message != null && !message.isEmpty())
            match.sendMessage(message);
        message = event.getServerMessage();
        if (event.getServerChannel() != Channels.NullChannel && message != null && !message.isEmpty())
            event.getServerChannel().broadcast(message);
    }

    public String getMessage(String node) {
        return getMessage( node, null );
    }

    public String getMessage(String node, Map<String, String> map) {
        String text = messages.getNodeText(node);
        return text == null ? null : format(text,map);
    }

    public void sendMessage(String node) {
        sendMessage(node,null);
    }

    public void sendMessage(String node, Map<String, String> map) {
        String msg = getMessage(node,map);
        if (msg != null && !msg.isEmpty())
            match.sendMessage(msg);
    }

    public String format(String text, Map<String, String> map) {
        if ( map == null || map.isEmpty())
            return text;
        String[] searchList =new String[map.size()];
        String[] replaceList =new String[map.size()];
        int i = 0;
        for(Map.Entry<String,String> entry : map.entrySet()){
            searchList[i] = entry.getKey();
            replaceList[i] = entry.getValue();
            i++;
        }
        return StringUtils.replaceEachRepeatedly( text, searchList, replaceList );
    }
    
    public void sendCountdownTillPrestart( int remaining ) {
        
        Channel channel = getChannel( MatchState.ONCOUNTDOWNTOEVENT );
        Message message = messages.getNodeMessage("event.countdownTillEvent");
        Message serverMessage = messages.getNodeMessage("event.server_countdownTillEvent");
        Set<MessageOption> ops = message.getOptions();
        
        if ( channel != Channels.NullChannel ) {
            ops.addAll( serverMessage.getOptions() );
        }
        MessageFormatter msgf = new MessageFormatter( messages, params, 0, message, ops );
        msgf.formatCommonOptions( null, remaining );
        
        if ( channel != Channels.NullChannel ) 
            channel.broadcast( msgf.getFormattedMessage( serverMessage ) );
    }
}
