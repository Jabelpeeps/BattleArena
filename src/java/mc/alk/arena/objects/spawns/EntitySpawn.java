package mc.alk.arena.objects.spawns;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;

import lombok.Getter;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.util.TeamUtil;

public class EntitySpawn extends SpawnInstance {
    final private EntityType et;
    final List<Entity> entities = new ArrayList<>();
    @Getter int number = 1;
    ArenaPlayer owner;

    public EntitySpawn(EntityType _et) {
        super(null);
        et = _et;
    }

    public EntitySpawn(EntityType _et, int _number ) {
        super(null);
        et = _et;
        number = _number;
    }

    public EntitySpawn(EntitySpawn entitySpawn) {
        super(null);
        et = entitySpawn.et;
        number = entitySpawn.number;
    }

    @Override
    public void spawn() {

        for ( Entity id : entities ) {
            if (!id.isDead())
                return; 
        }
        entities.clear();
        
        for ( int i = 0; i < number; i++ ) {

            Entity entity = Bukkit.getWorld( location.getWorld().getUID() ).spawnEntity( location, et );
            
            if ( entity instanceof Wolf && owner != null && owner.getTeam() != null ) {
                ((Wolf) entity).setCollarColor( TeamUtil.getDyeColor(owner.getTeam().getIndex() ) );
            }
            entities.add(entity);
        }
    }

    @Override
    public void despawn() {
        for ( Entity id : entities ) {
            if ( !id.isDead() ) {
                id.remove();
            }
        }
        entities.clear();
    }

    public void setOwner(ArenaPlayer player) {
        owner = player;
        setOwner(player.getPlayer());
    }

    public void setOwner(AnimalTamer tamer){
        for ( Entity le : entities ) {
            if ( !le.isDead() ) {
                if ( le instanceof Tameable ) {
                    ((Tameable)le).setTamed(true);
                    ((Tameable)le).setOwner(tamer);
                }
                if ( le instanceof Wolf ) {
                    ((Wolf)le).setSitting(false);
                    if (owner != null && owner.getTeam()!=null){
                        ((Wolf) le).setCollarColor( TeamUtil.getDyeColor(owner.getTeam().getIndex()));
                    }
                }
            }
        }
    }

    public String getEntityString() { return et.toString(); }
    @Override
    public String toString() { return "[ES " + et + ":" + number + "]"; }

    public void setTarget( LivingEntity entity ) {
        for ( Entity id : entities ) {
            if ( !id.isDead() && id instanceof Creature )
                    ((Creature)id).setTarget( entity );
        }
    }
}

