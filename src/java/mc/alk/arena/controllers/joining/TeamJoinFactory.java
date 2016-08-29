package mc.alk.arena.controllers.joining;

import java.util.Collections;
import java.util.List;

import mc.alk.arena.Defaults;
import mc.alk.arena.competition.Competition;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.exceptions.NeverWouldJoinException;
import mc.alk.arena.objects.teams.ArenaTeam;

public class TeamJoinFactory {

    public static AbstractJoinHandler createTeamJoinHandler(MatchParams params) throws NeverWouldJoinException {
        return createTeamJoinHandler( params, null, null );
    }

    public static AbstractJoinHandler createTeamJoinHandler(MatchParams params, Competition competition) 
                                                                                            throws NeverWouldJoinException {
		return createTeamJoinHandler( params, competition, null );
	}

    public static AbstractJoinHandler createTeamJoinHandler(MatchParams params, List<ArenaTeam> teams) 
                                                                                            throws NeverWouldJoinException {
        return createTeamJoinHandler( params, null, teams );
    }

	private static AbstractJoinHandler createTeamJoinHandler(MatchParams params, Competition competition, List<ArenaTeam> teams) 
	                                                                                        throws NeverWouldJoinException {
	    if ( teams == null ) teams = Collections.emptyList();
	    
		if ( params.getMaxTeams() <= Defaults.MAX_TEAMS )
			return new AddToLeastFullTeam( params, competition, teams );	
		
        return new BinPackAdd( params, competition, teams );
	}

}
