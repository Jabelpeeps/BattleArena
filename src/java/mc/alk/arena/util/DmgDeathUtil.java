package mc.alk.arena.util;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.projectiles.ProjectileSource;

import mc.alk.arena.controllers.PlayerController;
import mc.alk.arena.objects.ArenaPlayer;

public class DmgDeathUtil {

	public static ArenaPlayer getPlayerCause( PlayerDeathEvent event ) {
		return getPlayerCause( event.getEntity().getLastDamageCause() );
	}

	public static ArenaPlayer getPlayerCause(EntityDamageEvent lastDamageCause) {
		if (lastDamageCause == null) return null;
		
		if ( !(lastDamageCause instanceof EntityDamageByEntityEvent) ) return null;

		Entity entityLastDamage = ((EntityDamageByEntityEvent) lastDamageCause).getDamager();
		
		return getPlayerCause( entityLastDamage );
	}

	public static ArenaPlayer getPlayerCause(Entity lastDamageCause) {
		if (lastDamageCause == null) return null;
		
		if ( lastDamageCause instanceof Player ) { 
            return PlayerController.toArenaPlayer((Player) lastDamageCause);
        }		 
        else if ( lastDamageCause instanceof Projectile ) { 
		    
			ProjectileSource shooter = ((Projectile) lastDamageCause).getShooter();
			
			if ( shooter instanceof Player) { 			    
				return PlayerController.toArenaPlayer( (Player) shooter );
			}
		} 
		return null;
	}
}
