package mc.alk.arena.tracker;

import java.util.Collection;

import mc.alk.arena.objects.tracker.Stat;

public class EloCalculator {
	public static final float MIN_ELO = 100;
	public static float DEFAULT_ELO = 1250;

	float defaultElo, spread;

	public String getName(){
		return "Elo";
	}
	private float eloChange( Stat p1, Stat p2, float result ) {
		float Di = p2.getRating() - p1.getRating();

		float expected = (float) ( 1.0f / ( 1 + Math.pow( 10, Di / spread ) ) );

		float eloChange = getK(  p1.getRating() ) * ( result - expected);
		return eloChange;
	}

	public void changeRatings( Stat p1, Stat p2, boolean tie ) {
		float eloChange = eloChange( p1, p2, tie ? 0.5f : 1.0f );
		float p1elo = p1.getRating() + eloChange;
		float p2elo = p2.getRating() - eloChange;
		p1.setRating(p1elo > MIN_ELO? p1elo : MIN_ELO);
		p2.setRating(p2elo > MIN_ELO? p2elo : MIN_ELO);
	}

	public void changeRatings(Stat ts1, Collection<Stat> teamstats, boolean tie) {
		float result = tie ? 0.5f : 1.0f;
		double eloWinner = 0;
		double dampening = teamstats.size() == 1 ? 1 : teamstats.size() / 2.0D;
		for (Stat ts : teamstats){
			double eloChange = eloChange(ts1,ts,result) / dampening;
			eloWinner+= eloChange;
			ts.setRating((int) (ts.getRating() - eloChange));
		}
		ts1.setRating((int) (ts1.getRating() + eloWinner));
	}


	protected int getK(float elo) {
		if (elo < 1600){
			return 50;
		} else if (elo < 1800){
			return 35;
		} else if (elo < 2000){
			return 20;
		} else if (elo < 2500){
			return 10;
		}
		return 6;
	}

	public void setEloSpread(float _spread) {
		spread = _spread;
	}

	public void setDefaultRating(float elo) {
		defaultElo = elo;
	}

	public float getDefaultRating() {
		return defaultElo;
	}
}
