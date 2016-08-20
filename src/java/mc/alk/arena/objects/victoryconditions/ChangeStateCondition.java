package mc.alk.arena.objects.victoryconditions;

import lombok.AllArgsConstructor;
import mc.alk.arena.competition.Match;
import mc.alk.arena.objects.arenas.ArenaListener;

@AllArgsConstructor
public class ChangeStateCondition implements ArenaListener{
	protected final Match match;
}
