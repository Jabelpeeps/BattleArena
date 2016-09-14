package mc.alk.arena.listeners;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import lombok.AllArgsConstructor;
import mc.alk.arena.Defaults;
import mc.alk.arena.objects.tracker.Stat;
import mc.alk.arena.objects.tracker.WLTRecord.WLT;
import mc.alk.arena.tracker.Tracker;
import mc.alk.arena.tracker.TrackerConfigController;
import mc.alk.arena.tracker.TrackerInterface;
import mc.alk.arena.tracker.TrackerMessageController;
import mc.alk.arena.util.MessageUtil;


public class BTEntityListener implements Listener {
	static final String UNKNOWN = "unknown";
	ConcurrentHashMap<String,Long> lastDamageTime = new ConcurrentHashMap<>();
	ConcurrentHashMap<String,RampageStreak> lastKillTime = new ConcurrentHashMap<>();
	static HashSet<String> ignoreEntities = new HashSet<>();
	static HashSet<UUID> ignoreWorlds = new HashSet<>();

	Random r = new Random();
	TrackerInterface playerTi;
	TrackerInterface worldTi;
	int count = 0;

	@AllArgsConstructor
	class RampageStreak {
		Long time; int nkills;
	}

	public BTEntityListener() {
		playerTi = Tracker.getPVPInterface();
		worldTi = Tracker.getPVEInterface();
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDeath(PlayerDeathEvent event) {
		ede(event);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDeath(EntityDeathEvent event) {
		if (event instanceof PlayerDeathEvent) return;
		
		/// we have a player killing a mob, if we are not tracking pve
		/// we don't need to enter, no messages are usually sent for this
		if ( !TrackerConfigController.getBoolean( "trackPvE", false ) ) return;
		
		ede(event);
	}

	private void ede(EntityDeathEvent event) {
		if (ignoreWorlds.contains(event.getEntity().getWorld().getUID())) return;
		
		String target, killer;
		boolean targetPlayer = false, killerPlayer = false;
		boolean isMelee = true;
		ItemStack killingWeapon = null;

		/// Get our hapless target
		Entity targetEntity = event.getEntity();
		if (targetEntity instanceof Player){
			target = ((Player)targetEntity).getName();
			targetPlayer = true;
		} else if (targetEntity instanceof Tameable){
			target = "Tamed" + targetEntity.getType().getEntityClass().getSimpleName();
		} else {
			target = targetEntity.getType().getEntityClass().getSimpleName();
		}
		/// Should we be tracking this person
		if (targetPlayer && Tracker.notTracked(target) ){
			if (event instanceof PlayerDeathEvent)
				((PlayerDeathEvent) event).setDeathMessage(""); /// Set to none, will cancel all non pvp messages
			return;
		}
		if (  !targetPlayer 
		        && !TrackerConfigController.getBoolean( "trackPvP", false ) 
		        && !TrackerConfigController.getBoolean( "sendPVPDeathMessages", false ) ) {
			return;
		}

		/// Determine our killer
		EntityDamageEvent lastDamageCause = targetEntity.getLastDamageCause();
		Player killerEntity = null;
		if (lastDamageCause instanceof EntityDamageByEntityEvent){
			Entity damager = ((EntityDamageByEntityEvent) lastDamageCause).getDamager();
			
			if (damager instanceof Player) { /// killer is player
				killerEntity = (Player) damager;
				killer = killerEntity.getName();
				killerPlayer = true;
				killingWeapon = killerEntity.getItemOnCursor();
			} 
			else if (damager instanceof Projectile) { /// we have some sort of projectile
				isMelee = false;
				Projectile proj = (Projectile) damager;
				if (proj.getShooter() instanceof Player){ /// projectile was shot by a player
					killerPlayer = true;
					killerEntity = (Player) proj.getShooter();
					killer = killerEntity.getName();
					killingWeapon = killerEntity.getItemOnCursor();
				} 
				else if (proj.getShooter() != null){ /// projectile shot by some mob, or other source
					killer = proj.getShooter().getClass().getSimpleName();
				} 
				else {
					killer = UNKNOWN; /// projectile was null?
				}
			} 
			else if (damager instanceof Tameable && ((Tameable) damager).isTamed()) {
				AnimalTamer at = ((Tameable) damager).getOwner();
				if (at != null){
					if (at instanceof Player){
						killerPlayer = true;
						killerEntity = (Player) at;
					}
					killer = at.getName();
				} 
				else {
					killer = damager.getType().getEntityClass().getSimpleName();
				}
			} 
			else { 
				killer = damager.getType().getEntityClass().getSimpleName();
			}
		} else {
			if (lastDamageCause == null || lastDamageCause.getCause() == null)
				killer  = UNKNOWN;
			else
				killer = lastDamageCause.getCause().name();
		}
        if (killer == null) killer = UNKNOWN;
        
		if (killerPlayer && Tracker.notTracked(killer)) return;
		
		if (    ignoreEntities.contains(killer) 
		        || ignoreEntities.contains(targetEntity.toString()) 
		        || ignoreEntities.contains(target) )
			return;

		if (targetPlayer && killerPlayer){

			if (TrackerConfigController.getBoolean("trackPvP",true) )
				addRecord(playerTi,killer,target,WLT.WIN);
			
			PlayerDeathEvent pde = (PlayerDeathEvent) event;

			if (!Defaults.PVP_MESSAGES && !Defaults.BUKKIT_PVP_MESSAGES){

				if (Defaults.INVOLVED_PVP_MESSAGES){
					String msg = getPvPDeathMessage(killer,target,isMelee,playerTi,killingWeapon);
					sendMessage(killerEntity, (Player)targetEntity,msg);
				} 
				else {
					sendMessage(pde,null);
				}
				return;
			}

			if (Defaults.PVP_MESSAGES) {
				sendMessage(pde, getPvPDeathMessage(killer,target,isMelee,playerTi,killingWeapon) );
			}
		} 
		else if ( !targetPlayer && !killerPlayer) { 

		} 
		else { 
			if ( !killerPlayer && killer.contains("Craft") ) 
				killer = killer.substring(5);
			
			if (!targetPlayer && target.contains("Craft"))
				target = target.substring(5);

			if (TrackerConfigController.getBoolean("trackPvE",true))
				addRecord(worldTi, killer,target,WLT.WIN);

			if (targetPlayer && event instanceof PlayerDeathEvent){

				PlayerDeathEvent pde = (PlayerDeathEvent) event;
				if (!Defaults.PVE_MESSAGES && !Defaults.BUKKIT_PVE_MESSAGES){
					if (Defaults.INVOLVED_PVE_MESSAGES){
						final String wpn = killingWeapon != null ? killingWeapon.getType().name().toLowerCase() : null;
						sendMessage( null,(Player) targetEntity,getPvEDeathMessage(killer,target,isMelee,wpn) );
					} 
					else {
						pde.setDeathMessage(null);
					}
					return;
				}

				if (Defaults.PVE_MESSAGES){
					final String wpn = killingWeapon != null ? killingWeapon.getType().name().toLowerCase() : null;
					sendMessage( pde, getPvEDeathMessage(killer,target,isMelee,wpn) );
				}
			}
		}
	}

	private void sendMessage(Player killerEntity, Player targetEntity, String msg) {
		if (killerEntity != null){
		    MessageUtil.sendMessage(killerEntity, msg);}
		if (targetEntity != null){
		    MessageUtil.sendMessage(targetEntity, msg);}
	}

	private void sendMessage(PlayerDeathEvent event, String msg){
		if (msg == null){
			event.setDeathMessage(null);
			return;
		}

		if (Defaults.RADIUS <= 0){
			event.setDeathMessage(msg);
		} 
		else {
			Player player = event.getEntity();
			if (player == null){
				event.setDeathMessage(msg);
				return;
			}
			Location l = player.getLocation();
			if (l==null){
				event.setDeathMessage(msg);
				return;
			}
			event.setDeathMessage(null);
			UUID wid = l.getWorld().getUID();
			Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
			Player players[] = onlinePlayers.toArray(new Player[onlinePlayers.size()]);

			for (Player p: players){
				if (wid != p.getLocation().getWorld().getUID()  || p.getLocation().distanceSquared(l) >= Defaults.RADIUS){
					continue;}
				p.sendMessage(msg);
			}
		}
	}

	public String getPvPDeathMessage(String killer, String target, boolean isMeleeDeath,
			                                        TrackerInterface ti, ItemStack killingWeapon) {

		try {
			RampageStreak lastKill = lastKillTime.get(killer);
			long now = System.currentTimeMillis() ;
			if (lastKill != null && now - lastKill.time < Defaults.RAMPAGE_TIME){
				lastKill.nkills++;
				lastKill.time = now;
				return TrackerMessageController.getSpecialMessage(TrackerMessageController.SpecialType.RAMPAGE, lastKill.nkills, killer,target, killingWeapon);
			}
            lastKillTime.put(killer, new RampageStreak(now, 1));
            
		} catch (Exception e){ }

		Stat stat = ti.loadPlayerRecord(killer);
		final int streak = stat.getStreak();
		boolean hasStreak = TrackerMessageController.contains("special.streak." + streak );
		
		/// they are on a streak
		if ( hasStreak || ( streak != 0 && Defaults.STREAK_EVERY != 0 && streak % Defaults.STREAK_EVERY == 0 ) ){
			return TrackerMessageController.getSpecialMessage(TrackerMessageController.SpecialType.STREAK, streak, killer,target, killingWeapon);
		}
        return TrackerMessageController.getPvPMessage(isMeleeDeath,killer, target, killingWeapon);
	}

	public String getPvEDeathMessage(String p1, String p2, boolean isMeleeDeath, String killingWeapon){
		return TrackerMessageController.getPvEMessage(isMeleeDeath, p1, p2,killingWeapon);
	}

	public static void addRecord(final TrackerInterface ti,final String e1, final String e2, final WLT record){
		ti.addPlayerRecord(e1, e2, record);
	}

	public static void setIgnoreEntities(List<String> list) {
		ignoreEntities.clear();
		if (list != null)
			ignoreEntities.addAll(list);
	}

	public static void setIgnoreWorlds(List<String> list) {
		ignoreWorlds.clear();
		if (list != null){
			for (String s: list){
				World w = Bukkit.getWorld(s);
				if (w != null){
					ignoreWorlds.add(w.getUID());}
			}
		}
	}
}
