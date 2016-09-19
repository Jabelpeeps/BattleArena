package mc.alk.arena.objects.messaging;

import java.util.IllegalFormatException;
import java.util.List;

import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.serializers.MessageSerializer;
import mc.alk.arena.util.Log;

public class MessageHandler {

	public static String getSystemMessage( List<Object> vars, String string, Object... varArgs ) {
	    
		Message msg = MessageSerializer.getDefaultMessage( "system." + string );		
		if  ( msg == null ) return null;
		
		String stringmsg = msg.getMessage();
		
		if ( vars != null && !vars.isEmpty() && !stringmsg.contains("{") ) {	
    		for ( Object o : vars ) {
    			if ( o instanceof MatchParams) {
    				stringmsg = stringmsg.replaceAll( "\\{matchname\\}", ((MatchParams) o).getName() );
    				stringmsg = stringmsg.replaceAll( "\\{cmd\\}", ((MatchParams) o).getCommand() );
    			} 
    			else if ( o instanceof ArenaTeam ) {
    				stringmsg = stringmsg.replaceAll( "\\{team\\}", ((ArenaTeam) o).getName() );
    			}
    		}
		}
		return formatMessage( stringmsg, varArgs );
	}

	public static String getSystemMessage(MatchParams params, String string, Object... varArgs) {
	    
		Message msg = MessageSerializer.getDefaultMessage( "system." + string );		
		if ( msg == null ) return null;
		
		String stringmsg = msg.getMessage();
		
		if ( stringmsg.indexOf('{') != -1 ) {
            stringmsg = stringmsg.replaceAll( "\\{matchname\\}", params.getName() );
            stringmsg = stringmsg.replaceAll( "\\{cmd\\}", params.getCommand() );
            stringmsg = stringmsg.replaceAll( "\\{prefix\\}", params.getPrefix() );	    
		}	
		return formatMessage( stringmsg, varArgs );
	}

	public static String getSystemMessage( String string, Object... varArgs ) {
	    
		Message msg = MessageSerializer.getDefaultMessage( "system." + string );		
		if ( msg == null ) return null;
		
        return formatMessage( msg.getMessage(), varArgs );
	}
	
	private static String formatMessage( String string, Object... varArgs ) {
	    try {
            return String.format( string, varArgs );
        } 
        catch ( IllegalFormatException e ) {
            String err = "&c[BA Message Error] system.+" + string;
            Log.err( err );
            for ( Object o: varArgs ) {
                Log.err( "Message Option: " + o );    
            }
            Log.printStackTrace(e);
            return err;
        }
	}
}
