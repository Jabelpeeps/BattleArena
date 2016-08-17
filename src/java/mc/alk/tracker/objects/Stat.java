package mc.alk.tracker.objects;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.bukkit.entity.Player;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.Defaults;
import mc.alk.arena.events.tracker.MaxRatingChangeEvent;
import mc.alk.arena.events.tracker.WinStatChangeEvent;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.Util;
import mc.alk.arena.util.Cache.CacheObject;
import mc.alk.tracker.controllers.TrackerInterface;
import mc.alk.tracker.objects.VersusRecords.VersusRecord;
import mc.alk.tracker.ranking.EloCalculator;


public abstract class Stat extends CacheObject<String, Stat>{
    @Getter protected String strID = null;
	@Getter protected String name;
	@Getter protected float rating = EloCalculator.DEFAULT_ELO;
	@Getter protected float maxRating = rating;
	@Getter protected int wins = 0, losses= 0, ties = 0;
	@Getter protected int streak = 0, maxStreak =0;
	@Getter protected int count = 1; /// How many members are in the team
	@Getter boolean hidden = false;

	@Getter List<String> members ;

	@Getter VersusRecords recordSet = null;
	@Setter private TrackerInterface parent;

	@Override
	public String getKey() {
		if (strID.length() > 32 )
			Util.printStackTrace();

		return getStrID();
	}

	public void setName(String _name) {
		name = _name; 
		setDirty();
		
		if ( strID != null && strID.length() > 32 ) {
			Log.err("NAME = " + _name + "    strid=" + strID );
			Util.printStackTrace();
		}
	}
	public void setWins(int i) {   wins = i;   setDirty(); }
	public void setStreak(int i) { streak = i; setDirty(); }
	public void setLosses(int i) { losses = i; setDirty(); }
	public void setTies(int i) {   ties = i;   setDirty(); }
	public void setCount(int i){   count = i;  setDirty(); }
    public void endStreak() {      streak = 0; setDirty(); }
	
	public float getKDRatio() { return ((float) wins) / losses; }
	
	public void incLosses() {  streak = 0; losses++;       setDirty(); }
	public void incTies() {    streak = 0; ties++;         setDirty(); }
	public void incWins() {    wins++;     incStreak();    setDirty(); }
	
    public void setMaxStreak(int _maxStreak) { maxStreak = _maxStreak; setDirty(); }
    public void setMaxRating(int _maxRating) { maxRating = _maxRating; setDirty(); }
	
	public void incStreak() {
		streak++;
		if ( streak > maxStreak )
			maxStreak = streak;
		setDirty();
	}

	public void setRating(float _rating){
		rating = _rating;
		if ( rating > maxRating ){
			int threshold =  ( ((int)maxRating) /100) *100 + 100;
			double oldRating = maxRating;
			maxRating = rating;
			if (maxRating < threshold && rating >= threshold){
				maxRating = rating;
				new MaxRatingChangeEvent( parent, this, oldRating ).callSyncEvent();
			}
		}
		setDirty();
	}

	@Override
	public boolean equals( Object obj ) {
		if(this == obj) return true;
		if((obj == null) || (obj.getClass() != this.getClass())) return false;
		TeamStat test = (TeamStat)obj;
		return compareTo(test) == 0;
	}

	/**
	 * Teams are ordered list of strings
	 */
	public int compareTo(TeamStat o) {
		return strID.compareTo(o.strID);
	}

	public VersusRecords getRecord(){
		if (recordSet == null)
			recordSet = new VersusRecords(getKey(),parent.getSQL()) ;
		return recordSet;
	}

	public void win(Stat ts) {
		if (Defaults.DEBUG_ADD_RECORDS) Log.info( "BT Debug: win: tsID=" + ts.getStrID() +
				"  parent=" + parent + "  " + (parent !=null ? parent.getSQL() 
				                                             : "null") + " key=" + getKey() );
		wins++;
		streak++;
		if (streak > maxStreak)
			maxStreak=streak;
		
		getRecord().addWin( ts.getStrID() );
		new WinStatChangeEvent( parent, this, ts ).callSyncEvent();
		setDirty();
	}

	public void loss(Stat ts) {
		losses++;
		streak = 0;
		getRecord().addLoss( ts.getStrID() );
		setDirty();
	}

	public void tie(Stat ts) {
		ties++;
		getRecord().addTie( ts.getStrID() );
		setDirty();
	}

	public static String getKey(Player players) { return players.getName(); }
	public static String getKey(String player){ return player; }

	protected static String getKey(List<String> playernames){
		Collections.sort(playernames);
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String s : playernames){
			if (!first) sb.append(",");
			sb.append(s);
			first = false;
		}
		if (sb.length() > 32){
			return sb.toString().hashCode() + "";
		}
		return sb.toString();
	}

	public VersusRecord getRecordVersus(Stat stat) {
		
		return getRecord().getRecordVersus(stat.getStrID());
	}

	protected void createName(){
		if (name != null && !name.isEmpty()) return;
		
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String n : members){
			if (!first) sb.append(",");
			sb.append(n);
			first = false;
		}
		name = sb.toString();
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		
		sb.append( "[Team=" + getName() + " ["+getRating() + ":" +getKDRatio() + "](" + getWins() + ":" + getLosses() + ":" + getStreak() + ") id=" + strID +
				",count=" + count + ",p.size=" + (members == null ? "null" 
				                                                  : members.size() ) );
		if (recordSet != null){
			sb.append("  [Kills]= ");
			HashMap<String,List<WLTRecord>> records = recordSet.getIndividualRecords();
			if (records != null){
				for (String tk : records.keySet())
					sb.append( tk + ":" + recordSet.getIndividualRecords().get(tk) + " ," );
			}
		}
		sb.append("]");
		return sb.toString();
	}

	public void setSaveIndividual(boolean saveIndividualRecord) {
		if (recordSet != null)
			recordSet.setSaveIndividual(saveIndividualRecord);
	}

	public int getFlags() { return hidden ? 1 : 0; }

	public void setFlags(int flags) { hidden = (flags == 0 ? false : true); }

	public void hide(boolean hide) {
		if ( hidden != hide) {
			setDirty();
			hidden = hide;
		}
	}

	public float getStat(StatType statType) {
		switch(statType){
		case WINS: case KILLS: return getWins();
		case LOSSES: case DEATHS: return getLosses();
		case RANKING: case RATING: return getRating();
		case KDRATIO : case WLRATIO : return getKDRatio();
		case MAXRANKING : case MAXRATING : return getMaxRating();
		case MAXSTREAK: return getMaxStreak();
		case STREAK: return getStreak();
		case TIES: return getTies();
		default:
			break;
		}
		return 0;
	}
	
	public enum SpecialType {
	    STREAK, RAMPAGE
	}

}
