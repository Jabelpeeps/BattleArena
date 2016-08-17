package mc.alk.arena.events.tracker;


import lombok.Getter;
import mc.alk.tracker.controllers.TrackerInterface;
import mc.alk.tracker.objects.Stat;

public class MaxRatingChangeEvent extends TrackerEvent{
	@Getter final Stat stat;
	@Getter final double oldMaxRating;
	public MaxRatingChangeEvent(TrackerInterface trackerInterface, Stat stat, double oldMaxRating){
		super(trackerInterface);
		this.stat = stat;
		this.oldMaxRating = oldMaxRating;
	}
}
