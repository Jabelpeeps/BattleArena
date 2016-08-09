package mc.alk.arena.objects.victoryconditions;

import mc.alk.arena.competition.Match;
import mc.alk.arena.objects.arenas.ArenaListener;

public class ChangeStateCondition implements ArenaListener{
	protected final Match match;

	public ChangeStateCondition(Match _match){
		match = _match;
	}
}
