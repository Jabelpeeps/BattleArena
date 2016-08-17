package mc.alk.arena.events.tracker;

import lombok.Getter;
import mc.alk.tracker.controllers.TrackerInterface;
import mc.alk.tracker.objects.Stat;

public class WinStatChangeEvent extends TrackerEvent{
	@Getter final Stat winner;
	@Getter final Stat loser;
	public WinStatChangeEvent(TrackerInterface trackerInterface, Stat winner, Stat loser){
		super(trackerInterface);
		this.winner = winner;
		this.loser = loser;
	}
}
