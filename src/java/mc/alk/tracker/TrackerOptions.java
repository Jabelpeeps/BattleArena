package mc.alk.tracker;

import lombok.Getter;
import lombok.Setter;
import mc.alk.tracker.controllers.ConfigController;
import mc.alk.tracker.ranking.EloCalculator;
import mc.alk.tracker.ranking.RatingCalculator;

public class TrackerOptions {
	@Setter boolean saveIndividualRecords = false;
	@Getter @Setter RatingCalculator ratingCalculator;

	public TrackerOptions(){
		EloCalculator ec = new EloCalculator();
		ec.setDefaultRating((float) ConfigController.getDouble("elo.default",1250));
		ec.setEloSpread((float) ConfigController.getDouble("elo.spread",400));
		ratingCalculator = ec;
	}
	public boolean savesIndividualRecords() {
		return saveIndividualRecords;
	}
}
