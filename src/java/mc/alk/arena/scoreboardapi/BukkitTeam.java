package mc.alk.arena.scoreboardapi;

import java.util.Collection;
import java.util.Set;

import org.bukkit.OfflinePlayer;
import org.bukkit.scoreboard.Team;

import mc.alk.arena.util.MessageUtil;

class BukkitTeam extends SAPIEntry implements STeam {
    protected SScoreboard board;
	Team team;

	public BukkitTeam(SScoreboard sScoreboard, Team _team) {
		super( _team.getName(), _team.getDisplayName());
		team = _team;
		board = sScoreboard;
	}

	@Override
	public void addPlayers(Collection<? extends OfflinePlayer> players) {
		for (OfflinePlayer p : players){
			team.addEntry(p.getName());
		}
		if (board != null){
			for (SObjective o : board.getObjectives()){
				if (o.isDisplayPlayers() && o.contains(this)){
					for (String player: team.getEntries()){
                        SEntry e = o.getScoreboard().getEntry(player);
                        if (o.getPoints(e) == -1){
                            o.addEntry(player, 0);
                        }
					}
				}
			}
		}
	}
    @Override
    public void addPlayer(OfflinePlayer p) {
        addPlayer(p, 0);
    }

    @Override
    public void addPlayer(OfflinePlayer p, int defaultPoints) {
        board.createEntry(p);

        team.addEntry(p.getName()); /// Note: no spigot speed problems
        if (board != null && defaultPoints != Integer.MIN_VALUE){
            for (SObjective o : board.getObjectives()){
                if (o.isDisplayPlayers() && o.contains(this)) {
                    SEntry e = o.getScoreboard().getEntry(p);
                    if (o.getPoints(e) == -1){
                        o.addEntry(p, 0);
                    }
                }
            }
        }
    }


	@Override
	public void removePlayer(OfflinePlayer p){
        board.removeEntry(p);
		team.removeEntry(p.getName());
	}

	@Override
	public Set<String> getPlayers() {
		return team.getEntries();
	}

	@Override
	public void setPrefix(String prefix){
		prefix = MessageUtil.colorChat(prefix);
		team.setPrefix(prefix);
	}

	@Override
	public void setSuffix(String suffix){
		suffix = MessageUtil.colorChat(suffix);
		team.setSuffix(suffix);
	}

	@Override
	public String getPrefix() {
		return team.getPrefix();
	}

	@Override
	public String getSuffix() {
		return team.getSuffix();
	}

    @Override
    public int size() {
        return team.getSize();
    }
}
