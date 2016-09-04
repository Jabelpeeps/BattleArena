package mc.alk.arena.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mc.alk.arena.objects.arenas.Arena;

@AllArgsConstructor @Getter
public class ArenaDeleteEvent extends BAEvent {
	final Arena arena;
}
