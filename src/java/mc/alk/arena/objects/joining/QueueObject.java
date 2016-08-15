package mc.alk.arena.objects.joining;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.objects.arenas.ArenaListener;
import mc.alk.arena.objects.options.JoinOptions;
import mc.alk.arena.objects.teams.ArenaTeam;

@RequiredArgsConstructor
public abstract class QueueObject {

	@Getter protected Integer priority;
    @Getter final protected JoinOptions joinOptions;
	@Getter final protected MatchParams matchParams;
	@Getter int numPlayers;
    @Getter public List<ArenaListener> listeners;

    public QueueObject(JoinOptions options){
		joinOptions = options;
        matchParams = options.getMatchParams();
    }

	public abstract boolean hasMember(ArenaPlayer p);
	public abstract ArenaTeam getTeam(ArenaPlayer p);
	public abstract int size();
	public abstract List<ArenaTeam> getTeams();
	public abstract boolean hasTeam(ArenaTeam team);

	public long getJoinTime() { return joinOptions.getJoinTime(); }
    public Arena getArena() { return joinOptions.getArena(); }
}
