package mc.alk.tracker.objects;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;

import lombok.Getter;
import lombok.Setter;
import mc.alk.tracker.TrackerInterface;

public class LeaderboardHologram extends Hologram {
	@Getter private String leaderboardName;
	@Getter @Setter private int topAmount;
	@Getter @Setter private TrackerInterface trackerInterface;
	@Getter @Setter private StatType statType;

	public LeaderboardHologram( TrackerInterface trackerInterface, StatType statType, String leaderboardName, 
	                            int topAmount, double distanceBetweenLines, Location location ) {
	    
		super(distanceBetweenLines, location, ChatColor.GREEN+ "Updating...");
		setLeaderboardName(leaderboardName);
		setTrackerInterface(trackerInterface);
		setStatType(statType);
		setTopAmount(topAmount);
		update();
	}

	public LeaderboardHologram( TrackerInterface trackerInterface, StatType statType, String leaderboardName, 
	                            int topAmount, VerticalTextSpacing spacingType, Location location ) {
	    
		super(spacingType.getSpacing(), location, ChatColor.GREEN+ "Updating...");
		setLeaderboardName(leaderboardName);
		setTrackerInterface(trackerInterface);
		setStatType(statType);
		setTopAmount(topAmount);
		update();
	}

	public void setLeaderboardName(String name) {
		getLines().remove(getLeaderboardName());
		getLines().add(0, leaderboardName);
		this.leaderboardName = name;
	}

	public void update() {
		getLines().clear();
		setLeaderboardName(getLeaderboardName());
		List<Stat> stats = getTrackerInterface().getTopX(getStatType(), getTopAmount());
		
		for (Stat stat : stats) {
			getLines().add(stat.getName() + " - " + stat.getWins());
		}
	}
}
