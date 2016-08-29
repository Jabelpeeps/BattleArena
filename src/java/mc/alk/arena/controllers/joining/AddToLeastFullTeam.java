package mc.alk.arena.controllers.joining;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mc.alk.arena.Defaults;
import mc.alk.arena.competition.Competition;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.ArenaSize;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.exceptions.NeverWouldJoinException;
import mc.alk.arena.objects.joining.TeamJoinObject;
import mc.alk.arena.objects.options.JoinOptions;
import mc.alk.arena.objects.options.JoinOptions.JoinOption;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.objects.teams.TeamFactory;

public class AddToLeastFullTeam extends AbstractJoinHandler {

    public AddToLeastFullTeam(MatchParams params, Competition competition, List<ArenaTeam> newTeams)
                                                                            throws NeverWouldJoinException {
        
        super(params,competition, newTeams);
        if (minTeams == ArenaSize.MAX || maxTeams == ArenaSize.MAX)
            throw new NeverWouldJoinException("If you add players by adding them to the next team in the list, there must be a finite number of players");
        /// Lets add in all our teams first
        if (minTeams > Defaults.MAX_TEAMS)
            throw new NeverWouldJoinException( "You can't make more than " + Defaults.MAX_TEAMS + " teams" );
        if (newTeams != null){
            for (ArenaTeam at : newTeams) {
                addTeam(at);
            }
        }
        for ( int i = teams.size(); i < minTeams; i++ ) {
            ArenaTeam team = TeamFactory.createCompositeTeam(i, params);
            addTeam(team);
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
                return new TeamJoinResult(TeamJoinStatus.ADDED_TO_EXISTING,oldTeam.getMinPlayers() - oldTeam.size());
            }
        }
        /// Try to let them join their specified team if possible
        JoinOptions jo = tqo.getJoinOptions();
        if (jo != null && jo.hasOption(JoinOption.TEAM)){
            Integer index = (Integer) jo.getOption(JoinOption.TEAM);
            if (index < maxTeams){ /// they specified a team index within range
                ArenaTeam baseTeam= teams.get(index);
                TeamJoinResult tjr = teamFits(baseTeam, team);
                if (tjr != CANTFIT)
                    return tjr;
            }
        }
        boolean hasZero = false;
        for (ArenaTeam t : teams){
            if (t.size() == 0){
                hasZero = true;
                break;
            }
        }
        /// Since this is nearly the same as BinPack add... can we merge somehow easily?
        if (!hasZero && teams.size() < maxTeams){
            ArenaTeam ct = TeamFactory.createCompositeTeam(teams.size(), matchParams);
            ct.setCurrentParams(tqo.getMatchParams());
            ct.addPlayers(team.getPlayers());
            team.setIndex(ct.getIndex());
            if (ct.size() <= ct.getMaxPlayers()){
                addTeam(ct);
                if (ct.size() >= ct.getMinPlayers()) {
                    return new TeamJoinResult(TeamJoinStatus.ADDED, ct.getMinPlayers() - ct.size());
                }
                return new TeamJoinResult(TeamJoinStatus.ADDED_STILL_NEEDS_PLAYERS, ct.getMinPlayers() - ct.size());
            }
        }
        /// Try to fit them with an existing team
        List<ArenaTeam> sortedBySize = new ArrayList<>(teams);
        
        Collections.sort( sortedBySize, (at1, at2) -> { return at1.size() - at2.size(); });
        
        for (ArenaTeam baseTeam : sortedBySize){
            TeamJoinResult tjr = teamFits(baseTeam, team);
            if (tjr != CANTFIT)
                return tjr;
        }
        return CANTFIT;
    }

    private TeamJoinResult teamFits(ArenaTeam baseTeam, ArenaTeam team) {
        if ( baseTeam.size() + team.size() <= baseTeam.getMaxPlayers()){
            team.setIndex(baseTeam.getIndex());
            addToTeam(baseTeam, team.getPlayers());
            if (baseTeam.size() == 0){
                return new TeamJoinResult(TeamJoinStatus.ADDED, baseTeam.getMinPlayers() - baseTeam.size());
            }
            return new TeamJoinResult(TeamJoinStatus.ADDED_TO_EXISTING, baseTeam.getMinPlayers() - baseTeam.size());
        }
        return CANTFIT;
    }

    @Override
    public String toString(){
        return "["+competition.getParams().getName() +":JH:AddToLeast]";
    }
}
