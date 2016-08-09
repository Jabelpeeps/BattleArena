package mc.alk.arena.objects.victoryconditions;

import mc.alk.arena.competition.Match;

public class NoTeamsLeft extends NTeamsNeeded{
	public NoTeamsLeft(Match match) {
		super(match,1);
	}
}
