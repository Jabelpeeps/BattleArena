package mc.alk.arena.objects.victoryconditions;

import mc.alk.arena.competition.Match;
import mc.alk.arena.events.players.ArenaPlayerDeathEvent;
import mc.alk.arena.objects.events.ArenaEventHandler;
import mc.alk.arena.objects.events.ArenaEventHandler.ArenaEventPriority;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.objects.victoryconditions.interfaces.DefinesNumLivesPerPlayer;

public class NLives extends VictoryCondition implements DefinesNumLivesPerPlayer{
	int nLives; /// number of lives before a player is eliminated from a team

	public NLives(Match _match) {
		super(_match);
		nLives = 1;
	}

	public NLives(Match _match, Integer maxLives) {
		super(_match);
		nLives = maxLives;
	}

	public void setMaxLives(Integer maxLives) {
		nLives = maxLives;
	}

	@ArenaEventHandler( priority = ArenaEventPriority.LOW )
	public void playerDeathEvent(ArenaPlayerDeathEvent event) {
		ArenaTeam team = event.getTeam();
		Integer deaths = team.getNDeaths(event.getPlayer());
		if ( deaths == 0 ) deaths = 1;
		if ( deaths >= nLives )
			team.killMember(event.getPlayer());
	}

	@Override
	public int getLivesPerPlayer() {
		return nLives;
	}
}
