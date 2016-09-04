package mc.alk.arena.listeners.custom;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.util.DmgDeathUtil;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.MapOfTreeSet;


/**
 *
 * @author alkarin
 *
 */
class SpecificPlayerEventListener extends BaseEventListener {
    /** map of player to listeners listening for that player */
    final protected MapOfTreeSet<UUID,RListener> listeners = new MapOfTreeSet<>( RListener.class );

    /** The method which will return a Player if invoked */
    final Method getPlayerMethod;

    /**
     * Construct a listener to listen for the given bukkit event
     * @param bukkitEvent : which event we will listen for
     * @param _getPlayerMethod : a method which when not null and invoked will return a Player
     */
    public SpecificPlayerEventListener( Class<? extends Event> bukkitEvent,
                                        EventPriority _bukkitPriority, 
                                        Method _getPlayerMethod ) {
        super(bukkitEvent, _bukkitPriority);
        if (Defaults.DEBUG_EVENTS) 
            Log.info("Registering GenericPlayerEventListener for type " + bukkitEvent +" pm="+_getPlayerMethod);
        getPlayerMethod = _getPlayerMethod;
    }

    /**
     * Does this event even have any listeners
     * @return true if has listeners
     */
    @Override
    public boolean hasListeners(){
        return !listeners.isEmpty() ;
    }

    /**
     * Get the map of players to listeners
     * @return players
     */
    public MapOfTreeSet<UUID, RListener> getListeners(){
        return listeners;
    }

    /**
     * Returns the players being listened for in this event
     * @return players
     */
    public Collection<UUID> getPlayers(){
        return listeners.keySet();
    }

    /**
     * Add a player listener to this bukkit event
     * @param rl RListener
     * @param players the players
     */
    public void addListener(RListener rl, Collection<UUID> players) {
        if (Defaults.DEBUG_EVENTS) 
            Log.info( "--adding listener   players=" + players + " listener=" + rl + "  " +
                ((players != null && rl.hasSpecificPlayerMethod()) ? " MatchListener" 
                                                                  : " SpecificPlayerListener" ));
        for (UUID player: players)
            addSPListener(player, rl);
    }

    /**
     * remove a player listener from this bukkit event
     * @param rl RListener
     * @param players the players
     */
    public synchronized void removeListener(RListener rl, Collection<UUID> players) {
        if (Defaults.DEBUG_EVENTS) System.out.println("    removing listener  player="+players+"   listener="+rl);

        for (UUID player: players){
            removeSPListener(player, rl);}
    }

    @Override
    public synchronized void removeAllListeners(RListener rl) {
        if (Defaults.DEBUG_EVENTS) System.out.println("    removing all listeners  listener="+rl);
        synchronized(listeners){
            for (UUID name : listeners.keySet()){
                listeners.removeValue(name, rl);
            }
        }
    }

    /**
     * Add a listener for a specific player
     * @param p player
     * @param spl RListener
     */
    public void addSPListener(UUID p, RListener spl) {
        if (!isListening()){
            startListening();}
        listeners.add(p,spl);
    }

    /**
     * Remove a listener for a specific player
     * @param p the player
     * @param spl RListener
     * @return player was removed from collection
     */
    public boolean removeSPListener(UUID p, RListener spl) {
        final boolean removed = listeners.removeValue(p,spl);
        
        if (removed && !hasListeners() && isListening())
            stopListening();
        return removed;
    }

    /**
     * do the bukkit event for players
     * @param event Event
     */
    @Override
    public void invokeEvent( Event event ) {
        /// Need special handling of Methods that have 2 entities involved, as either entity may be in a match
        /// These currently use getClass() for speed and the fact that there aren't bukkit
        /// subclasses at this point.  These would need to change to instanceof if subclasses are created
        if (event.getClass() == EntityDamageByEntityEvent.class){
            doEntityDamageByEntityEvent((EntityDamageByEntityEvent)event);
            return;
        } 
        else if (event.getClass() == EntityDeathEvent.class){
            doEntityDeathEvent((EntityDeathEvent)event);
            return;
        } 
        else if (event instanceof EntityDamageEvent){
            doEntityDamageEvent((EntityDamageEvent)event);
            return;
        }

        Entity entity = getEntityFromMethod(event, getPlayerMethod);
        if (!(entity instanceof Player))
            return;
        doMethods(event, (Player) entity);
    }

    private void doMethods( Event _event, Player p ) {
        RListener[] lmethods = listeners.getSafe( p.getUniqueId() );
        if (lmethods == null) return;
        
        /// For each of the splisteners methods that deal with this BukkitEvent
        for(RListener lmethod: lmethods){
            try {
                lmethod.getMethod().getCallMethod().invoke(lmethod.getListener(), _event); 
            } 
            catch (Exception e){
                Log.err("["+BattleArena.getNameAndVersion()+" Error] method=" + lmethod.getMethod().getCallMethod() +
                        ",  types.length=" +lmethod.getMethod().getCallMethod().getParameterTypes().length +
                        ",  p=" + p +",  listener="+lmethod);
                Log.printStackTrace(e);
            }
        }
    }

    private void doEntityDeathEvent( EntityDeathEvent _event ) {
        if (_event.getEntity() instanceof Player &&
                listeners.containsKey( ( (Player) _event.getEntity() ).getUniqueId() ) ){
            doMethods(_event, (Player) _event.getEntity());
            return;
        }
        ArenaPlayer ap = DmgDeathUtil.getPlayerCause(_event.getEntity().getLastDamageCause());
        if (ap == null)
            return;
        if (listeners.containsKey(ap.getUniqueId())){
            doMethods(_event, ap.getPlayer());
        }
    }

    private void doEntityDamageByEntityEvent( EntityDamageByEntityEvent _event ) {
        if (_event.getEntity() instanceof Player &&
                listeners.containsKey( ( (Player) _event.getEntity() ).getUniqueId() ) ) {
            doMethods(_event, (Player) _event.getEntity());
            return;
        }
        if (_event.getDamager() instanceof Player &&
                listeners.containsKey( ( (Player) _event.getEntity() ).getUniqueId() ) ) {
            doMethods(_event, (Player) _event.getDamager());
            return;
        }

        Player player = null;
        if (_event.getDamager() instanceof Projectile){ 
            Projectile proj = (Projectile) _event.getDamager();
            if (proj.getShooter() instanceof Player){ 
                player= (Player) proj.getShooter();
            }
        }
        if (player != null){
            doMethods(_event, player);
        }
        /// Else the target wasnt a player, and the shooter wasnt a player.. nothing to do
    }

    private void doEntityDamageEvent( EntityDamageEvent _event ) {
        if (_event.getEntity() instanceof Player){
            doMethods(_event, (Player) _event.getEntity());
            return;
        }

        EntityDamageEvent lastDamage = _event.getEntity().getLastDamageCause();
        ArenaPlayer damager = DmgDeathUtil.getPlayerCause(lastDamage);
        if (damager != null){
            doMethods(_event, damager.getPlayer());
        }
    }

    private Entity getEntityFromMethod( Event _event, Method method) {
        try {
            Object o = method.invoke(_event);
            if (o instanceof Entity)
                return (Entity) o;
            return null;
        }
        catch(Exception e){
            Log.printStackTrace(e);
            return null;
        }
    }
}
