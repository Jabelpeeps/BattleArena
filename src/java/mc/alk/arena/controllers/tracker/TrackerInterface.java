package mc.alk.arena.controllers.tracker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import lombok.Getter;
import mc.alk.arena.Defaults;
import mc.alk.arena.controllers.Scheduler;
import mc.alk.arena.objects.tracker.PlayerStat;
import mc.alk.arena.objects.tracker.Stat;
import mc.alk.arena.objects.tracker.StatChange;
import mc.alk.arena.objects.tracker.StatType;
import mc.alk.arena.objects.tracker.TeamStat;
import mc.alk.arena.objects.tracker.WLTRecord;
import mc.alk.arena.objects.tracker.WLTRecord.WLT;
import mc.alk.arena.serializers.tracker.SQLInstance;
import mc.alk.arena.serializers.tracker.SQLSerializer;
import mc.alk.arena.tracker.EloCalculator;
import mc.alk.arena.util.Cache;
import mc.alk.arena.util.Cache.CacheSerializer;

public class TrackerInterface implements CacheSerializer<String, Stat>{
	Cache<String, Stat> cache = new Cache<>(this);
	boolean trackIndividual = false;
	@Getter EloCalculator ratingCalculator;
	@Getter SQLInstance SQL = null;
	@Getter String interfaceName;

	public TrackerInterface( String tableName, boolean saveIndividualRecords ) {
	      
        SQLSerializer serial = new SQLSerializer();
        serial.configureSQL( TrackerConfigController.config.getConfigurationSection( "SQLOptions" ) );
        SQL = new SQLInstance( tableName, serial );
        interfaceName = tableName;     
        cache.setSaveEvery( Defaults.SAVE_EVERY_X_SECONDS *1000 );
        
		ratingCalculator = new EloCalculator();
		ratingCalculator.setDefaultRating((float) TrackerConfigController.getDouble( "elo.default", 1250 ) );
		ratingCalculator.setEloSpread((float) TrackerConfigController.getDouble( "elo.spread", 400 ) );
		trackIndividual = saveIndividualRecords;
	}

	@Override
    public Stat load(String id, MutableBoolean dirty, Object... varArgs) {
		Stat stat = SQL.getRecord(id);
		if ( Defaults.DEBUG_TRACKING ) System.out.println( " sql returning " + stat );
		if ( stat != null ) {
			stat.setCache( cache );
			dirty.setValue(false);
		} 
		else if ( varArgs.length != 0 ) {
			dirty.setValue(true);
			stat = (Stat) varArgs[0];
			if ( Defaults.DEBUG_TRACKING ) System.out.println( " returning premade " + stat );
			stat.setCache(cache);
			stat.setRating(ratingCalculator.getDefaultRating());
		}
		if ( stat != null )
			stat.setParent(this);
		return stat;
	}

	@Override
    public void save(List<Stat> stats) {
		SQL.saveAll(stats.toArray(new Stat[stats.size()]));
	}

	public void save( Stat... stats ) { SQL.saveAll( stats ); }

	private Stat getRecord(Stat pStat){
		Stat stat = cache.get( pStat.getStrID(), pStat );
		if ( stat == null ) {
			stat = pStat;
			stat.setCache(cache);
			stat.setRating(ratingCalculator.getDefaultRating());
			cache.put(pStat);
		}
		stat.setParent(this);
		return stat;
	}

    public void addStatRecord( Stat team1, Stat team2, WLT wlt ){
		addStatRecord(team1,team2,wlt,true);
	}

	private void addStatRecord( Stat team1, Stat team2, WLT wlt, boolean changeWinLossRecords) {
	    
		if ( cache.contains( team1 ) && cache.contains( team2 ) ) {
			_addStatRecord( new StatChange( team1, team2, wlt, changeWinLossRecords ) );
		} 
		else {
			Scheduler.scheduleAsynchronousTask( 
			        () -> _addStatRecord( new StatChange( team1, team2, wlt, changeWinLossRecords ) ) );
		}
	}

