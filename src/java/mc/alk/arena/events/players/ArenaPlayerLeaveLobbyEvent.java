package mc.alk.arena.events.players;

import lombok.Getter;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.teams.ArenaTeam;

public class ArenaPlayerLeaveLobbyEvent extends ArenaPlayerEvent{
	@Getter final ArenaTeam team;

	public ArenaPlayerLeaveLobbyEvent(ArenaPlayer arenaPlayer, ArenaTeam _team) {
		super(arenaPlayer);
		team = _team;
	}
}
