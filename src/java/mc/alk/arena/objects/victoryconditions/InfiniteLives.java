package mc.alk.arena.objects.victoryconditions;

import mc.alk.arena.competition.Match;

public class InfiniteLives extends NLives{

	public InfiniteLives(Match match) {
		super(match, Integer.MAX_VALUE);
	}

}
