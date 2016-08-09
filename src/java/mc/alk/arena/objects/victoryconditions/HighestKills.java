package mc.alk.arena.objects.victoryconditions;

import org.bukkit.configuration.ConfigurationSection;

import mc.alk.arena.competition.Match;

@Deprecated
public class HighestKills extends PlayerKills {
	public HighestKills(Match match, ConfigurationSection section) {
		super(match, section);
	}
}
