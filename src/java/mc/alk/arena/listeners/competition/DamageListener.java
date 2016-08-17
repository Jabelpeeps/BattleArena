package mc.alk.arena.listeners.competition;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import mc.alk.arena.controllers.PlayerController;
import mc.alk.arena.listeners.PlayerHolder;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.PVPState;
import mc.alk.arena.objects.StateGraph;
import mc.alk.arena.objects.arenas.ArenaListener;
import mc.alk.arena.objects.events.ArenaEventHandler;
import mc.alk.arena.objects.events.ArenaEventPriority;
import mc.alk.arena.objects.options.StateOptions;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.util.DmgDeathUtil;

public class DamageListener implements ArenaListener{
	StateGraph transitionOptions;
	PlayerHolder holder;

    public DamageListener(PlayerHolder holder){
		this.transitionOptions = holder.getParams().getStateGraph();
		this.holder = holder;
	}

	@ArenaEventHandler( suppressCastWarnings=true, priority=ArenaEventPriority.LOW)
	public void onEntityDamageEvent(EntityDamageEvent event) {
        ArenaPlayer damager = null;
        final ArenaPlayer target = 
                (event.getEntity() instanceof Player) ? PlayerController.toArenaPlayer((Player) event.getEntity()) 
                                                      : null;

        /// Handle setting targets for mob spawns first
        if (event instanceof EntityDamageByEntityEvent && event.getEntity() instanceof LivingEntity){
            final Entity damagerEntity = ((EntityDamageByEntityEvent)event).getDamager();
            damager = DmgDeathUtil.getPlayerCause(damagerEntity);
            if (damager != null ) {
                if (target == null || damager.getTeam()==null || target.getTeam()==null ||
                        !target.getTeam().equals(damager.getTeam())){
                    damager.setTarget((LivingEntity) event.getEntity());
                }
            }
            if (target != null && damagerEntity instanceof LivingEntity) {
                if ((target.getTarget() == null || target.getTarget().isDead()) &&
                        (damager == null ||
                                damager.getTeam()==null ||
                                target.getTeam()==null ||
                                !target.getTeam().equals(damager.getTeam())
                        )
                        )
                target.setTarget((LivingEntity) damagerEntity);
            }
        }
		if (target == null) {
            return;
        }
		final StateOptions to = transitionOptions.getOptions(holder.getState());
		if (to == null) {
            return;
        }
		final PVPState pvp = to.getPVP();
		if (pvp == null) {
            return;
        }

		if (pvp == PVPState.INVINCIBLE){
			/// all damage is cancelled
			target.setFireTicks(0);
            event.setDamage( 0 );
			event.setCancelled(true);
			return;
		}

		if (!(event instanceof EntityDamageByEntityEvent)){
			return;}


        switch(pvp){
		case ON:
			ArenaTeam targetTeam = holder.getTeam(target);
			if (targetTeam == null || !targetTeam.hasAliveMember(target)) /// We dont care about dead players
				return;
			if (damager == null){ /// damage from some source, its not pvp though. so we dont care
				return;}
			ArenaTeam t = holder.getTeam(damager);
			if (t != null && t.hasMember(target)){ /// attacker is on the same team
				event.setCancelled(true);
			} else {/// different teams... lets make sure they can actually hit
				event.setCancelled(false);
			}
			break;
		case OFF:
			if (damager != null){ /// damage done from a player
                event.setDamage( 0 );
				event.setCancelled(true);
			}
			break;
		default:
			break;
		}
	}
}
