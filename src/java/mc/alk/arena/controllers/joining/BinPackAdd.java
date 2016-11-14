package mc.alk.arena.controllers.joining;

import java.util.Collection;
import java.util.List;

import mc.alk.arena.competition.Competition;
import mc.alk.arena.controllers.TeamController;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.joining.TeamJoinObject;
import mc.alk.arena.objects.teams.ArenaTeam;

/**
 * When there is an infinite number of teams
 * @author alkarin
 *
 */
public class BinPackAdd extends AbstractJoinHandler {
    boolean full = false;

    public BinPackAdd(MatchParams params, Competition competition, List<ArenaTeam> newTeams) {
        super(params, competition, newTeams);
        if (newTeams != null){
            for (ArenaTeam at : newTeams) {
                addTeam(at);
            }
        }
    }

    @Override
    public boolean switchTeams(ArenaPlayer player, Integer toTeamIndex, boolean checkSizes) {
        ArenaTeam oldTeam = player.getTeam();
        if (oldTeam == null || toTeamIndex >= maxTeams) // no correct team, or team out of range
            return false;
        if (checkSizes){
            if (oldTeam.size()-1 < oldTeam.getMinPlayers())
                return false;
            ArenaTeam team = addToPreviouslyLeftTeam(player);
            if (team != null)
                return true;

            /// Try to let them join their specified team if possible
            team = teams.get(toTeamIndex);
            if (team.size() + 1 <= team.getMaxPlayers()) {
                removeFromTeam(oldTeam, player);
                addToTeam(team, player);
                return true;
            }
            return false;
        }
        ArenaTeam team = teams.get(toTeamIndex);
        removeFromTeam(oldTeam, player);
        addToTeam(team, player);
        return true;
    }

    @Override
    public TeamJoinResult joiningTeam(TeamJoinObject tqo) {
        ArenaTeam team = tqo.getTeam();
        if (team.size()==1){

            ArenaTeam oldTeam = addToPreviouslyLeftTeam(team.getPlayers().iterator().next());
            if (oldTeam != null){
                team.setIndex(oldTeam.getIndex());
                return new TeamJoinResult( TeamJoinStatus.ADDED, oldTeam.getMinPlayers() - oldTeam.size() );
            }
        }
        /// So we couldnt add them to an existing team
        /// Can we add them to a new team
        if (teams.size() < maxTeams) {
            Collection<ArenaPlayer> players = team.getPlayers();
            /// Fill empty teams first
            for (ArenaTeam t : teams) {
                if (t.size() == 0 && players.size() == t.getMaxPlayers()){
                    addToTeam(t,players);
                    return new TeamJoinResult( TeamJoinStatus.ADDED, t.getMinPlayers() - t.size() );
                }
            }
            ArenaTeam ct = TeamController.createCompositeTeam(teams.size(), matchParams);
            ct.addPlayers(team.getPlayers());
            team.setIndex(ct.getIndex());
            if (ct.size() <= ct.getMaxPlayers()){
                addTeam(ct);
                if (ct.size() >= ct.getMinPlayers()) {
                    return new TeamJoinResult( TeamJoinStatus.ADDED, ct.getMinPlayers() - ct.size() );
                }
                return new TeamJoinResult( TeamJoinStatus.STILL_NEEDS_PLAYERS, ct.getMinPlayers() - ct.size() );
            }
        }

        for ( ArenaTeam t : teams ) {
            final int size = t.size() + team.size();
            if ( size <= t.getMaxPlayers() ) {
                t.addPlayers( team.getPlayers() );
                
                if ( size >= t.getMinPlayers() ) { 
                    team.setIndex( t.getIndex() );
                    addToTeam( t, team.getPlayers() );
                    return new TeamJoinResult( TeamJoinStatus.ADDED, 0 );
                }
                return new TeamJoinResult( TeamJoinStatus.STILL_NEEDS_PLAYERS, t.getMinPlayers() - t.size() );
            }
        }
        return CANTFIT;
    }

    @Override
    public String toString(){
        return (competition == null ? " null" : "["+competition.getParams().getName()) +":JH:BinPackAdd]";
    }
}
