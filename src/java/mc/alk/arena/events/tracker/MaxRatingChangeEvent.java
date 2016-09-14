package mc.alk.arena.events.tracker;


import lombok.Getter;
import mc.alk.arena.objects.tracker.Stat;
import mc.alk.arena.tracker.TrackerInterface;

public class MaxRatingChangeEvent extends TrackerEvent{
	@Getter final Stat stat;
	@Getter final double oldMaxRating;
	public MaxRatingChangeEvent(TrackerInterface trackerInterface, Stat stat, double oldMaxRating){
		super(trackerInterface);
		this.stat = stat;
		this.oldMaxRating = oldMaxRating;
	}
}
