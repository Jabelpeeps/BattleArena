package mc.alk.arena.competition;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.Getter;
import mc.alk.arena.events.BAEvent;
import mc.alk.arena.events.CompetitionEvent;
import mc.alk.arena.listeners.PlayerHolder;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.CompetitionState;
import mc.alk.arena.objects.arenas.ArenaListener;
import mc.alk.arena.objects.joining.JoinResponseHandler;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.objects.teams.TeamHandler;

/**
 * Base class for Matches and Events
 * @author alkarin
 *
 */
public abstract class Competition extends PlayerHolder implements JoinResponseHandler, TeamHandler {

	@Getter protected List<ArenaTeam> teams = new CopyOnWriteArrayList<>();
	protected final Set<UUID> leftPlayers = Collections.synchronizedSet(new HashSet<UUID>());
	static int count =0;
	@Getter final protected int id = count++;
	/**
	 * Get the time of when the competition did the given state
	 * @return time or null if not found
	 */
	public abstract Long getTime(CompetitionState state);

	public abstract String getName();

	/**
	 * Transition from one state to another
	 * onStart -> onVictory
	 * @param state CompetitionState
	 */
	protected abstract void transitionTo(CompetitionState state);

	/**
	 * Signify that a player has left the competition
	 * @param player ArenaPlayer
	 * @return whether the player has left or not
	 */
	public boolean playerLeft(ArenaPlayer player) {
		return leftPlayers.contains(player.getUniqueId());
	}

	public void setTeams(List<ArenaTeam> _teams){
		teams.clear();
		teams.addAll( _teams );
		
		for ( int i = 0; i < _teams.size(); i++ ) {
			_teams.get(i).setIndex(i);
		}
	}

	/**
	 * Notify Bukkit Listeners and specific listeners to this match
	 * @param event BAevent
	 */
	@Override
    public void callEvent(BAEvent event) {
		if (event instanceof CompetitionEvent && ((CompetitionEvent)event).getCompetition() == null ) 
			((CompetitionEvent)event).setCompetition(this);
		
		methodController.callEvent(event);
	}

	/**
	 * Add a collection of listeners for this competition
	 * @param transitionListeners collection of ArenaListener
	 */
	public void addArenaListeners(Collection<ArenaListener> transitionListeners){
		for (ArenaListener tl: transitionListeners){
			addArenaListener(tl);}
	}

	/**
	 * Get the team that this player is inside of
	 * @param player ArenaPlayer
	 * @return ArenaPlayer, or null if no team contains this player
	 */
	@Override
    public ArenaTeam getTeam(ArenaPlayer player) {
		for (ArenaTeam t: teams) {
			if (t.hasMember(player)) return t;}
		return null;
	}

    /**
     * Get the team that this player has left
     * @param player ArenaPlayer
     * @return ArenaPlayer, or null if no team has this player leaving
     */
    public ArenaTeam getLeftTeam(ArenaPlayer player) {
        for (ArenaTeam t: teams) {
            if (t.hasLeft(player)) return t;}
        return null;
    }

	/**
	 * Get the team with this index
	 * @param teamIndex index of the team
	 * @return ArenaPlayer, or null if no team exists
	 */
	public ArenaTeam getTeam(int teamIndex) {
		return teams.size() <= teamIndex? null : teams.get(teamIndex);
	}

	/**
	 * Is the player inside of this competition?
	 * @param player to check for
	 * @return true or false
	 */
	public boolean hasPlayer(ArenaPlayer player) {
		for (ArenaTeam t: teams) {
			if (t.hasMember(player)) return true;}
		return false;
	}

	/**
	 * Is the player alive and inside of this competition?
	 * @param player to check for
	 * @return true or false
	 */
	public boolean hasAlivePlayer(ArenaPlayer player) {
		for (ArenaTeam t: teams) {
			if (t.hasAliveMember(player)) return true;}
		return false;
	}

	/**
	 * Get the players that are currently inside of this competition
	 * @return Set of ArenaPlayers
	 */
	public Set<ArenaPlayer> getPlayers() {
		HashSet<ArenaPlayer> players = new HashSet<>();
		for (ArenaTeam t: teams){
			players.addAll(t.getPlayers());}
		return players;
	}
}
