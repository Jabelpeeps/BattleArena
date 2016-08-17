package mc.alk.arena.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import mc.alk.arena.Defaults;
import mc.alk.arena.controllers.PlayerController;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.stats.ArenaStat;
import mc.alk.arena.objects.stats.TrackerArenaStat;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.util.Log;
import mc.alk.tracker.Tracker;
import mc.alk.tracker.controllers.TrackerController;
import mc.alk.tracker.controllers.TrackerInterface;
import mc.alk.tracker.objects.Stat;
import mc.alk.tracker.objects.StatType;
import mc.alk.tracker.objects.WLTRecord.WLT;


public class BTInterface {
	public static Tracker battleTracker = null;
	public static TrackerInterface aBTI = null;
	static private HashMap<String, TrackerInterface> btis = new HashMap<>();
	static private HashMap<String, TrackerInterface> currentInterfaces = new HashMap<>();
	final TrackerInterface ti;
	boolean valid = false;

	public BTInterface(MatchParams mp){
		ti = getInterface(mp);
		valid = battleTracker != null && ti != null;
	}
	public boolean isValid(){
		return valid;
	}
	public Stat getRecord(TrackerInterface bti, ArenaTeam t){
		try{return bti.getRecord(t.getBukkitPlayers());} catch(Exception e){Log.printStackTrace(e);return null;}
	}
	public Stat loadRecord(TrackerInterface bti, ArenaTeam t){
		try{return bti.loadRecord(t.getBukkitPlayers());} catch(Exception e){Log.printStackTrace(e);return null;}
	}
	public static TrackerInterface getInterface(MatchParams mp){
		if (mp == null)
			return null;
		final String db = mp.getDBTableName();
		return db == null ? null : btis.get(db);
	}
	public static boolean hasInterface(MatchParams mp){
		if (mp == null)
			return false;
		final String db = mp.getDBTableName();
		return db != null && btis.containsKey(db);
	}

	public static void addRecord(TrackerInterface bti, Set<ArenaTeam> victors, Set<ArenaTeam> losers,
			                        Set<ArenaTeam> drawers, WLT wld, boolean teamRating) {
		if (victors != null) {
			Set<ArenaPlayer> winningPlayers = new HashSet<>();
			for (ArenaTeam w : victors){
				winningPlayers.addAll(w.getPlayers());
			}
			addRecord( bti, winningPlayers, losers, wld, teamRating );
		}
	}

	public static void addRecord(TrackerInterface bti, Set<ArenaPlayer> players, Collection<ArenaTeam> losers,
			                                                    WLT win, boolean teamRating) {
		if (bti == null)
			return;
		try{
			Set<Player> winningPlayers = PlayerController.toPlayerSet(players);
			if (losers.size() == 1){
				Set<Player> losingPlayers = new HashSet<>();
				for (ArenaTeam t: losers){losingPlayers.addAll(t.getBukkitPlayers());}
				if (Defaults.DEBUG_TRACKING) Log.info("BA Debug: addRecord ");
				for (Player p: winningPlayers){
					if (Defaults.DEBUG_TRACKING) Log.info("BA Debug: winner = "+p.getName());}
				for (Player p: losingPlayers){
					if (Defaults.DEBUG_TRACKING) Log.info("BA Debug: loser = "+p.getName());}
				bti.addTeamRecord(winningPlayers, losingPlayers, WLT.WIN);
			} 
			else {
				Collection<Collection<Player>> plosers = new ArrayList<>();
				for (ArenaTeam t: losers){
					plosers.add(t.getBukkitPlayers());
				}
				bti.addRecordGroup(winningPlayers, plosers, WLT.WIN);
			}
		} catch(Exception e){
			Log.printStackTrace(e);
		}
	}

	public static boolean addBTI(MatchParams pi) {
		if (battleTracker == null)
			return false;
		final String dbName = pi.getDBTableName();
		if (Defaults.DEBUG) Log.info("adding BTI for " + pi +"  " + dbName);
		TrackerInterface bti = btis.get(dbName);
		if (bti == null){

			bti = currentInterfaces.get(dbName);
			if (bti==null){ 
				bti = Tracker.getInterface(dbName);
				currentInterfaces.put(dbName, bti);
				if (aBTI == null)
					aBTI = bti;
			}
			btis.put(dbName, bti);
		}
		return true;
	}

