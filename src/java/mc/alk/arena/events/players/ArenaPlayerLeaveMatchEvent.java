package mc.alk.arena.events.players;

import lombok.Getter;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.teams.ArenaTeam;

/**
 * Signifies that the player typed the command to leave the competition
 */
public class ArenaPlayerLeaveMatchEvent extends ArenaPlayerEvent{
	@Getter final ArenaTeam team;

	public ArenaPlayerLeaveMatchEvent(ArenaPlayer arenaPlayer, ArenaTeam _team) {
		super(arenaPlayer);
		team = _team;
	}
}
