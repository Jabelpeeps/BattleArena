package mc.alk.arena.events.players;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mc.alk.arena.events.CompetitionEvent;
import mc.alk.arena.objects.ArenaPlayer;

@Getter @AllArgsConstructor
public class ArenaPlayerEvent extends CompetitionEvent {
	final ArenaPlayer player;
}
