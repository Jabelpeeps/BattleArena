package mc.alk.tracker.objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mc.alk.tracker.objects.WLTRecord.WLT;

@AllArgsConstructor @Getter
public class StatChange {
	final Stat team1;
	final Stat team2;
	final WLT wlt;
	final boolean changeWinLossRecords;

}
