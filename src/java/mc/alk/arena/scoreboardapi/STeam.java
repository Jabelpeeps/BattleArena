package mc.alk.arena.scoreboardapi;

import java.util.Collection;
import java.util.Set;

import org.bukkit.OfflinePlayer;

public interface STeam extends SEntry {
	public void addPlayer(OfflinePlayer p);

    public void addPlayer(OfflinePlayer p, int defaultPoints);

    public void addPlayers(Collection<? extends OfflinePlayer> players);

	public void removePlayer(OfflinePlayer p);

	public Set<? extends OfflinePlayer> getPlayers();

	public void setPrefix(String prefix);

	public void setSuffix(String suffix);

	public String getPrefix();

	public String getSuffix();

    public int size();
}
