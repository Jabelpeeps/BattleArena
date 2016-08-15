package mc.alk.arena.events.players;

import org.bukkit.event.entity.PlayerDeathEvent;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.teams.ArenaTeam;

public class ArenaPlayerKillEvent extends ArenaPlayerEvent{
	@Getter final ArenaPlayer target;
	@Getter final ArenaTeam team;
	@Getter @Setter PlayerDeathEvent playerDeathEvent;

	public ArenaPlayerKillEvent(ArenaPlayer arenaPlayer, ArenaTeam _team, ArenaPlayer _target) {
		super( arenaPlayer );
		team = _team;
		target = _target;
	}

	public int getPoints() {
		return 1;
	}
}