	public static void resumeTracking(ArenaPlayer p) {
		if (aBTI != null)
		    TrackerController.resumeTracking(p.getName());
	}
	public static void stopTracking(ArenaPlayer p) {
		if (aBTI != null)
		    TrackerController.stopTracking( p.getName() );
	}
	public static void resumeTrackingMessages(ArenaPlayer p) {
		if (aBTI != null)
		    TrackerController.resumeAnnouncing(p.getName());
	}
	public static void stopTrackingMessages(ArenaPlayer p) {
		if (aBTI != null)
		    TrackerController.stopAnnouncing(p.getName());
	}

	public static void resumeTracking(Set<Player> players) {
		if (aBTI != null)
		    TrackerController.resumeTracking(players);
	}
	public static void stopTracking(Set<Player> players) {
		if (aBTI != null)
		    TrackerController.stopTracking(players);
	}

	public Integer getElo(ArenaTeam t) {
		if (!isValid())
			return (int) Defaults.DEFAULT_ELO;
		Stat s = getRecord(ti,t);
		return (int) (s == null ? Defaults.DEFAULT_ELO : s.getRating());
	}

	public Stat loadRecord(ArenaTeam team) {
		if (!isValid()) return null;
		return loadRecord(ti, team);
	}
	public Stat loadRecord(OfflinePlayer player){
		if (!isValid()) return null;
		try{return ti.loadRecord(player);} catch(Exception e){Log.printStackTrace(e);return null;}
	}

	public static ArenaStat loadRecord(String dbName, ArenaPlayer ap) {
		TrackerInterface ti = btis.get(dbName);
		if (ti == null)
			return TrackerController.BLANK_STAT;
		Stat st = null;
		try{st = ti.loadPlayerRecord(ap.getName());}catch(Exception e){Log.printStackTrace(e);}
		return st == null ? TrackerController.BLANK_STAT : new TrackerArenaStat(dbName, st);
	}

	public static ArenaStat loadRecord(String dbName, ArenaTeam t) {
		TrackerInterface ti = btis.get(dbName);
		if (ti == null)
			return TrackerController.BLANK_STAT;
		Stat st = null;
		try{st = ti.loadRecord(t.getBukkitPlayers());}catch(Exception e){Log.printStackTrace(e);}
		return st == null ? TrackerController.BLANK_STAT : new TrackerArenaStat(dbName, st);
	}

	public boolean setRating(OfflinePlayer player, Integer elo) {
		return ti.setRating(player, elo);
	}
	public void resetStats() {
		ti.resetStats();
	}
	public void printTopX(CommandSender sender, int x, int minTeamSize, String headerMsg, String bodyMsg) {
		ti.printTopX(sender, StatType.RANKING, x, minTeamSize,headerMsg,bodyMsg);
	}
	public static void setTrackerPlugin(Plugin plugin) {
		battleTracker = (Tracker) plugin;
	}

//	public static void addRecord(MatchParams mp, ArenaPlayer victor,ArenaPlayer loser, WLT wld) {
//		TrackerInterface bti = BTInterface.getInterface(mp);
//		if (bti != null ){
//			switch (wld){
//			case TIE:
//				break;
//			case LOSS:
//				break;
//			case UNKNOWN:
//				break;
//			case WIN:
//				break;
//			default:
//				break;
//			}
//			bti.addPlayerRecord(victor.getName(), loser.getName(), wld);
//		}
//	}

	public static void addRecord(MatchParams mp, Set<ArenaTeam> victors, Set<ArenaTeam> losers, 
	                                        Set<ArenaTeam> drawers, WLT wld, boolean teamRating) {
		TrackerInterface bti = BTInterface.getInterface(mp);
		if (bti != null ){
			try{
				BTInterface.addRecord(bti,victors,losers,drawers,wld, teamRating);
			}catch(Exception e){
				Log.printStackTrace(e);
			}
		}
	}

}
