package mc.alk.arena.objects.teams;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import lombok.Getter;
import mc.alk.arena.objects.ArenaPlayer;


/**
 * Class that is a collection of other teams
 * @author alkarin
 *
 */
public class CompositeTeam extends ArenaTeam {
	@Getter final Set<ArenaTeam> oldTeams = new HashSet<>();

	public CompositeTeam() {
		super();
		pickupTeam = true;
	}

	protected CompositeTeam(ArenaPlayer ap) {
		super(ap);
		pickupTeam = true;
	}

	protected CompositeTeam(Collection<ArenaPlayer> _players) {
		super(_players);
		pickupTeam = true;
	}

	protected CompositeTeam(ArenaTeam team) {
		this();
		addTeam(team);
	}

	public void addTeam(ArenaTeam t) {
		if (t instanceof CompositeTeam){
			CompositeTeam ct = (CompositeTeam) t;
			oldTeams.add(ct);
			oldTeams.addAll(ct.oldTeams);
			players.addAll(ct.getPlayers());
			nameChanged = true;
		} 
		else if (oldTeams.add(t)){
			nameChanged = true;
			players.addAll(t.getPlayers());
		}
	}

	public boolean removeTeam(ArenaTeam t) {
		if (t instanceof CompositeTeam){
			for (ArenaTeam tt : ((CompositeTeam)t).getOldTeams()){
				if (oldTeams.remove(tt)){
					nameChanged = true;}
			}
		}
		boolean has = oldTeams.remove(t);
		if (has){
			players.removeAll(t.getPlayers());
			nameChanged = true;
		}
		return has;
	}

	@Override
	public boolean hasTeam(ArenaTeam team){
		for (ArenaTeam t: oldTeams){
			if (t.hasTeam(team))
				return true;
		}
		return false;
	}

	@Override
	public boolean removePlayer(ArenaPlayer p) {
		boolean success = super.removePlayer(p);
		for (ArenaTeam t: oldTeams){
			if (t.hasMember(p)){
				success |= t.removePlayer(p);
				if (t.size() == 0){
					oldTeams.remove(t);
				}
				nameChanged = true;
				break;
			}
		}
        return success;
    }

	@Override
	public void clear() {
		super.clear();
		for (ArenaTeam t: oldTeams){
			t.clear();
		}
	}
}
