package mc.alk.arena.events.players;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.teams.ArenaTeam;

/**
 * Signifies that the player has typed the command to leave the competition
 */
public class ArenaPlayerLeaveEvent extends ArenaPlayerEvent{
	public enum QuitReason {
		QUITCOMMAND, QUITMC, KICKED, OTHER
	}
	@Getter final ArenaTeam team;
	final QuitReason reason;
	@Getter @Setter boolean handledQuit = false;
	@Getter List<String> messages = null;

	public ArenaPlayerLeaveEvent(ArenaPlayer arenaPlayer, ArenaTeam _team, QuitReason _reason) {
		super(arenaPlayer);
		team = _team;
		reason = _reason;
	}

	public void addMessage(String str) {
		if (messages == null)
			messages = new ArrayList<>();
		
		if (!messages.contains(str))
			messages.add(str);
	}
}
