//package mc.alk.arena.objects.scoreboard.base;
//
//import java.util.List;
//
//import mc.alk.arena.competition.Match;
//import mc.alk.arena.objects.ArenaPlayer;
//import mc.alk.arena.objects.scoreboard.ArenaObjective;
//import mc.alk.arena.objects.scoreboard.ArenaScoreboard;
//import mc.alk.arena.objects.teams.ArenaTeam;
//import mc.alk.arena.scoreboardapi.SAPIDisplaySlot;
//import mc.alk.arena.scoreboardapi.STeam;
//
//public class BAScoreboard extends ArenaScoreboard {
//	Match match;
//
//	public BAScoreboard(Match match) {
//		super(match.getName());
//		this.match = match;
//	}
//
//	@Override
//	public ArenaObjective createObjective(String id, String criteria, String displayName) {
//		return createObjective(id,criteria,displayName,SAPIDisplaySlot.SIDEBAR);
//	}
//
//	@Override
//	public ArenaObjective createObjective(String id, String criteria, String displayName,
//			SAPIDisplaySlot slot) {
//		return createObjective(id,criteria,displayName,SAPIDisplaySlot.SIDEBAR, 50);
//	}
//
//	@Override
//	public ArenaObjective createObjective(String id, String criteria, String displayName,
//			SAPIDisplaySlot slot, int priority) {
//		ArenaObjective o = new ArenaObjective(id,criteria,displayName,slot,priority);
//		addObjective(o);
//		return o;
//	}
//
//	@Override
//	public STeam addTeam(ArenaTeam team) { return null; }
//
//	@Override
//	public STeam addedToTeam(ArenaTeam team, ArenaPlayer player) { return null; }
//
//	@Override
//	public STeam removeTeam(ArenaTeam team) { return null; }
//
//	@Override
//	public STeam removedFromTeam(ArenaTeam team, ArenaPlayer player) { return null; }
//
//	@Override
//	public void setDead(ArenaTeam t, ArenaPlayer p) { }
//
//	@Override
//	public void leaving(ArenaTeam t, ArenaPlayer player) { }
//
//	@Override
//	public void addObjective(ArenaObjective scores) {
//	    setObjectiveScoreboard(scores);
//	}
//
//	@Override
//	public List<STeam> getTeams() { return null; }
//}
