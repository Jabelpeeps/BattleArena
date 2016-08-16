package mc.alk.tracker.serializers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import mc.alk.tracker.objects.PlayerStat;
import mc.alk.tracker.objects.Stat;
import mc.alk.tracker.objects.StatType;
import mc.alk.tracker.objects.TeamStat;
import mc.alk.tracker.objects.VersusRecords;
import mc.alk.tracker.objects.VersusRecords.VersusRecord;
import mc.alk.tracker.objects.WLTRecord;
import mc.alk.tracker.objects.WLTRecord.WLT;
import mc.alk.util.Log;


public class SQLInstance extends SQLSerializer {
	public static final int TEAM_ID_LENGTH = 32;
	public static final int TEAM_NAME_LENGTH = 48;
	static public String URL = "localhost";
	static public String PORT = "3306";
	static public String USERNAME = "root";
	static public String PASSWORD = "";

	public String DB = "BattleTracker";
	public static final String TABLE_PREFIX = "bt_";
	public final String VERSUS_TABLE_SUFFIX = "_versus";
	public final String OVERALL_TABLE_SUFFIX = "_overall";
	public final String INDIVIDUAL_TABLE_SUFFIX = "_tally";
	public String OVERALL_TABLE, VERSUS_TABLE, INDIVIDUAL_TABLE;
	public static final String MEMBER_TABLE = TABLE_PREFIX + "members";

	static final public String NAME = "Name";
	static final public String TEAMID = "ID";
	static final public String ID1 = "ID1";
	static final public String ID2 = "ID2";
    static final public String WINS= "Wins";
    static final public String LOSSES = "Losses";
//    static final public String KILLS= "Kills";
//    static final public String DEATHS = "Deaths";
	static final public String WLTIE = "WLTIE";
	static final public String TIES = "Ties";
	static final public String STREAK = "Streak";
	static final public String MAXSTREAK = "maxStreak";
	static final public String ELO = "Elo";
	static final public String MAXELO = "maxElo";
	static final public String COUNT = "Count";
	static final public String DATE = "Date";


	static final public String RANK = "Rank";
	static final public String RANK_TYPE = "RankType";
	static final public String TX_KILLS = "TK";
	static final public String TX_STREAK = "Streak";
	static final public String TX_KD = "KD";
	static final public String TX_ELO = "ELO";
	static final public String VALUE = "Value";
	static final public String MEMBERS = "Members";
	static final public String FLAGS = "Flags";

	String drop_tables;

	String create_individual_table, create_versus_table, create_member_table,create_overall_table;
	String create_individual_table_idx, create_versus_table_idx, create_member_table_idx,create_overall_table_idx;

	String get_overall_totals, insert_overall_totals;
	String get_topx_wins, get_topx_losses, get_topx_ties;
	String get_topx_streak,get_topx_maxstreak;
	String get_topx_kd, get_topx_elo, get_topx_maxelo;
	String get_topx_wins_tc, get_topx_losses_tc, get_topx_ties_tc;
	String get_topx_streak_tc,get_topx_maxstreak_tc;
	String get_topx_kd_tc, get_topx_elo_tc, get_topx_maxelo_tc;
	String save_ind_record, get_ind_record;
	String insert_versus_record, get_versus_record;
	String get_versus_records, getx_versus_records, get_wins_since;
	String truncate_all_tables;
	String get_rank;

	public static final String get_members = "select "+NAME+" from " + MEMBER_TABLE + " where " + TEAMID +" = ?";
	String save_members;

	String tableName;

	public SQLInstance(){}

	public void setTable(String tableName) {
		this.tableName = tableName;
	}
	public String getTable(){
		return tableName;
	}

