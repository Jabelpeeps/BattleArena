package mc.alk.arena.objects.spawns;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;

import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.TeamUtil;

public class EntitySpawn extends SpawnInstance{
    final private EntityType et;
    final List<Entity> uids = new ArrayList<Entity>();
    int number = 1;
    ArenaPlayer owner;

    static Method spawnEntityMethod;
    static {
        try {
            spawnEntityMethod = World.class.getMethod("spawnEntity", Location.class, EntityType.class);
        } catch (Exception e) {
            try {
                spawnEntityMethod = World.class.getMethod("spawnCreature", Location.class, EntityType.class);
            } catch (NoSuchMethodException e1) {
                Log.printStackTrace(e1);
            }
        }
    }

    public EntitySpawn(EntityType et) {
        super(null);
        this.et = et;
    }

    public EntitySpawn(EntityType et,int number) {
        super(null);
        this.et = et;
        this.number =number;
    }

    public EntitySpawn(EntitySpawn entitySpawn) {
        super(null);
        this.et = entitySpawn.et;
        this.number = entitySpawn.number;
    }

    @Override
    public void spawn() {
        if (spawnEntityMethod==null)
            return;
        for (Entity id: uids){
            if (!id.isDead()){
                return;} /// The entities are already spawned
        }
        uids.clear();
        for (int i=0;i< number;i++) {
            try {
                Entity entity = (Entity) spawnEntityMethod.invoke(loc.getWorld(), loc, et);
                if (entity instanceof Wolf && owner != null && owner.getTeam()!=null) {
                    ((Wolf) entity).setCollarColor( TeamUtil.getDyeColor(owner.getTeam().getIndex() ) );
                }
                uids.add(entity);
            } catch (Exception e) {
                Log.printStackTrace(e);
            }
        }
    }

    @Override
    public void despawn() {
        for (Entity id: uids){
            if (!id.isDead()){
                id.remove();
            }
        }
        uids.clear();
    }

    public void setOwner(ArenaPlayer player) {
        this.owner = player;
        this.setOwner(player.getPlayer());
    }

    public void setOwner(AnimalTamer tamer){
        for (Entity le: uids){
            if (!le.isDead()){
                if (le instanceof Tameable){
                    ((Tameable)le).setTamed(true);
                    ((Tameable)le).setOwner(tamer);
                }
                if (le instanceof Wolf){
                    ((Wolf)le).setSitting(false);
                    if (owner != null && owner.getTeam()!=null){
                        ((Wolf) le).setCollarColor( TeamUtil.getDyeColor(owner.getTeam().getIndex()));
                    }
                }
            }
        }
    }

    public String getEntityString() {
        return et.getName();
    }

    public int getNumber() {
        return number;
    }

    @Override
    public String toString(){
        return "[ES "+et +":" + number+"]";
    }

    public void setTarget(LivingEntity entity) {
        for (Entity id: uids){
            if (!id.isDead()){
                if (id instanceof Creature){
                    ((Creature)id).setTarget(entity);
                }
            }
        }
    }
}

