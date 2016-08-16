package mc.alk.tracker.events;

import mc.alk.tracker.TrackerInterface;
import mc.alk.tracker.objects.Stat;

public class MaxRatingChangeEvent extends TrackerEvent{
	final Stat stat;
	final double oldMaxRating;
	public MaxRatingChangeEvent(TrackerInterface trackerInterface, Stat stat, double oldMaxRating){
		super(trackerInterface);
		this.stat = stat;
		this.oldMaxRating = oldMaxRating;
	}

	public Stat getStat(){
		return stat;
	}

	public double getOldMaxRating(){
		return oldMaxRating;
	}
}
