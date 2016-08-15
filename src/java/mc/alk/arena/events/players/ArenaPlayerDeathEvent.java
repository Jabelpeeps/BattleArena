package mc.alk.arena.events.players;

import org.bukkit.event.entity.PlayerDeathEvent;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.teams.ArenaTeam;

public class ArenaPlayerDeathEvent extends ArenaPlayerEvent{
    @Getter final ArenaTeam team;
    @Getter @Setter PlayerDeathEvent playerDeathEvent;
	@Getter @Setter boolean exiting = false;

	public ArenaPlayerDeathEvent(ArenaPlayer arenaPlayer, ArenaTeam _team) {
		super(arenaPlayer);
		team = _team;
	}
	public boolean isTrueDeath() {
		return playerDeathEvent != null;
	}
}
