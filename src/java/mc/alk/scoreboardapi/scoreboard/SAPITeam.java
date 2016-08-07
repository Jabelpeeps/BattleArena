package mc.alk.scoreboardapi.scoreboard;

import mc.alk.scoreboardapi.api.STeam;
import org.bukkit.OfflinePlayer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SAPITeam extends SAPIEntry implements STeam{
	protected SAPIScoreboard board;

	public SAPITeam(SAPIScoreboard board, String id, String displayName) {
		super(id, displayName);
		this.board = board;
	}

	@Override
    public void addPlayer(OfflinePlayer p) {
		this.board.createEntry(p);
	}

    @Override
    public void addPlayer(OfflinePlayer p, int defaultPoints) {
        this.board.createEntry(p);
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
		this.board.removeEntry(p);
	}

	@Override
    public Set<OfflinePlayer> getPlayers() {
		return new HashSet<OfflinePlayer>(0);
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
