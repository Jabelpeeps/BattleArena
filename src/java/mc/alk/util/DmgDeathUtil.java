package mc.alk.util;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import mc.alk.arena.controllers.PlayerController;
import mc.alk.arena.objects.ArenaPlayer;

public class DmgDeathUtil {

	public static ArenaPlayer getPlayerCause(PlayerDeathEvent event) {
		return getPlayerCause(event.getEntity().getLastDamageCause());
	}

	public static ArenaPlayer getPlayerCause(EntityDamageEvent lastDamageCause) {
		if (lastDamageCause == null)
			return null;
		if (!(lastDamageCause instanceof EntityDamageByEntityEvent))
			return null;
		final EntityDamageByEntityEvent edbee = (EntityDamageByEntityEvent) lastDamageCause;
		final Entity entityLastDamage = edbee.getDamager();
		return entityLastDamage != null ? getPlayerCause(entityLastDamage) : null;
	}

	public static ArenaPlayer getPlayerCause(Entity lastDamageCause) {
		if (lastDamageCause == null)
			return null;
		if (lastDamageCause instanceof Projectile) { /// we have some sort of projectile
			Projectile proj = (Projectile) lastDamageCause;
			if (proj.getShooter() instanceof Player){ /// projectile was shot by a player
				return PlayerController.toArenaPlayer((Player) proj.getShooter());
			}
            return null;
		} else if (! (lastDamageCause instanceof Player)) { /// killer not player
			return null;
		} else { /// Killer is a player
			return PlayerController.toArenaPlayer((Player) lastDamageCause);
		}
	}

}
