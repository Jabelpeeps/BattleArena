package mc.alk.tracker;
//package mc.alk.tracker;
//
//import java.util.Collection;
//import java.util.List;
//import java.util.Set;
//
//import org.bukkit.OfflinePlayer;
//import org.bukkit.command.CommandSender;
//import org.bukkit.entity.Player;
//
//import mc.alk.tracker.objects.PlayerStat;
//import mc.alk.tracker.objects.Stat;
//import mc.alk.tracker.objects.StatType;
//import mc.alk.tracker.objects.TeamStat;
//import mc.alk.tracker.objects.WLTRecord;
//import mc.alk.tracker.objects.WLTRecord.WLT;
//import mc.alk.tracker.ranking.RatingCalculator;
//
//
//
//public interface TrackerInterface{
//	public void printTopX(CommandSender sender, StatType statType, int x);
//	public void printTopX(CommandSender sender, StatType statType, int x,String headerMsg, String bodyMsg);
//
//	public void printTopX(CommandSender sender, StatType statType, int x, int teamSize);
//	public void printTopX(CommandSender sender, StatType statType, int x, int teamSize, String headerMsg, String bodyMsg);
//
//	public void addStatRecord(Stat team1, Stat team2, WLT wlt);
//
//	public void addPlayerRecord(String p1, String p2, WLT wlt);
//	public void addPlayerRecord(OfflinePlayer p1, OfflinePlayer p2, WLT wlt);
//
//	public void addTeamRecord(String t1, String t2, WLT wlt);
//	public void addTeamRecord(Set<String> team1, Set<String> team2, WLT wlt);
//	public void addTeamRecord(Collection<Player> team1, Collection<Player> team2, WLT wlt);
//
//	public void changePlayerElo(String p1, String p2, WLT wlt);
//
//	public PlayerStat getPlayerRecord(String player) ;
//	public PlayerStat getPlayerRecord(OfflinePlayer player) ;
//
//	public TeamStat getTeamRecord(String teamName);
//	public TeamStat getTeamRecord(Set<String> players);
//
//	public boolean hidePlayer(String player, boolean hide);
//
//	/**
//	 * Stop tracking stats
//	 * @param playerName
//	 */
//	public void stopTracking(String playerName);
//
//	/**
//	 * Resume tracking stats
//	 * @param playerName
//	 */
//	public void resumeTracking(String playerName);
//
//	/**
//	 * Stop displaying kill messages for this player
//	 * @param playerName
//	 */
//	public void stopMessages(String playerName);
//	/**
//	 * Resume displaying kill messages for this player
//	 * @param playerName
//	 */
//	public void resumeMessages(String playerName);
//
//	/**
//	 * Stop tracking stats
//	 * @param player
//	 */
//	public void stopTracking(OfflinePlayer player);
//	/**
//	 * Resume tracking stats
//	 * @param player
//	 */
//	public void resumeTracking(OfflinePlayer player);
//
//	/**
//	 * Stop displaying kill messages for this player
//	 * @param player
//	 */
//	public void stopMessages(OfflinePlayer player);
//	/**
//	 * Resume displaying kill messages for this player
//	 * @param player
//	 */
//	public void resumeMessages(OfflinePlayer player);
//
//	public void resumeMessages(Collection<Player> players);
//	public void resumeTracking(Collection<Player> players);
//	public void stopMessages(Collection<Player> players);
//	public void stopTracking(Collection<Player> players);
//
//	public void addRecordGroup(Collection<Player> team1, Collection<Collection<Player>> teams, WLT wlt);
//
//	public Stat getRecord(String player);
//
//	public Stat getRecord(OfflinePlayer player);
//	public Stat loadRecord(OfflinePlayer op);
//	public Stat loadPlayerRecord(String name);
//	public Stat loadRecord(Set<Player> players);
//
//	public Stat getRecord(Collection<Player> players);
//
//	public void saveAll();
//
//	public List<Stat> getTopX(StatType statType, int x);
//
//	/**
//	 * Returns the top x players in rating
//	 */
//	public List<Stat> getTopXRating(int x);
//	/**
//	 * Returns the top x players in losses
//	 */
//	public List<Stat> getTopXLosses(int x);
//	/**
//	 * Returns the top x players in wins
//	 */
//	public List<Stat> getTopXWins(int x);
//	/**
//	 * Returns the top x players in k/d ratio
//	 */
//	public List<Stat> getTopXKDRatio(int x);
//
//	public List<Stat> getTopX(StatType statType, int x, Integer teamSize);
//	public List<Stat> getTopXRating(int x, Integer teamSize);
//	public List<Stat> getTopXLosses(int x, Integer teamSize);
//	public List<Stat> getTopXWins(int x, Integer teamSize);
//	public List<Stat> getTopXKDRatio(int x, Integer teamSize);
//
//	/**
//	 * Get the rank of this player
//	 * @param sender
//	 * @return
//	 */
//	public Integer getRank(OfflinePlayer sender);
//
//	/**
//	 * Get the rank of this player/team
//	 * @param name
//	 * @return
//	 */
//	public Integer getRank(String name);
//
//	/**
//	 * reset the stats for this interface
//	 */
//	public void resetStats();
//
//	/**
//	 * Dont Track Individual records or Versus Recrods, Just the overall tally
//	 * @param b
//	 */
//	public void onlyTrackOverallStats(boolean b);
//
//
//	/**
//	 * Set the rating of the given player
//	 * @param player
//	 * @param rating
//	 * @return
//	 */
//	public boolean setRating(OfflinePlayer player, int rating);
//
//	/**
//	 * Get the rating calculator being used(default EloCalculator)
//	 * @return
//	 */
//	public RatingCalculator getRatingCalculator();
//
//	/**
//	 * Get the records of player1 vs player2
//	 * @param playerName
//	 * @param playerName2
//	 * @param x : the number of records to return, -1 for all
//	 * @return
//	 */
//	public List<WLTRecord> getVersusRecords(String playerName, String playerName2, int x);
//
//
//	/**
//	 * What is the name of this interface
//	 * @return
//	 */
//	public String getInterfaceName();
//
//	/**
//	 * Immediately save these stats (they usually get saved at an appropriate time)
//	 * @param stats
//	 */
//	public void save(Stat... stats);
//
//	/**
//	 * Immediately save all records (they usually get saved at an appropriate time)
//	 * and empty the cache
//	 */
//	public void flush();
//
//	/**
//	 * Get how many records are in this interface
//	 * @return
//	 */
//	public int getRecordCount();
//
////	/**
////	 * Returns the Version of BattleTracker
////	 * @return
////	 */
////	public Version getVersion();
//
//	public List<WLTRecord> getWinsSince(Stat stat, Long time);
//
//	/**
//	 * Return whethe the database has changed
//	 * This is not guaranteed to be 100% correct if data is being
//	 * changed from outside the TrackerInterface
//	 * @return
//	 */
//	boolean isModified();
//}