	void _addStatRecord(StatChange sc){
		Stat team1 = sc.getTeam1();
		Stat team2 = sc.getTeam2();
		WLT wlt = sc.getWlt();
		boolean changeWinLossRecords = sc.isChangeWinLossRecords();
		/// Get our records
		Stat ts1 = getRecord(team1);
		Stat ts2 = getRecord(team2);

		if (Defaults.DEBUG_ADD_RECORDS) System.out.println("BT Debug: addStatRecord:sql="+SQL + "  ts1 = " + ts1 +"    " + ts2);

		/// Change win loss record
		if (changeWinLossRecords){
			ts1.setSaveIndividual(trackIndividual);
			ts2.setSaveIndividual(trackIndividual);
			switch(wlt){
			case WIN:
				ts1.win(ts2); ts2.loss(ts1);
				break;
			case LOSS:
				ts1.loss(ts2); ts2.win(ts1);
				break;
			case TIE:
				ts1.tie(ts2); ts2.tie(ts1);
				break;
			default:
			}
		}
		/// Change the elo
		switch(wlt){
    		case WIN:
    			ratingCalculator.changeRatings(ts1,ts2,false);
    			break;
    		case LOSS:
    			ratingCalculator.changeRatings(ts2,ts1,false);
    			break;
    		case TIE:
    			ratingCalculator.changeRatings(ts1,ts2,true);
    			break;
    		default:
		}
	}

    public void addPlayerRecord(String p1, String p2, WLT wlt) {
		Stat ts1 = new PlayerStat(p1);
		Stat ts2 = new PlayerStat(p2);
		addStatRecord(ts1, ts2, wlt);
	}

    public void changePlayerElo(String p1, String p2, WLT wlt) {
		Stat ts1 = new PlayerStat(p1);
		Stat ts2 = new PlayerStat(p2);
		addStatRecord(ts1, ts2, wlt,false);
	}

    public void addPlayerRecord(OfflinePlayer p1, OfflinePlayer p2, WLT wlt) {
		addPlayerRecord(p1.getName(), p2.getName(),wlt);
	}

    public void addTeamRecord(String t1, String t2, WLT wlt) {
		TeamStat ts1 = new TeamStat(t1,false);
		TeamStat ts2 = new TeamStat(t2,false);
		addStatRecord(ts1, ts2,wlt);
	}

    public void addTeamRecord(Set<String> team1, Set<String> team2, WLT wlt) {
		TeamStat ts1 = new TeamStat(team1);
		TeamStat ts2 = new TeamStat(team2);
		addStatRecord(ts1, ts2,wlt);
	}

    public void addTeamRecord(Collection<Player> team1, Collection<Player> team2, WLT wlt) {
		HashSet<String> names = new HashSet<>();
		for (OfflinePlayer p : team1){
			names.add(p.getName());}
		TeamStat ts1 = new TeamStat(names);
		names = new HashSet<>();
		for (OfflinePlayer p : team2){
			names.add(p.getName());}
		TeamStat ts2 = new TeamStat(names);
		addStatRecord(ts1, ts2,wlt);
	}

    public TeamStat getTeamRecord(String teamName) {
		return (TeamStat) cache.get(teamName);
	}

    public TeamStat getTeamRecord(Set<String> players) {
		TeamStat ts = new TeamStat(players);
		return (TeamStat) cache.get(ts.getKey());
	}

    public PlayerStat getPlayerRecord(String player) {
		Stat ts = cache.get(player);
		if (ts instanceof PlayerStat)
			return (PlayerStat) ts;
		return null;
	}

    public PlayerStat getPlayerRecord(OfflinePlayer player) {
		return (PlayerStat) cache.get(player.getName());
	}

