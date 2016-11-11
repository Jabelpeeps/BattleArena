package mc.alk.arena.objects;

import lombok.Getter;

public class CommandLineString {
	private enum SenderType { CONSOLE, PLAYER }
	
	@Getter String rawCommand;
	@Getter SenderType senderType;
	String command;

	public static CommandLineString parse( String line ) throws IllegalArgumentException {
		try {
			CommandLineString cls = new CommandLineString();
			int index = line.indexOf(' ');
			cls.senderType = SenderType.valueOf( line.substring( 0, index ).toUpperCase() );
			cls.command = line.substring( index ).trim();
			cls.rawCommand = line;
			return cls;
		} catch (Exception e){
			throw new IllegalArgumentException("Format for commands must be: <player or console> <commands> ... <commands>");
		}
	}
	public boolean isConsoleSender() {
		return senderType == SenderType.CONSOLE;
	}
	public String getCommand( String playerName ) {
		return command.contains("player") ? command.replaceAll("player", playerName) : command;
	}
}
