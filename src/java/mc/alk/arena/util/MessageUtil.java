package mc.alk.arena.util;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import mc.alk.arena.Defaults;
import mc.alk.arena.controllers.messaging.MessageHandler;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.teams.ArenaTeam;

public class MessageUtil {

	public static String colorChat(String msg) { return msg.replace('&', (char) 167); }

	public static String decolorChat(String msg) {
		return msg.contains("§") || msg.contains("&") ? ChatColor.stripColor(msg).replaceAll("(&|§).", "") : msg;}

    public static boolean sendSystemMessage(CommandSender p, String nodeString, Object... varArgs) {
        return sendMessage(p, MessageHandler.getSystemMessage( nodeString, varArgs ));
    }
    public static boolean sendSystemMessage(ArenaPlayer p, String nodeString, Object... varArgs) {
        return sendMessage(p, MessageHandler.getSystemMessage( nodeString, varArgs ));
    }
    public static void sendMessage(Collection<ArenaPlayer> players, String message) {
        for ( ArenaPlayer ap : players )
            sendMessage( ap.getPlayer(), message );
    }
    public static boolean sendMessage(ArenaPlayer player, String message) {
         return sendMessage( player.getPlayer(), message );
    }
    public static void sendMessage(Set<Player> players, String message) {
        for (Player each : players){
            sendMessage( each, message );
        }
    }
    public static boolean sendMessage(CommandSender sender, String message) {
        
		if ( message ==null || message.isEmpty() ) {
		    if ( Defaults.DEBUG ) Log.err( "Attempting to send empty or null string to " + sender.getName() );
		    return true;
		}
		
        message = colorChat( message.trim() );
		
		if ( message.contains("\n") )
			return sendMultilineMessage( sender, message );
		
        return sendLine( sender, message );
	}
    
    public static boolean sendMessage(ArenaPlayer player, List<String> messages) {
        for ( String line : messages ) {
            line = colorChat( line.trim() );
            sendLine( player.getPlayer(), line );
        }
        return true;
    }
    
	private static boolean sendMultilineMessage(CommandSender sender, String message) {
		
		String[] lines = message.split("\n");
		
		for (String line: lines){
			sendLine( sender, line) ;
		}
		return true;
	}

	private static boolean sendLine( CommandSender sender, String message ) {
	    
	    if ( sender instanceof Player && ((Player) sender).isOnline() )
            sender.sendMessage( message );
	    
	    else if ( sender instanceof ConsoleCommandSender ) 
            sender.sendMessage( message );
	    
	    return true;
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

    public static void broadcastMessage(String message) {
        try {
            Bukkit.broadcastMessage(message);
        } 
        catch (Throwable e){ }
    }
}