    public void addRecordGroup(Collection<Player> team1, Collection<Collection<Player>> teams, WLT wlt) {
		TeamStat ts = new TeamStat(toStringCollection(team1));
		Stat ts1 = cache.get(ts,ts);
		ts1.incWins();
		Collection<Stat> lstats = new ArrayList<>();
		for (Collection<Player> t : teams){
			TeamStat loser = new TeamStat(toStringCollection(t));
			Stat lstat = cache.get(loser,loser);
			if (lstat == null){
				cache.put(loser);
				lstat = loser;
			}
			lstat.incLosses();
			lstats.add(lstat);
		}
		ratingCalculator.changeRatings(ts1, lstats, false);
	}

	private Set<String> toStringCollection(Collection<Player> players) {
		Set<String> col = new HashSet<>();
		for (Player p: players){
			col.add(p.getName());
		}
		return col;
	}
    public Stat getRecord( String player ) { return cache.get( player ); }
    public Stat getRecord( OfflinePlayer player ) { return cache.get( player.getName() ); }
    public Stat loadRecord( OfflinePlayer op ) { return loadStat( new PlayerStat( op ) ); }
    public Stat loadPlayerRecord( String name ) { return loadStat( new PlayerStat( name ) ); }
    public int getRecordCount() { return SQL.getRecordCount(); }
    public boolean isModified() { return cache.isModified(); }

    public Stat loadRecord(Set<Player> players) {
		HashSet<String> names = new HashSet<>();
		for ( OfflinePlayer p : players ) {
			names.add( p.getName() );
		}
		return loadStat( new TeamStat( names ) );
	}
	private Stat loadStat( Stat stat ) {
		Stat s = cache.get(stat, stat);
		if ( s == null ) {
			cache.put(stat);
			return stat;
		}
		return s;
	}

    public Stat getRecord( Collection<Player> players ) {
		HashSet<String> names = new HashSet<>();
		for ( OfflinePlayer p : players ) {
			names.add( p.getName() );
		}
		return cache.get( new TeamStat( names ) );
	}

    public void saveAll() { cache.save(); }

 	public List<Stat> getTopXRating(int x) { return getTopX( StatType.RATING, x, null ); }
    public List<Stat> getTopXWins(int x) { return getTopX( StatType.WINS, x, null ); }
    public List<Stat> getTopX(StatType statType, int x) { return getTopX( statType, x, 1 ); }
    private List<Stat> getTopXRanking(int x, Integer teamsize) { return getTopX( StatType.RANKING, x, teamsize ); }


    private List<Stat> getTopX(StatType statType, int x, Integer teamsize) {
		cache.save();
		return SQL.getTopX(statType, x, teamsize);
	}

    public void resetStats() {
		cache.clear();
		SQL.deleteTables();
	}

	/**
	 * write out all dirty records.  and empty the cache
	 */
    public void flush() { cache.flush(); }

    public void onlyTrackOverallStats( boolean b ) { trackIndividual = !b; }

    public boolean setRating(OfflinePlayer player, int rating){
		Stat stat = cache.get(new PlayerStat(player));
		if (stat == null)
			return false;
		stat.setRating(rating);
		return true;

	}

	public List<WLTRecord> getVersusRecords(String name, String name2) {
		return getVersusRecords( name, name2, 10 );
	}

    public List<WLTRecord> getVersusRecords(String name, String name2, int x) {
		cache.save();
		return SQL.getVersusRecords(name,name2,x);
	}
	public List<WLTRecord> getWinsSince(Stat stat, Long time) {
		cache.save();
		return SQL.getWinsSince(stat.getName(),time);
	}

    public void printTopX( CommandSender sender, StatType statType, int x ) {
		printTopX( sender, statType, x, 0, Defaults.MSG_TOP_HEADER, Defaults.MSG_TOP_BODY );
	}

    public void printTopX( CommandSender sender, StatType statType, int x, String headerMsg, String bodyMsg ) {
		printTopX( sender, statType, x, 0, headerMsg, bodyMsg );
	}

    public void printTopX( CommandSender sender, StatType statType, int x, int teamSize ) {
		printTopX( sender, statType, x, teamSize, Defaults.MSG_TOP_HEADER, Defaults.MSG_TOP_BODY );
	}

