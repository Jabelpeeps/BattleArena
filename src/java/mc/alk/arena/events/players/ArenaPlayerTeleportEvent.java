package mc.alk.arena.events.players;

import lombok.Getter;
import mc.alk.arena.controllers.TeleportLocationController.TeleportDirection;
import mc.alk.arena.listeners.PlayerHolder.LocationType;
import mc.alk.arena.objects.ArenaLocation;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.arenas.ArenaType;
import mc.alk.arena.objects.teams.ArenaTeam;

public class ArenaPlayerTeleportEvent extends ArenaPlayerEvent{
    @Getter final ArenaTeam team;
    @Getter final ArenaLocation srcLocation;
    @Getter final ArenaLocation destLocation;
	@Getter final TeleportDirection direction;
	@Getter final ArenaType arenaType;

	public ArenaPlayerTeleportEvent(ArenaType at, ArenaPlayer arenaPlayer, ArenaTeam _team,
			                        ArenaLocation src, ArenaLocation dest, TeleportDirection _direction) {
		super(arenaPlayer);
		arenaType = at;
		team = _team;
		srcLocation = src;
		destLocation = dest;
		direction = _direction;
	}
	public LocationType getSrcType(){ return srcLocation.getType(); }
	public LocationType getDestType(){ return destLocation.getType(); }
}
