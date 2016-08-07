package mc.alk.scoreboardapi.api;

import mc.alk.scoreboardapi.scoreboard.SAPIDisplaySlot;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.List;


public interface SScoreboard {
	public SObjective registerNewObjective(String objectiveName, String criteria, String displayName,
			SAPIDisplaySlot slot) ;

	public void setDisplaySlot(SAPIDisplaySlot slot, SObjective objective) ;

	public void setDisplaySlot(SAPIDisplaySlot slot, SObjective objective, boolean fromObjective) ;

	public SObjective getObjective(SAPIDisplaySlot slot) ;

	public SObjective getObjective(String id) ;

	public List<SObjective> getObjectives();

	public String getPrintString() ;

	public SEntry createEntry(OfflinePlayer p) ;

	public SEntry createEntry(OfflinePlayer p, String displayName) ;

	public SEntry createEntry(String id, String displayName) ;

	public STeam createTeamEntry(String id, String displayName) ;

	public SEntry removeEntry(OfflinePlayer p) ;

	public SEntry removeEntry(SEntry e) ;

	public String getName();

	public SEntry getEntry(String id) ;

	public SEntry getEntry(OfflinePlayer player);

	public STeam getTeam(String id);

	public void clear();

	public SObjective registerNewObjective(SObjective objective);

	public SEntry getOrCreateEntry(OfflinePlayer p);

	public Collection<SEntry> getEntries();

    public void removeScoreboard(Player player);

    public void setScoreboard(Player p);

    public boolean setEntryDisplayName(String id, String name);

    public void setEntryDisplayName(SEntry e, String name);

    boolean setEntryNamePrefix(String id, String name);

    void setEntryNamePrefix(SEntry entry, String name);

    boolean setEntryNameSuffix(String id, String name);

    void setEntryNameSuffix(SEntry entry, String name);

    public Plugin getPlugin();

    boolean hasThisScoreboard(Player player);
}
