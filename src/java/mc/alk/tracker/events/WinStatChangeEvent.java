package mc.alk.tracker.events;

import mc.alk.tracker.TrackerInterface;
import mc.alk.tracker.objects.Stat;

public class WinStatChangeEvent extends TrackerEvent{
	final Stat winner;
	final Stat loser;
	public WinStatChangeEvent(TrackerInterface trackerInterface, Stat winner, Stat loser){
		super(trackerInterface);
		this.winner = winner;
		this.loser = loser;
	}

	/**
	 * The winner
	 * @return winner, will never be null
	 */
	public Stat getWinner(){
		return winner;
	}
	/**
	 * The loser, can be null
	 * @return Whoever lost
	 */
	public Stat getLoser(){
		return loser;
	}
}
