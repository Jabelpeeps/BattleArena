//package mc.alk.tracker;
//
//import lombok.Getter;
//import lombok.Setter;
//import mc.alk.tracker.controllers.TrackerConfigController;
//import mc.alk.tracker.ranking.EloCalculator;
//import mc.alk.tracker.ranking.RatingCalculator;
//
//public class TrackerOptions {
//	@Setter boolean saveIndividualRecords = false;
//	@Getter @Setter RatingCalculator ratingCalculator;
//
//	public TrackerOptions(){
//		EloCalculator ec = new EloCalculator();
//		ec.setDefaultRating((float) TrackerConfigController.getDouble("elo.default",1250));
//		ec.setEloSpread((float) TrackerConfigController.getDouble("elo.spread",400));
//		ratingCalculator = ec;
//	}
//	public boolean savesIndividualRecords() {
//		return saveIndividualRecords;
//	}
//}
