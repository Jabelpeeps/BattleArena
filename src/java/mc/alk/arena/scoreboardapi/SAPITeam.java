package mc.alk.arena.scoreboardapi;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.OfflinePlayer;

public class SAPITeam extends SAPIEntry implements STeam{
	protected SScoreboard board;

	public SAPITeam(SScoreboard board, String id, String displayName) {
		super(id, displayName);
		this.board = board;
	}

	@Override
    public void addPlayer(OfflinePlayer p) {
		board.createEntry(p);
	}

    @Override
    public void addPlayer(OfflinePlayer p, int defaultPoints) {
        board.createEntry(p);
        /// no points currently for sapiteams
    }

    @Override
	public void addPlayers(Collection<? extends OfflinePlayer> players) {
		for (OfflinePlayer p: players){
			addPlayer(p);
		}
	}

	@Override
    public void removePlayer(OfflinePlayer p) {
		board.removeEntry(p);
	}

	@Override
    public Set<String> getPlayers() {
		return new HashSet<>(0);
	}

	@Override
    public void setPrefix(String prefix){
		/* do nothing */
	}

	@Override
    public void setSuffix(String suffix){
		/* do nothing */
	}

	@Override
	public String getPrefix() {
		return null;
	}

	@Override
	public String getSuffix() {
		return null;
	}

    @Override
    public int size() {
        return 0;
    }
}