	@Override
	public boolean init(){
		super.init();
		VERSUS_TABLE = TABLE_PREFIX + tableName + VERSUS_TABLE_SUFFIX;
		OVERALL_TABLE = TABLE_PREFIX + tableName + OVERALL_TABLE_SUFFIX;
		INDIVIDUAL_TABLE = TABLE_PREFIX + tableName + INDIVIDUAL_TABLE_SUFFIX;

		create_overall_table = "CREATE TABLE IF NOT EXISTS " + OVERALL_TABLE +" ("+
				TEAMID + " VARCHAR(" + TEAM_ID_LENGTH +") NOT NULL ,"+
				NAME + " VARCHAR(" + TEAM_NAME_LENGTH +") ,"+
                WINS + " INTEGER UNSIGNED ," +
                LOSSES + " INTEGER UNSIGNED," +
//                KILLS + " INTEGER UNSIGNED ," +
//                DEATHS + " INTEGER UNSIGNED," +
				TIES + " INTEGER UNSIGNED," +
				STREAK + " INTEGER UNSIGNED," +
				MAXSTREAK + " INTEGER UNSIGNED," +
				ELO + " INTEGER UNSIGNED DEFAULT " + 1250+"," +
				MAXELO + " INTEGER UNSIGNED DEFAULT " + 1250+"," +
				COUNT + " INTEGER UNSIGNED DEFAULT 1," +
				FLAGS + " INTEGER UNSIGNED DEFAULT 0," +
				"PRIMARY KEY (" + TEAMID +")) ";

		create_versus_table = "CREATE TABLE IF NOT EXISTS " + VERSUS_TABLE +" ("+
				ID1 + " VARCHAR(" + TEAM_ID_LENGTH +") NOT NULL ,"+
				ID2 + " VARCHAR(" + TEAM_ID_LENGTH +") NOT NULL ,"+
				WINS + " INTEGER UNSIGNED ," +
				LOSSES + " INTEGER UNSIGNED," +
				TIES + " INTEGER UNSIGNED," +
				"PRIMARY KEY ("+ID1 +", "+ID2+"))";

		create_member_table = "CREATE TABLE IF NOT EXISTS " + MEMBER_TABLE +" ("+
				TEAMID + " VARCHAR(" + TEAM_ID_LENGTH +") NOT NULL ,"+
				NAME + " VARCHAR(" + MAX_NAME_LENGTH +") NOT NULL ," +
				"PRIMARY KEY (" + TEAMID +","+NAME+"))";

		get_topx_wins = "select * from "+OVERALL_TABLE +" WHERE "+FLAGS+" & 1 <> 1 ORDER BY "+WINS+" DESC LIMIT ? ";
		get_topx_losses = "select * from "+OVERALL_TABLE +" WHERE "+FLAGS+" & 1 <> 1 ORDER BY "+LOSSES+" DESC LIMIT ? ";
		get_topx_ties = "select * from "+OVERALL_TABLE +" WHERE "+FLAGS+" & 1 <> 1 ORDER BY "+TIES+" DESC LIMIT ? ";
		get_topx_streak = "select * from "+OVERALL_TABLE +" WHERE "+FLAGS+" & 1 <> 1 ORDER BY "+STREAK +" DESC LIMIT ? ";
		get_topx_maxstreak = "select * from "+OVERALL_TABLE +" WHERE "+FLAGS+" & 1 <> 1 ORDER BY "+MAXSTREAK +" DESC LIMIT ? ";
		get_topx_elo = "select * from "+OVERALL_TABLE +" WHERE "+FLAGS+" & 1 <> 1 ORDER BY "+ELO+" DESC LIMIT ? ";
		get_topx_maxelo = "select * from "+OVERALL_TABLE +" WHERE "+FLAGS+" & 1 <> 1 ORDER BY "+MAXELO+" DESC LIMIT ? ";
		get_topx_kd = "select *,(" + WINS + "/" + LOSSES+") as KD from "+OVERALL_TABLE +" WHERE "+FLAGS+" & 1 <> 1 ORDER BY KD DESC LIMIT ? ";

		get_topx_wins_tc = "select * from "+OVERALL_TABLE +" WHERE "+COUNT+"=? AND "+FLAGS+" & 1 <> 1 ORDER BY "+WINS+" DESC LIMIT ? ";
		get_topx_losses_tc = "select * from "+OVERALL_TABLE +" WHERE "+COUNT+"=? AND "+FLAGS+" & 1 <> 1 ORDER BY "+LOSSES+" DESC LIMIT ? ";
		get_topx_ties_tc = "select * from "+OVERALL_TABLE +" WHERE "+COUNT+"=? AND "+FLAGS+" & 1 <> 1 ORDER BY "+TIES+" DESC LIMIT ? ";
		get_topx_streak_tc = "select * from "+OVERALL_TABLE +" WHERE "+COUNT+"=? AND "+FLAGS+" & 1 <> 1 ORDER BY "+STREAK +" DESC LIMIT ? ";
		get_topx_maxstreak_tc = "select * from "+OVERALL_TABLE +" WHERE "+COUNT+"=? AND "+FLAGS+" & 1 <> 1 ORDER BY "+MAXSTREAK +" DESC LIMIT ? ";
		get_topx_elo_tc = "select * from "+OVERALL_TABLE +" WHERE "+COUNT+"=? AND "+FLAGS+" & 1 <> 1 ORDER BY "+ELO+" DESC LIMIT ? ";
		get_topx_maxelo_tc = "select * from "+OVERALL_TABLE +" WHERE "+COUNT+"=? AND "+FLAGS+" & 1 <> 1 ORDER BY "+MAXELO+" DESC LIMIT ? ";
		get_topx_kd_tc = "select *,(" + WINS + "/" + LOSSES+") as KD from "+OVERALL_TABLE +" WHERE "+COUNT+"=? AND "+FLAGS+" & 1 <> 1 ORDER BY KD DESC LIMIT ? ";

		get_overall_totals = "select * from " + OVERALL_TABLE + " where " + TEAMID +" = ?";

		get_versus_record = "select "+WINS+","+LOSSES+","+TIES+" from "+VERSUS_TABLE+" WHERE "+ID1+"=? AND "+ID2+"=?";

		getx_versus_records = "select * from "+INDIVIDUAL_TABLE+" WHERE ("+ID1+"=? AND "+ID2+"=?) OR ("+ID1+"=? AND "+ID2+"=?) ORDER BY "+DATE+" DESC LIMIT ?";


		get_rank = "select  count(*) from "+OVERALL_TABLE+" where "+ELO+" > ? and "+COUNT+"=?";

		get_wins_since = "select * from "+INDIVIDUAL_TABLE+" WHERE ("+ID1+"=? AND WLTIE=1) OR ("+ID2+"=? AND WLTIE=0) AND "+DATE+" >= ? ORDER BY "+DATE+" DESC ";

		switch(type){
		case MYSQL:
			create_individual_table = "CREATE TABLE IF NOT EXISTS " + INDIVIDUAL_TABLE +" ("+
					ID1 + " VARCHAR(" + TEAM_ID_LENGTH +") NOT NULL ,"+
					ID2 + " VARCHAR(" + TEAM_ID_LENGTH +") NOT NULL ,"+
					DATE + " DATETIME," +
					WLTIE + " INTEGER UNSIGNED," +
					"PRIMARY KEY (" + ID1 +", " + ID2 + "," + DATE + "), "+
					"INDEX USING HASH (" + ID1 +"),INDEX USING BTREE (" + DATE +")) ";

			create_member_table_idx = "CREATE INDEX "+MEMBER_TABLE+"_idx ON " +MEMBER_TABLE+" ("+TEAMID+") USING HASH";
			create_versus_table_idx = "CREATE INDEX "+VERSUS_TABLE+"_idx ON " +VERSUS_TABLE+" ("+ID1+") USING HASH";

			insert_overall_totals = "INSERT INTO "+OVERALL_TABLE+" VALUES (?,?,?,?,?,?,?,?,?,?,?) " +
					"ON DUPLICATE KEY UPDATE " +
					WINS + " = VALUES(" + WINS +"), " + LOSSES +"=VALUES(" + LOSSES + "), " + TIES +"=VALUES(" + TIES + "), " +
					STREAK +"= VALUES(" + STREAK+")," +MAXSTREAK +"= VALUES(" + MAXSTREAK+")," +
					ELO +"= VALUES(" + ELO + ")," +  MAXELO +"= VALUES(" + MAXELO+"),"+
					FLAGS+"=VALUES("+FLAGS+")";

			insert_versus_record = "insert into "+VERSUS_TABLE+" VALUES(?,?,?,?,?) " +
					"ON DUPLICATE KEY UPDATE " +
					WINS + " = VALUES(" + WINS +"), " + LOSSES +"=VALUES(" + LOSSES + "), " + TIES +"=VALUES(" + TIES + ")";

			save_ind_record = "insert ignore into "+INDIVIDUAL_TABLE+" VALUES(?,?,?,?)";
			save_members = "insert ignore into " + MEMBER_TABLE + " VALUES(?,?) ";
			truncate_all_tables = "truncate table " +OVERALL_TABLE+"; truncate table " + VERSUS_TABLE+"; truncate table "+INDIVIDUAL_TABLE;

			break;
		case SQLITE:
			create_individual_table = "CREATE TABLE IF NOT EXISTS " + INDIVIDUAL_TABLE +" ("+
					ID1 + " VARCHAR(" + TEAM_ID_LENGTH +") NOT NULL ,"+
					ID2 + " VARCHAR(" + TEAM_ID_LENGTH +") NOT NULL ,"+
					DATE + " DATETIME," +
					WLTIE + " INTEGER UNSIGNED," +
					"PRIMARY KEY (" + ID1 +", " + ID2 + "," + DATE + ")) ";

			create_member_table_idx = "CREATE UNIQUE INDEX IF NOT EXISTS "+MEMBER_TABLE+"_idx ON " +MEMBER_TABLE+" ("+TEAMID+")";
			create_versus_table_idx = "CREATE UNIQUE INDEX IF NOT EXISTS "+VERSUS_TABLE+"_idx ON " +VERSUS_TABLE+" ("+ID1+")";

			insert_versus_record = "insert or replace into "+VERSUS_TABLE+" VALUES(?,?,?,?,?)";

			save_ind_record = "insert or ignore into "+INDIVIDUAL_TABLE+" VALUES(?,?,?,?)";

			insert_overall_totals = "INSERT OR REPLACE INTO "+OVERALL_TABLE+" VALUES (?,?,?,?,?,?,?,?,?,?,?) ";

			//			insert_overall_totals = "INSERT OR REPLACE INTO "+OVERALL_TABLE+" VALUES (?," +
			//					"(select "+NAME+" from "+OVERALL_TABLE+" where "+TEAMID+"=?),"+
			//					"?,?,? ,?,? ,?,?,"+
			//					"(select "+COUNT+" from "+OVERALL_TABLE+" where "+TEAMID+"=?))";

			save_members = "insert or ignore into " + MEMBER_TABLE + " VALUES(?,?) ";
			truncate_all_tables = "drop table " +OVERALL_TABLE+"; drop table " + VERSUS_TABLE+"; drop table "+INDIVIDUAL_TABLE;
		}
        if (shouldUpdateTo1point0()){
            updateTo1Point0();}
//        if (shouldUpdateTo1point1()){
//            updateTo1Point1();}
		try {
			createTable(VERSUS_TABLE, create_versus_table, create_versus_table_idx);

			createTable(OVERALL_TABLE, create_overall_table );
			createTable(INDIVIDUAL_TABLE,create_individual_table,create_individual_table_idx);
			createTable(MEMBER_TABLE,create_member_table,create_member_table_idx);
		} catch (Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public List<Stat> getTopX(StatType statType, int x, Integer teamcount) {
		if (x <= 0){
			x = Integer.MAX_VALUE;}
		RSCon rscon = null;
		if (teamcount == null){
			switch(statType){
			case WINS: case KILLS: rscon = executeQuery(get_topx_wins,x);break;
			case LOSSES: case DEATHS: rscon = executeQuery(get_topx_losses,x); break;
			case TIES: rscon = executeQuery(get_topx_ties,x); break;
			case RATING: case RANKING: rscon = executeQuery(get_topx_elo,x); break;
			case MAXRATING: case MAXRANKING: rscon = executeQuery(get_topx_maxelo,x); break;
			case STREAK: rscon = executeQuery(get_topx_streak,x); break;
			case MAXSTREAK: rscon = executeQuery(get_topx_maxstreak,x); break;
			case WLRATIO: case KDRATIO: rscon = executeQuery(get_topx_kd,x); break;
			default:
			}
		} else {
			switch(statType){
			case WINS: case KILLS: rscon = executeQuery(get_topx_wins_tc,teamcount,x);break;
			case LOSSES: case DEATHS: rscon = executeQuery(get_topx_losses_tc,teamcount,x); break;
			case TIES: rscon = executeQuery(get_topx_ties_tc,teamcount,x); break;
			case RATING: case RANKING: rscon = executeQuery(get_topx_elo_tc,teamcount,x); break;
			case MAXRATING: case MAXRANKING: rscon = executeQuery(get_topx_maxelo_tc,teamcount,x); break;
			case STREAK: rscon = executeQuery(get_topx_streak_tc,teamcount,x); break;
			case MAXSTREAK: rscon = executeQuery(get_topx_maxstreak_tc,teamcount,x); break;
			case WLRATIO: case KDRATIO: rscon = executeQuery(get_topx_kd_tc,teamcount,x); break;
			default:
			}
		}

		return rscon == null ? null : createStatList(rscon);
	}

	public Integer getRanking(int rating, int teamSize) {
		Integer rank = getInteger(get_rank, rating, teamSize);
		return rank != null ? rank + 1: null;
	}

	private List<Stat> createStatList(RSCon rscon){
		List<Stat> stats = new ArrayList<>();
		if (rscon == null)
			return stats;
		try {
			ResultSet rs = rscon.rs;
			while (rs.next()){
				Stat s = createStat(rscon);
				if (s != null)
					stats.add(s);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeConnection(rscon);
		}
		return stats;
	}

	public Stat getRecord(String key) {
		RSCon rscon = executeQuery(get_overall_totals, key);
		try {
			ResultSet rs = rscon.rs;
			//			System.out.println("rscon & rs " + rscon +"  " + rs);
			while (rs.next()){
				return createStat(rscon);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeConnection(rscon);
		}
		return null;
	}

	private Stat createStat(RSCon rscon) throws SQLException{
		ResultSet rs = rscon.rs;
		Stat ts = null;
		String id = rs.getString(TEAMID);
		String name= rs.getString(NAME);
		int kills = rs.getInt(WINS);
		int deaths = rs.getInt(LOSSES);
		int streak = rs.getInt(STREAK);
		int maxStreak = rs.getInt(MAXSTREAK);
		int ties = rs.getInt(TIES);
		int elo = rs.getInt(ELO);
		int maxElo = rs.getInt(MAXELO);
		int count = rs.getInt(COUNT);
		int flags = rs.getInt(FLAGS);
		if (DEBUG) System.out.println("name =" + name + " id=" + id +" ranking=" + elo +" count="+count);
		if (count == 1){
			ts = new PlayerStat(id);
		} else {
			ts = new TeamStat(id,true);
			ts.setName(name);
		}
		Integer nid= null;
		try {nid = Integer.valueOf(id);} catch (NumberFormatException nfe){}
		if (nid != null && ts instanceof TeamStat){
			HashSet<String> players = new HashSet<>();
			RSCon rscon2 = null;
			try{
				rscon2 = executeQuery(rscon.con, true, TIMEOUT, get_members, id);
				ResultSet rs2 = rscon2.rs;
				while (rs2.next()){
					players.add(rs2.getString(NAME));
				}

				((TeamStat)ts).setMembers(players);
			} finally{
				closeConnection(rscon2);
			}
		} else {
		}
		ts.setWins(kills);
		ts.setLosses(deaths);
		ts.setStreak(streak);
		ts.setTies(ties);
		ts.setRating(elo);
		ts.setCount(count);
		ts.setMaxStreak(maxStreak);
		ts.setMaxRating(maxElo);
		ts.setFlags(flags);
		if (DEBUG) System.out.println("stat = " + ts);
		return ts;
	}
	public void save(Stat stat) {
		saveAll(stat);
	}

	public void saveAll(Stat... stats) {
		saveTotals(stats);
		/// Now Save members
		for (Stat stat: stats){
			try{
				/// We only need to save the members if they exceed the id length, which turns the id into a hash
				Integer nid = null;
				try {nid = Integer.valueOf(stat.getStrID());} catch (NumberFormatException e){}
				/// Save members
				List<String> members = stat.getMembers();
				if (nid != null && members != null && members.size() > 1)
					saveMembers(stat.getStrID(), members);

				VersusRecords rs = stat.getRecordSet();
				if (DEBUG) System.out.println("SaveVersusRecords " + rs);
				if (rs != null){
					rs.flushOverallRecords();
					//					saveOverallRecords(rs.getOverallRecords());
					//					rs.setOverallRecords(null);
					if (saveIndividualRecords(stat.getStrID(), rs.getIndividualRecords())){
						rs.setIndividualRecords(null);/// lets keep  the memory small where we can
					}
				}
			} catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	public boolean saveIndividualRecords(String id, HashMap<String, List<WLTRecord>> indRecords) {
		if (indRecords == null || indRecords.isEmpty())
			return true;
		if (DEBUG) System.out.println("SaveIndividual " + id);
		List<List<Object>> batch = new ArrayList<>();
		for (String oid : indRecords.keySet()){
			HashSet<Timestamp> times = new HashSet<>();

			for (WLTRecord wlt: indRecords.get(oid)){
				switch(wlt.wlt){
				case LOSS: /// do nothing, let the winner do the saving
					continue;
				case TIE: /// whoevers name is less stores the data
					if (id.compareTo(oid) > 0)
						continue;
				default:
					break;
				}
				Timestamp ts = new Timestamp((wlt.date /1000)*1000);
				while (times.contains(ts)){ /// Since mysql can only handle seconds, increment to first free second
					ts.setTime(ts.getTime()+1000);
				}
				times.add(ts);
				batch.add(Arrays.asList(new Object[]{id,oid,ts, wlt.wlt.ordinal()}));
			}
		}
		try {
			executeBatch(save_ind_record, batch);
		} catch (Exception e){
			return false;
		}
		return true;
	}

	private void saveTotals(Stat... stats){
		if (stats == null || stats.length==0)
			return;
		List<List<Object>> batch = new ArrayList<>();
		if (DEBUG) System.out.println("saveTotals ");

		for (Stat stat: stats){
			/// The "name" is just a comma delimited list of ids in the simple case, we can reconstruct it from the members
			String name= stat.getName();
			if (name!= null && name.length() > TEAM_NAME_LENGTH){
				name = null;}
			if (stat.getRating() < 0 || stat.getRating() > 200000){
				Log.err("ELO OUT OF RANGE " + stat.getRating() +"   stat=" + stat);
			}
			batch.add(Arrays.asList(new Object[]{stat.getStrID(),name, stat.getWins(), stat.getLosses(),stat.getTies(),
					stat.getStreak(),stat.getMaxStreak(), stat.getRating(),
					stat.getMaxRating(), stat.getCount(), stat.getFlags()}));
		}
		try{
			executeBatch(insert_overall_totals, batch);
		} catch (Exception e){
			System.err.println("ERROR SAVING TOTALS");
			e.printStackTrace();

			for (Stat stat: stats){
				Log.err(" Possible failed stat = " + stat);
			}
		}
	}

	public void saveMembers(String strid, List<String> players) {
		if (players == null)
			return ;
		if (DEBUG) Log.info( "SaveMember " + strid +"  players=" + players);
		List<List<Object>> batch = new ArrayList<>();
		for (String player: players){
			batch.add(Arrays.asList(new Object[]{strid,player}));
		}
		executeBatch(save_members,batch);
	}

	public VersusRecord getVersusRecord(String id, String opponentId) {
		VersusRecord or = null;
		List<Object> objs = getObjects(get_versus_record, id, opponentId);
		if (objs != null && !objs.isEmpty()){
			or = new VersusRecord(id,opponentId);
			or.wins = Integer.valueOf(objs.get(0).toString());
			or.losses = Integer.valueOf(objs.get(1).toString());
			or.ties = Integer.valueOf(objs.get(2).toString());
		}
		return or;
	}

	private WLTRecord parseWLTRecord(ResultSet rs) {
		try{
			Timestamp ts = rs.getTimestamp(DATE);
			WLTRecord wlt = new WLTRecord(WLT.valueOf(rs.getInt(WLTIE)), ts.getTime());
			return wlt;
		} catch (Exception e){
			e.printStackTrace();
			return null;
		}
	}


	public List<WLTRecord> getVersusRecords(String id, String opponentId, int x) {
		if (x <= 0){
			x = Integer.MAX_VALUE;}
		RSCon rscon = executeQuery(getx_versus_records,id,opponentId,opponentId, id, x);
		List<WLTRecord> list = new ArrayList<>();
		if (rscon != null){
			try {
				ResultSet rs = rscon.rs;
				String winner;
				while (rs.next()){
					winner = rs.getString(ID1);
					WLTRecord wlt = parseWLTRecord(rs);
					if (wlt == null)
						continue;
					if (winner.equalsIgnoreCase(opponentId)){
						wlt.reverse();
					}
					list.add(wlt);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} finally{
				closeConnection(rscon);
			}
		}
		return list;
	}

	public List<WLTRecord> getWinsSince(String id, Long time) {
		RSCon rscon = null;
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date(time);
		String datestr = dateFormat.format(date);
		rscon = executeQuery(get_wins_since,id,id, datestr);
		List<WLTRecord> list = new ArrayList<>();
		if (rscon != null){
			try {
				ResultSet rs = rscon.rs;
				while (rs.next()){
					WLTRecord wlt = parseWLTRecord(rs);
					if (wlt == null)
						continue;
					list.add(wlt);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} finally{
				closeConnection(rscon);
			}
		}
		return list;
	}

	public void realsaveVersusRecords(Collection<VersusRecord> types) {
		if (types==null) return;
		
		if (DEBUG) Log.info( "saveOverallRecords types=" + types +"  size=" +(types != null ? types.size():0));
		
		List<List<Object>> batch = new ArrayList<>();
		
		for ( VersusRecord or : types ){
			/// Whichever id is less stores the information to avoid redundancy
			/// Alkarin vs Yodeler. Alkarin stores the info
			if (or.ids.get(0).compareTo(or.ids.get(1)) > 0) continue;
			
			batch.add(Arrays.asList(new Object[]{or.ids.get(0),or.ids.get(1),or.wins,or.losses,or.ties}));
		}
		executeBatch(insert_versus_record,batch);
	}

	public void deleteTables(){
		switch (this.getType()){
		case MYSQL:
			this.executeUpdate("truncate table " +OVERALL_TABLE);
			this.executeUpdate("truncate table " +VERSUS_TABLE);
			this.executeUpdate("truncate table " +INDIVIDUAL_TABLE);
			break;
		case SQLITE:
			this.executeUpdate(truncate_all_tables);
			/// For SQLite, need to drop the tables and recreate them
			init();
			break;
		default:
			break;
		}
	}

	public int getRecordCount() {
		return getInteger("select count(*) from " + INDIVIDUAL_TABLE);
	}

	public boolean shouldUpdateTo1point0() {
		return hasTable(OVERALL_TABLE) && !hasColumn(OVERALL_TABLE,FLAGS);
	}
//    public boolean shouldUpdateTo1point1() {
//        return !hasColumn(OVERALL_TABLE,KILLS);
//    }

	private void updateTo1Point0() {
		Log.warn("[BattleTracker] updating database to 1.0");
		String alter = "ALTER TABLE "+OVERALL_TABLE+" ADD "+FLAGS+" INTEGER DEFAULT 0 ";
		executeUpdate(alter);
	}
//
//    private void updateTo1Point1() {
//        Log.warn("[BattleTracker] updating database to 1.1");
//        String alter = "ALTER TABLE "+OVERALL_TABLE+" ADD "+KILLS+" INTEGER DEFAULT 0 ";
//        executeUpdate(alter);
//        alter = "ALTER TABLE "+OVERALL_TABLE+" ADD "+DEATHS+" INTEGER DEFAULT 0 ";
//        executeUpdate(alter);
//    }
}