	public void printTopX( CommandSender sender, StatType statType, int x, int teamSize, String headerMsg, String bodyMsg ) {
		if (x <= 0 )
			x = Integer.MAX_VALUE;
		cache.save();
		List<Stat> teamstats = getTopXRanking(x, teamSize);
		if (teamstats == null){
			TrackerMessageController.sendMessage(
			        sender,ChatColor.YELLOW + "The top " + statType.getName() + " can not be found");
			return;
		}
		/// Header Message
		String msg = headerMsg;
		msg = msg.replaceAll("\\{interfaceName\\}", getInterfaceName());
		msg = msg.replaceAll("\\{teamSize\\}", teamSize +"");
		msg = msg.replaceAll("\\{stat\\}", statType.getName());
		msg = msg.replaceAll("\\{x\\}", x +"");

		TrackerMessageController.sendMessage(sender,msg);

		/// Send the Body Messages
		Map<StatType,Pattern> patterns = new HashMap<>(StatType.values().length);
		for (StatType st: StatType.values()){
			patterns.put(st,Pattern.compile("\\{"+st.name().toLowerCase()+"\\}"));
		}

		final int max = Math.min(x,teamstats.size());
		for (int i=0;i<max;i++){
			msg = bodyMsg;
			Stat stat = teamstats.get(i);
			for (StatType st : patterns.keySet()){
				Matcher m = patterns.get(st).matcher(msg);
				if (!m.find())
					continue;
				switch (st){
    				case WINS: case KILLS: msg = m.replaceAll(stat.getWins()+""); break;
    				case LOSSES: case DEATHS: msg = m.replaceAll(stat.getLosses()+""); break;
    				case TIES: msg = m.replaceAll(stat.getTies()+""); break;
    				case RANKING:
    				case RATING: msg = m.replaceAll(stat.getRating()+""); break;
    				case MAXRANKING:
    				case MAXRATING: msg = m.replaceAll(stat.getMaxRating()+""); break;
    				case STREAK: msg = m.replaceAll(stat.getStreak()+""); break;
    				case MAXSTREAK: msg = m.replaceAll(stat.getMaxStreak()+""); break;
    				case WLRATIO: msg = m.replaceAll(stat.getKDRatio()+""); break;
    				default:
    					break;
				}
			}
			msg = msg.replaceAll("\\{rank\\}", i+1 +"");
			msg = msg.replaceAll("\\{name\\}", stat.getName());
			TrackerMessageController.sendMessage(sender,msg);
		}

	}

	public Integer getRank(OfflinePlayer sender) {
		cache.save();
		Stat s = getPlayerRecord(sender);
		if ( s == null ) return null;
		return SQL.getRanking( (int) s.getRating(),s.getCount() );
	}

	public Integer getRank(String team) {
		cache.save();
		Stat s = getRecord(team);
		if ( s == null ) return null;
		return SQL.getRanking( (int) s.getRating(),s.getCount() );
	}

	public boolean hidePlayer(String player, boolean hide) {
		Stat s = getPlayerRecord(player);
		if ( s == null ) return false;
		s.hide(hide);
		cache.flush();
		return true;
	}

    public String getRankMessage(OfflinePlayer player) {
        Stat stat = loadRecord(player);
        if (stat == null){
            return "&eCouldn't find stats for player " + player.getName();}
        Integer rank = getRank(player.getName());
        if (rank == null)
            rank = -1;
        return "&eRank:&6" + rank + "&e (&4" + stat.getWins() + "&e:&8" + stat.getLosses() + "&e)&6[" + stat.getRating() + 
                        "]&e" + ". Highest &6[" + stat.getMaxRating() + "]&e Longest Streak &b" + stat.getMaxStreak();
    }

    @Override
    public String toString(){
        return new StringBuilder( "[TI=" ).append( SQL != null ? SQL.getTableName() 
                                                               : "null" )
                                          .append( "]" )
                                          .toString();
    }
}
