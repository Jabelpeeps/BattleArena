package mc.alk.arena.competition;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.Getter;
import mc.alk.arena.objects.Matchup;


class Round {
//	int round;
	@Getter List<Matchup> matchups = new CopyOnWriteArrayList<>();
	
//	public Round(int _round) {
//		round = _round;
//	}
	public void addMatchup(Matchup m){
		matchups.add(m);
	}
	
	public List<Matchup> getCompleteMatchups() {
		List<Matchup> completed = new ArrayList<>();
		for (Matchup m : matchups){
			if (m.getResult().isFinished())
				completed.add(m);
		}
		return completed;
	}
}
