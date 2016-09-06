package mc.alk.arena.controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import mc.alk.arena.events.players.ArenaPlayerLeaveEvent;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.objects.teams.FormingTeam;
import mc.alk.arena.objects.teams.TeamFactory;
import mc.alk.arena.plugins.HeroesController;


public final class TeamController {

    private TeamController() {}
    
	static final boolean DEBUG = false;

	/** Teams that are created through players wanting to be teams up, or an admin command */
	static final Map<UUID, ArenaTeam> selfFormedTeams = Collections.synchronizedMap(new HashMap<UUID, ArenaTeam>());

	/** Teams that are still being created, these aren't "real" teams yet */
	static final Set<FormingTeam> formingTeams = Collections.synchronizedSet(new HashSet<FormingTeam>());
	
	public static class TeamListener implements Listener {
        @EventHandler
        public void onPlayerLeave(ArenaPlayerLeaveEvent event) {
            ArenaPlayer p = event.getPlayer();
            FormingTeam t = getFormingTeam( p );
            if ( t != null && formingTeams.remove( t ) ) {
                t.sendMessage("&cYour team has been disbanded as &6" + p.getDisplayName() + "&c has left minecraft" );
                return;
            }
            if ( inSelfFormedTeam(p) ) {
                ArenaTeam at = getTeam(p);
                if ( at != null && removeSelfFormedTeam( at ) )
                    at.sendMessage("&cYour team has been disbanded as &6" + p.getDisplayName() + "&c has left minecraft");
            }
        }
	}
	/**
	 * A valid team should either be currently being "handled" or is selfFormed
	 * @param player ArenaPlayer
	 * @return Team
	 */
	public static ArenaTeam getTeam( ArenaPlayer player ) {
		ArenaTeam at = selfFormedTeams.get( player.getUniqueId() );
        if ( at == null && HeroesController.enabled() )
            return HeroesController.getTeam( player.getPlayer() );
        return at;
    }
	
    public static ArenaTeam createTeam(MatchParams mp, ArenaPlayer p) {
        if (DEBUG) System.out.println("------- createTeam sans handler " + p.getName());
        return TeamFactory.createCompositeTeam(-1, mp, p);
    }
    
    public static boolean inSelfFormedTeam( ArenaPlayer player ) {
        return selfFormedTeams.containsKey( player.getUniqueId() ) ||
                ( HeroesController.enabled() && HeroesController.getTeam(player.getPlayer() ) != null );
    }

    public static Collection<ArenaTeam> getSelfFormedTeams() {
        return selfFormedTeams.values();
    }

	public static boolean removeSelfFormedTeam(ArenaTeam team) {
        List<UUID> l = new ArrayList<>();
        for (Map.Entry<UUID, ArenaTeam> entry : selfFormedTeams.entrySet()) {
            if (entry.getValue().equals(team)){
                l.add(entry.getKey());
            }
        }
        for (UUID p: l) {
            selfFormedTeams.remove(p);
        }
        return !l.isEmpty();
    }

	public static void addSelfFormedTeam(ArenaTeam team) {
        for (ArenaPlayer ap: team.getPlayers()){
            selfFormedTeams.put(ap.getUniqueId(), team);
        }
	}

	public static FormingTeam getFormingTeam(ArenaPlayer p) {
		for ( FormingTeam ft : formingTeams ) {
			if ( ft.hasMember( p ) )
				return ft;
		}
		return null;
	}

	public static void addFormingTeam( FormingTeam ft ) { formingTeams.add(ft); }

	public static boolean removeFormingTeam( FormingTeam ft ) { return formingTeams.remove(ft); }

	@Override
	public String toString() { return "[TeamController]"; }
}
