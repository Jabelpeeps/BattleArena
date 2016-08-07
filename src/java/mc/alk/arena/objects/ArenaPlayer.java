package mc.alk.arena.objects;

import java.util.List;
import java.util.Stack;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.PlayerInventory;

import mc.alk.arena.competition.Competition;
import mc.alk.arena.controllers.containers.AreaContainer;
import mc.alk.arena.controllers.plugins.HeroesController;
import mc.alk.arena.controllers.plugins.TrackerController;
import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.objects.meta.PlayerMetaData;
import mc.alk.arena.objects.spawns.EntitySpawn;
import mc.alk.arena.objects.spawns.FixedLocation;
import mc.alk.arena.objects.spawns.SpawnInstance;
import mc.alk.arena.objects.spawns.SpawnLocation;
import mc.alk.arena.objects.stats.ArenaStat;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.util.PermissionsUtil;
import mc.alk.arena.util.PlayerUtil;
import mc.alk.arena.util.ServerUtil;


public class ArenaPlayer {
    
    static int count = 0;
    final int id = count++;

    /**
     * Which competitions is the player inside
     * This can be up to 2, in cases of a tournament or a reserved arena event
     * where they have the event, and the match
     * The stack order is the order in which they joined, the top being the most recent
     */
    final Stack<Competition> competitions = new Stack<>();

    Arena arena;

    /** Which class did the player pick during the competition */
    ArenaClass preferredClass;

    /** Which class is the player currently */
    ArenaClass currentClass;

    /** The players old location, from where they were first teleported*/
    SpawnLocation oldLocation;

    /** The players team, *this is not their self formed team* */
    ArenaTeam arenaTeam;

    /** The current location of the player (in arena, lobby, etc)*/
    ArenaLocation curLocation = new ArenaLocation(AreaContainer.HOMECONTAINER, null , LocationType.HOME);

    List<SpawnInstance> mobs;

    /** Has the player specified they are "ready" by clicking a block or sign */
    boolean isReady;

    final PlayerMetaData meta = new PlayerMetaData();
    final UUID uuid;
    final Player player;

    LivingEntity curTarget;

    public ArenaPlayer( Player _player ) {
        player = _player;
        uuid = _player.getUniqueId();
    }

    public ArenaPlayer( UUID _uuid ) {
        uuid = _uuid;
        player = Bukkit.getPlayer( _uuid );
    }

    public String getName() {
        return player.getName();
    }

    public void reset() {
        isReady = false;
        currentClass = null;
        preferredClass = null;
        if ( mobs != null ) {
            despawnMobs();
            mobs.clear();
        }
    }

    public void setHealth(double health) {
        PlayerUtil.setHealth(player,health);
    }
        
    public Player getPlayer() { return player; }
    public boolean isOnline() { return player.isOnline(); }
    public double getHealth() { return player.getHealth(); }
    public int getFoodLevel() { return player.getFoodLevel(); }
    public void setFoodLevel(int hunger) { player.setFoodLevel(hunger); }
    public String getDisplayName() { return player.getDisplayName(); }
    public void sendMessage(String colorChat) { player.sendMessage(colorChat); }
    public Location getLocation() { return player.getLocation(); }
    public EntityDamageEvent getLastDamageCause() { return player.getLastDamageCause(); }
    public void setFireTicks(int i) { player.setFireTicks(i); }
    public boolean isDead() { return player.isDead(); }
    public PlayerInventory getInventory() { return player.getInventory(); }
    public boolean hasPermission(String perm) { return player.hasPermission(perm); }

    
    public boolean isReady() { return isReady; }
    public void setReady(boolean _isReady) { isReady = _isReady; }
    public ArenaClass getCurrentClass() { return currentClass; }
    public void setCurrentClass(ArenaClass arenaClass) { currentClass = arenaClass; }
    public ArenaClass getPreferredClass() { return preferredClass; }
    public void setPreferredClass(ArenaClass arenaClass) { preferredClass = arenaClass; }

    public int getPriority() {
        return PermissionsUtil.getPriority(player);
    }

    public int getLevel() {
        return (HeroesController.enabled()) ? HeroesController.getLevel(player) : player.getLevel();
    }

    public Competition getCompetition() {
        return competitions.isEmpty() ? null : competitions.peek();
    }

    public void addCompetition(Competition competition) {
        if (!competitions.contains(competition))
            competitions.push(competition);
    }

    public boolean removeCompetition(Competition competition) {
        return competitions.remove(competition);
    }

    /**
     * Returns their current team, based on whichever competition is top of the stack
     * This is NOT a self made team, only the team from the competition
     * @return Team, or null if they are not inside a competition
     */
    public ArenaTeam getTeam() {
        if (arenaTeam != null)
            return arenaTeam;
        return competitions.isEmpty() ? null : competitions.peek().getTeam(this);
    }

    public void setTeam(ArenaTeam team) {
        arenaTeam = team;
    }

    /**
     * Sets the players oldLocation to the current spot ONLY if not already set
     */
    public void markOldLocation(){
        if (oldLocation == null){
            oldLocation = new FixedLocation(getLocation());}
    }

    public void clearOldLocation(){
        oldLocation = null;
    }

    public SpawnLocation getOldLocation(){
        return oldLocation;
    }

    public void setCurLocation(ArenaLocation type){
        this.curLocation = type;
    }

    public ArenaLocation getCurLocation(){
        return this.curLocation;
    }

    public void despawnMobs(){
        if (mobs != null){
            for (SpawnInstance es: mobs){
                es.despawn();}
        }
    }

    public void setMobs(List<SpawnInstance> mobs){
        this.mobs = mobs;
    }

    public void spawnMobs(){
        if (mobs == null) {
            return;}
        for (SpawnInstance es: mobs){
            es.despawn();
            es.setLocation(this.getLocation());
            es.spawn();
            if (es instanceof EntitySpawn) {
                ((EntitySpawn) es).setOwner(this);
            }
        }
    }

    public PlayerMetaData getMetaData(){
        return meta;
    }

    public ArenaStat getStat(MatchParams type) {
        return TrackerController.loadRecord(type, this);
    }

    public Player regetPlayer() {
        return ServerUtil.findPlayerExact(this.getName());
    }

    @Override
    public String toString() {
        return "[" + this.getName() + "]";
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public void setTarget(LivingEntity entity) {
        curTarget = entity;
        if (mobs == null) {
            return;
        }
        for (SpawnInstance es: mobs){
            if (es instanceof EntitySpawn) {
                ((EntitySpawn) es).setTarget(entity);
            }
        }
    }

    public LivingEntity getTarget() {
        return curTarget;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ArenaPlayer &&
                ((ArenaPlayer) obj).id == this.id;
    }
    @Override
    public int hashCode() {
        return id;
    }
}
