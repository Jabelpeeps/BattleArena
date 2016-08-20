package mc.alk.arena.objects.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mc.alk.arena.objects.tracker.Stat;
import mc.alk.arena.objects.tracker.VersusRecords.VersusRecord;

@AllArgsConstructor
public class TrackerArenaStat implements ArenaStat {
    @Getter final String DB;
	@Getter final Stat stat;

	@Override
	public int getWinsVersus(ArenaStat ostat) {
		VersusRecord vs = stat.getRecordVersus( ((TrackerArenaStat) ostat).getStat() );
		return vs == null ? 0 : vs.wins;
	}

	@Override
	public int getLossesVersus(ArenaStat ostat) {
		VersusRecord vs = stat.getRecordVersus( ((TrackerArenaStat) ostat).getStat() );
		return vs == null ? 0 : vs.losses;
	}

	@Override
	public int getWins() { return stat.getWins(); }
	@Override
	public int getLosses() { return stat.getLosses(); }
	@Override
	public int getRanking() { return (int) stat.getRating(); }
	@Override
	public float getRating() { return (int) stat.getRating(); }
	@Override
	public String toString() { return stat.toString(); }
}
