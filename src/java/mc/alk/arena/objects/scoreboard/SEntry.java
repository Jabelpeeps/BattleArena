package mc.alk.arena.objects.scoreboard;

public interface SEntry {
//    /**
//     * Get the offline player denoted by this Entry
//     * @return OfflinePlayer
//     */
//	public OfflinePlayer getOfflinePlayer();

    /**
     * Get the id of this entry (offlineplayer name)
     * @return String
     */
	public String getId() ;

    /**
     * Get the prefix+displayName+suffix
     * @return return the full display name (prefix+displayName+suffix)
     */
	public String getDisplayName();

    /**
     * Get the base displayName
     * @return the base displayName
     */
    public String getBaseDisplayName();

    /**
     * Set the displayName
     * @param displayName the display name
     */
	public void setDisplayName(String displayName);

    /**
     * This will put the given suffix at the end of the name
     * name + suffix should be <= 16 chars otherwise the
     * suffix will cover part of the name
     * @param suffix Suffix: max length of 8
     */
    public void setDisplayNamePrefix(String suffix);

    /**
     * Get the current prefix
     * @return String: the prefix
     */
    public String getDisplayNamePrefix();

    /**
     * This will put the given suffix at the end of the name
     * name + suffix should be <= 16 chars otherwise the
     * suffix will cover part of the name
     * @param suffix Suffix: max length of 8
     */
    public void setDisplayNameSuffix(String suffix);

    /**
     * Get the current suffix
     * @return String: the suffix
     */
    public String getDisplayNameSuffix();
}
