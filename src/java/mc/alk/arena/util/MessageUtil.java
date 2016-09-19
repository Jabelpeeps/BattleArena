package mc.alk.arena.util;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import mc.alk.arena.Defaults;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.messaging.MessageHandler;
import mc.alk.arena.objects.teams.ArenaTeam;

public class MessageUtil {

	public static String colorChat(String msg) { return msg.replace('&', (char) 167); }

	public static String decolorChat(String msg) {
		return msg.contains("§") || msg.contains("&") ? ChatColor.stripColor(msg).replaceAll("(&|§).", "") : msg;
	}

    public static void sendSystemMessage(CommandSender p, String nodeString, Object... varArgs) {
        sendMessage(p, MessageHandler.getSystemMessage( nodeString, varArgs ));
    }
    public static void sendSystemMessage(ArenaPlayer p, String nodeString, Object... varArgs) {
        sendMessage(p, MessageHandler.getSystemMessage( nodeString, varArgs ));
    }
    public static void sendMessage(Collection<ArenaPlayer> players, String message) {
        for ( ArenaPlayer ap : players )
            sendMessage( ap.getPlayer(), message );
    }
    public static void sendMessage(ArenaPlayer player, String message) {
         sendMessage( player.getPlayer(), message );
    }
    public static void sendMessage(Set<Player> players, String message) {
        for (Player each : players){
            sendMessage( each, message );
        }
    }
    public static void sendMessage(CommandSender sender, String message) {
        
		if ( message == null || message.isEmpty() ) {
		    if ( Defaults.DEBUG ) { 
		        Log.err( "Attempting to send empty or null string to " + sender.getName() );
		        Util.printStackTrace();
		    }
		    return;
		}	
        message = colorChat( message.trim() );
		
		if ( message.contains("\n") )
			sendMultilineMessage( sender, message );
		else 
		    sendLine( sender, message );
		
	}
    
    public static void sendMessage(ArenaPlayer player, List<String> messages) {
        for ( String line : messages ) {
            line = colorChat( line.trim() );
            sendLine( player.getPlayer(), line );
        }
    }
    
	private static void sendMultilineMessage(CommandSender sender, String message) {
		for ( String line : message.split("\n") )
			sendLine( sender, line) ;
	}

	private static void sendLine( CommandSender sender, String message ) {	    
	    if ( sender instanceof Player && ((Player) sender).isOnline() )
            sender.sendMessage( message );
	    else if ( sender instanceof ConsoleCommandSender ) 
            sender.sendMessage( message );
	}
	
	public static String minuteOrMinutes(int minutes) { return minutes == 1 ? "minute" : "minutes"; }
	public static String getTeamsOrPlayers(int teamSize) { return teamSize == 1 ? "players" : "teams"; }
	public static String teamsOrPlayers(int nPlayersPerTeam){ return nPlayersPerTeam > 1 ? "teams" : "players";} 
	public static String playerOrPlayers(int n) { return n > 1 ? "players" : "player"; }
	public static String hasOrHave(int size) { return size == 1 ? "has" : "have"; }

	public static ChatColor getFirstColor(String str) {
		int index = str.indexOf('§');
		
        if ( index == -1 ) index = str.indexOf('&');
        
        if ( index != -1 && str.length() > index + 1 ) {
			ChatColor cc = ChatColor.getByChar( str.charAt( index + 1 ) );
			if (cc != null)
				return cc;
		}
		return ChatColor.WHITE;
	}

	public static String joinTeams(Collection<ArenaTeam> teams, String joinStr){
	    StringJoiner joiner = new StringJoiner( joinStr );

		for ( ArenaTeam team : teams ) {
			joiner.add(team.getDisplayName() );
		}
		return joiner.toString();
	}

	public static String joinPlayers(Collection<ArenaPlayer> players, String joinStr){
        StringJoiner joiner = new StringJoiner( joinStr );
        
		for ( ArenaPlayer player : players ) {
			joiner.add(player.getName());
		}
		return joiner.toString();
	}
}
