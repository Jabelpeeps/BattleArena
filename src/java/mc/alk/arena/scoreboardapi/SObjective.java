package mc.alk.arena.scoreboardapi;

import org.bukkit.OfflinePlayer;

import java.util.List;


public interface SObjective {
    /**
     * Get the display name: prefix + base + suffix
     * @return the display name
     */
    public String getDisplayName();

    /**
     * Get the display base: base
     * @return the display name
     */
    public String getBaseDisplayName();

    /**
     * prefix + base + suffix must be less than 16 characters
	 * @param displayName the display name of this Objective
	 */
	public void setDisplayName(String displayName);

    /**
     * Get the suffix
     * @return suffix
     */
    public String getDisplayNameSuffix();

	/**
     * prefix + displayName + suffix must be less than or equal 32 characters
	 * @param suffix set the suffix
	 */
	public void setDisplayNameSuffix(String suffix);

    /**
     * Get the prefix
     * @return prefix
     */
    public String getDisplayNamePrefix();
    /**
     * prefix + displayName + suffix must be less than or equal 32 characters
     * @param prefix set the prefix
     */
    public void setDisplayNamePrefix(String prefix);

    public void setDisplaySlot(SAPIDisplaySlot slot);

	public SAPIDisplaySlot getDisplaySlot();

	public String getId();


	public int getPriority();

    public SEntry addEntry(OfflinePlayer p, int defaultPoints);

	public SEntry addEntry(String id, int defaultPoints);

	public boolean addEntry(SEntry entry, int defaultPoints);

	public STeam addTeam(String id, int points);

	public boolean addTeam(STeam entry, int points);

	public SEntry removeEntry(OfflinePlayer player);

	public SEntry removeEntry(String id);

	public SEntry removeEntry(SEntry entry);

	public void setScoreboard(SScoreboard scoreboard);

    int getPoints(String id);

    int getPoints(SEntry e);

    public boolean setPoints(String id, int points);

	public boolean setPoints(SEntry entry, int points);

	public boolean setTeamPoints(STeam team, int points);

	public void setDisplayPlayers(boolean display);

	public void setDisplayTeams(boolean display);

	public boolean isDisplayTeams();

	public boolean isDisplayPlayers();

	public SScoreboard getScoreboard();

	public boolean contains(SEntry e);

    void initPoints(List<SEntry> entries, List<Integer> points);
}
