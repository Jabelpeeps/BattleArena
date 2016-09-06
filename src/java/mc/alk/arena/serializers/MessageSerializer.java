package mc.alk.arena.serializers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Setter;
import mc.alk.arena.Defaults;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.messaging.AnnouncementOptions;
import mc.alk.arena.objects.messaging.Channel;
import mc.alk.arena.objects.messaging.Channels;
import mc.alk.arena.objects.messaging.Message;
import mc.alk.arena.objects.messaging.MessageFormatter;
import mc.alk.arena.objects.messaging.MessageOptions;
import mc.alk.arena.objects.messaging.MessageOptions.MessageOption;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.util.Log;


public class MessageSerializer extends BaseConfig {

	@Setter private static MessageSerializer defaultMessages;
	private HashMap<String, MessageOptions> msgOptions = new HashMap<>();
    final private static HashMap<String, MessageSerializer> files = new HashMap<>();
    final protected MatchParams matchParams;
    protected String name;
    protected AnnouncementOptions bos;

	public MessageSerializer( String _name, MatchParams params ) {
		matchParams = params;
		
		if (_name == null ) return;		
		name = _name;		
		MessageSerializer ms = files.get( _name.toUpperCase() );
		
		if ( ms != null ) {
			config = ms.config;
			file = ms.file;
			msgOptions = ms.msgOptions;
		}
	}

	@Override
    public BaseConfig setConfig( File _file ) {
	    if ( super.setConfig( _file ) != null ) {
            files.put( name.toUpperCase(), this );
            return this;
	    }
        return null;	    
	}
	
	public static void addMessageSerializer(String name, MessageSerializer ms){
		files.put( name.toUpperCase(), ms );
	}

	public static MessageSerializer getMessageSerializer( String name ){
		return files.get(name.toUpperCase());
	}

	public static void reloadConfig(String params) {
		MessageSerializer ms = files.get( params.toUpperCase() );
		if ( ms != null ) {
			ms.reloadFile();
			ms.initMessageOptions();
		}
	}

	public void initMessageOptions(){
		if ( config == null ) {
		    Log.info( "MessageSerialiser.initMessageOptions() called when config still null!" );
		    return;
		}
		
		msgOptions.clear();
		Set<String> keys = config.getKeys( true );
		keys.remove("version");
		
		for ( String key : keys ) {
			Object obj = config.get(key);
			if ( obj == null ) continue;
			
			String options = String.valueOf( obj );
			if ( Defaults.DEBUG_MSGS ) Log.info( options );
			msgOptions.put( key, new MessageOptions( options ) );
		}
	}

	public static Message getDefaultMessage(String path) {
		return defaultMessages != null ? defaultMessages.getNodeMessage( path ) : null;
	}

	public Message getNodeMessage( String path ) {
		if ( config != null && config.contains( path ) ) {
			return new Message( config.getString( path ), msgOptions.get( path ));
		}
		if ( this != defaultMessages ) {
			return defaultMessages.getNodeMessage( path );
		}
        return null;
	}

	public String getNodeText(String path) {
		if (config != null && config.contains(path)){
			return config.getString(path);
		}
		if (this != defaultMessages){
			return defaultMessages.getNodeText(path);
		}
        return null;
	}

	private boolean contains(String path) {
		return config.contains(path);
	}

	public static boolean hasMessage(String prefix, String node) {
		return defaultMessages != null && defaultMessages.contains( prefix + "." + node );
	}

	public static void loadDefaults() {
		if ( defaultMessages != null ) defaultMessages.reloadFile();
	}

	protected static String getStringPathFromSize(int size) {
		if (size == 1) return "oneTeam";
		else if (size == 2) return "twoTeams";
		else  return "multipleTeams";	
	}

	protected void sendVictory( Channel serverChannel, 
	                            Collection<ArenaTeam> victors, 
	                            Collection<ArenaTeam> losers, 
	                            String winnerpath,
	                            String loserpath, 
	                            String serverPath ) {
		
	    int size = victors != null ? victors.size() : 0;
		size += losers != null ? losers.size() : 0;
		
		Message winnermessage = getNodeMessage( winnerpath );
		Message losermessage = getNodeMessage( loserpath );
		Message serverMessage = getNodeMessage( serverPath );

		Set<MessageOption> ops = winnermessage.getOptions();
		if ( ops == null ) ops = new HashSet<>();
		
		ops.addAll( losermessage.getOptions() );
		
		if ( serverChannel != Channels.NullChannel && serverMessage != null ) {
			ops.addAll( serverMessage.getOptions() );
		}

		MessageFormatter msgf = new MessageFormatter( this, matchParams, size, losermessage, ops );
        List<ArenaTeam> teams;
        
        if ( losers != null ) 
            teams = new ArrayList<>( losers );
        else 
            teams = new ArrayList<>();

        if ( victors != null ) {
			teams.addAll( victors );
		}

		msgf.formatCommonOptions( teams, matchParams.getSecondsToLoot() );
		ArenaTeam vic = ( victors != null && !victors.isEmpty() ) ? victors.iterator().next() : null;
        if ( losers != null ) {
            for ( ArenaTeam t : losers ) {
                msgf.formatTeamOptions( t, false );
                msgf.formatTwoTeamsOptions( t, teams );
                msgf.formatTeams( teams );
                msgf.formatWinnerOptions( t, false );
                /// TODO : I now need to make this work with multiple winners
                if ( vic != null )
                    msgf.formatWinnerOptions( vic, true );
                t.sendMessage( msgf.getFormattedMessage( losermessage ) );
            }
        }

		if ( victors != null ) {
			for ( ArenaTeam victor : victors ) {
				msgf = new MessageFormatter( this, matchParams, size, winnermessage, ops );
				msgf.formatCommonOptions( teams, matchParams.getSecondsToLoot() );
				msgf.formatTeamOptions( victor, true );
				msgf.formatTwoTeamsOptions( victor, teams );
				msgf.formatTeams( teams );
				if ( losers != null && !losers.isEmpty() ) {
					msgf.formatWinnerOptions( losers.iterator().next(), false );
				}
				msgf.formatWinnerOptions( victor, true );
				victor.sendMessage( msgf.getFormattedMessage( winnermessage ) );
			}
		}

		if ( serverChannel != Channels.NullChannel && serverMessage != null ) {
            String msg = msgf.getFormattedMessage( serverMessage );
			serverChannel.broadcast( msg );
		}
	}
}
