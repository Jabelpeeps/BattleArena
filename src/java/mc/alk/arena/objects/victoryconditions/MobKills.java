package mc.alk.arena.objects.victoryconditions;

import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDeathEvent;

import mc.alk.arena.competition.Match;
import mc.alk.arena.events.matches.MatchFindCurrentLeaderEvent;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.events.ArenaEventHandler;
import mc.alk.arena.objects.events.ArenaEventHandler.ArenaEventPriority;
import mc.alk.arena.objects.scoreboard.ArenaObjective;
import mc.alk.arena.objects.scoreboard.ArenaScoreboard;
import mc.alk.arena.objects.scoreboard.SAPIDisplaySlot;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.objects.victoryconditions.interfaces.ScoreTracker;
import mc.alk.arena.util.DmgDeathUtil;

public class MobKills extends VictoryCondition implements ScoreTracker{
	final ArenaObjective mkills;

    public MobKills(Match _match, ConfigurationSection section) {
        super( _match );
        mkills = new ArenaObjective( getClass().getSimpleName(),
                                     section.getString( "displayName", "Mob Kills" ), 
                                     section.getString( "criteria", "Kill mobs" ),
                                     SAPIDisplaySlot.SIDEBAR,
                                     60 );
    }
	@Override
	public List<ArenaTeam> getLeaders() { return mkills.getLeaders(); }
	@Override
	public TreeMap<Integer,Collection<ArenaTeam>> getRanks() { return mkills.getRanks(); }

	@ArenaEventHandler( priority = ArenaEventPriority.LOW )
	public void mobDeathEvent( EntityDeathEvent event ) {
		switch( event.getEntityType() ) {
    		case BLAZE: case CAVE_SPIDER: case CHICKEN: case COW: case CREEPER: case ENDERMAN:
    		case ENDER_DRAGON: case GHAST: case GIANT: case IRON_GOLEM: case MAGMA_CUBE:
    		case MUSHROOM_COW: case OCELOT: case PIG: case PIG_ZOMBIE: case SHEEP: case SILVERFISH:
    		case SKELETON: case SLIME: case SNOWMAN: case SPIDER: case SQUID: case VILLAGER:
    		case WOLF: case ZOMBIE:
    			break;
    		default:
    			return;
		}
		ArenaPlayer killer = DmgDeathUtil.getPlayerCause(event.getEntity().getLastDamageCause());
		if ( killer == null ) return;
		
		ArenaTeam t = match.getTeam(killer);
		if ( t == null ) return;
		
		t.addKill(killer);
		mkills.addPoints(t, 1);
		mkills.addPoints(killer, 1);
	}

	@ArenaEventHandler( priority = ArenaEventPriority.LOW )
	public void onFindCurrentLeader(MatchFindCurrentLeaderEvent event) {
        event.setResult(mkills.getMatchResult(match));
	}
	@Override
	public void setScoreboard(ArenaScoreboard scoreboard) {
		mkills.setScoreboard(scoreboard);
		scoreboard.addObjective(mkills);
	}
	@Override
	public void setDisplayTeams(boolean display) {
		mkills.setDisplayTeams(display);
	}
}
