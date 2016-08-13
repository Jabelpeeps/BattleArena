package mc.alk.arena.modules;

import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import lombok.Getter;
import mc.alk.arena.objects.events.ArenaEventHandler;
import mc.alk.arena.objects.modules.ArenaModule;

/**
 * 
 * 
 * @author Nikolai
 */
public class Grenades extends ArenaModule {
    
    @Getter String name = "Grenades";
    @Getter String version = "1.0";
    EntityType grenades;
    double damage;
    
    public Grenades() {
        this( EntityType.EGG );
    }
    
    public Grenades( EntityType grenadeType ) {
        this( grenadeType, 20.0 );
    }
    public Grenades(EntityType grenadeType, double damageAmount) {
        grenades = grenadeType;
        damage = damageAmount;
    }
    
    @ArenaEventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        e.getPlayer().sendMessage("Module: Grenades");
    }
    
    @ArenaEventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {

        if ( event.getDamager().getType() == grenades ) {
            event.setDamage(damage);
        }        
    }
}
