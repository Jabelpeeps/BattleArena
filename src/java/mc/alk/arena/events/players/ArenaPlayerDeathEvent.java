package mc.alk.arena.events.players;

import org.bukkit.event.entity.PlayerDeathEvent;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.teams.ArenaTeam;

public class ArenaPlayerDeathEvent extends ArenaPlayerEvent {
    @Getter final ArenaTeam team;
    @Getter PlayerDeathEvent playerDeathEvent;
	@Getter @Setter boolean exiting = false;

	public ArenaPlayerDeathEvent(ArenaPlayer arenaPlayer, ArenaTeam _team, PlayerDeathEvent event) {
		super( arenaPlayer );
		team = _team;
		playerDeathEvent = event;
	}
}
