package mc.alk.arena.events.players;

import lombok.Getter;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.arenas.Arena;

public class ArenaPlayerLeaveQueueEvent extends ArenaPlayerEvent {
    @Getter final MatchParams params;
    @Getter final Arena arena;

    public ArenaPlayerLeaveQueueEvent(ArenaPlayer arenaPlayer, MatchParams _params, Arena _arena) {
        super(arenaPlayer);
        params = _params;
        arena = _arena;
    }
}
