package mc.alk.arena.events.players;

import org.bukkit.event.Cancellable;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.objects.ArenaPlayer;

/**
 * Signifies that the player has typed the command to Join the competition
 */
public class ArenaPlayerJoinEvent extends ArenaPlayerEvent implements Cancellable{
    @Getter @Setter boolean cancelled = false;
	@Getter @Setter String message;

	public ArenaPlayerJoinEvent(ArenaPlayer arenaPlayer) {
		super(arenaPlayer);
	}

	public void cancelWithMessage(String _message) {
		cancelled = true;
		message = _message;
	}
}
