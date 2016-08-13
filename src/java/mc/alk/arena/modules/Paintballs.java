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
public class Paintballs extends ArenaModule {
    
    @Getter String name = "Paintballs";
    @Getter String version = "1.0";
    EntityType paintballs;
    double damage;
    
    public Paintballs() {
        this( EntityType.SNOWBALL );
    }
    
    public Paintballs( EntityType paintballType ) {
        this( paintballType, 20 );
    }
    
    public Paintballs( EntityType paintballType, double damageAmount ) {
        paintballs = paintballType;
        damage = damageAmount;
    }

    @ArenaEventHandler
    public void onPlayerInteract( PlayerInteractEvent e ) {
        e.getPlayer().sendMessage( "Module: Paintballs" );
    }

    @ArenaEventHandler
    public void onEntityDamage( EntityDamageByEntityEvent event ) {
        
        if ( event.getDamager().getType() == EntityType.SNOWBALL ) {
            event.setDamage( damage );
        }        
    }
}
