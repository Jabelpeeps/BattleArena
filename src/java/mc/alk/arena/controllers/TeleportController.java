package mc.alk.arena.controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.Permissions;
import mc.alk.arena.listeners.BAPlayerListener;
import mc.alk.arena.listeners.competition.InArenaListener;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.plugins.CombatTagUtil;
import mc.alk.arena.plugins.EssentialsUtil;
import mc.alk.arena.plugins.VanishNoPacketUtil;
import mc.alk.arena.util.Log;

public class TeleportController implements Listener {

    final static Set<UUID> teleporting = Collections.synchronizedSet(new HashSet<UUID>());
    private static final int TELEPORT_FIX_DELAY = 15; // ticks

    public static boolean teleport( Player player, Location location) {
        return teleport(PlayerController.toArenaPlayer(player), location, false);
    }

    public static boolean teleport( ArenaPlayer player, Location location) {
        return teleport(player, location, false);
    }

    public static boolean teleport( ArenaPlayer arenaPlayer, Location location, boolean giveBypassPerms) {
        Player player = arenaPlayer.getPlayer();
        if (Defaults.DEBUG_SPAWNS) {
            Log.info("BattleArena beginning teleport player=" + player.getDisplayName());
        }
        try {
            teleporting.add(arenaPlayer.getUniqueId());
            player.setVelocity(new Vector(0, Defaults.TELEPORT_Y_VELOCITY, 0));
            player.setFallDistance(0);
            Location loc = location.clone();
            loc.setY(loc.getY() + Defaults.TELEPORT_Y_OFFSET);
            player.closeInventory();
            player.setFireTicks(0);
            arenaPlayer.despawnMobs();

            if (player.isInsideVehicle()) player.leaveVehicle();

            if (!loc.getWorld().isChunkLoaded(loc.getBlock().getChunk())) 
                loc.getWorld().loadChunk(loc.getBlock().getChunk());
    
            /// MultiInv and Multiverse-Inventories stores/restores items when changing worlds
            /// or game states ... lets not let this happen
            Permissions.givePlayerInventoryPerms(player);

            /// CombatTag will prevent teleports
            CombatTagUtil.untag(player);

            /// Give bypass perms for Teleport checks like noTeleport, and noChangeWorld
            if (    giveBypassPerms 
                    && BattleArena.getSelf().isEnabled() 
                    && !Defaults.DEBUG_STRESS ) {
                player.addAttachment(BattleArena.getSelf(), Permissions.TELEPORT_BYPASS_PERM, true, 1);
            }

            /// Some worlds "regenerate" which means they have the same name, but are different worlds
            /// To deal with this, reset the world
            World w = Bukkit.getWorld(loc.getWorld().getName());
            Location nl = new Location(w, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            
            if (    !player.teleport( nl, PlayerTeleportEvent.TeleportCause.PLUGIN )
                    || (    Defaults.DEBUG_VIRTUAL 
                            && !player.isOnline() ) ) {
                BAPlayerListener.teleportOnReenter(PlayerController.toArenaPlayer(player), nl, player.getLocation());
                return false;
            }
            arenaPlayer.spawnMobs();

            /// Handle the /back command from Essentials
            if (EssentialsUtil.isEnabled()) {
                
                Location l = BAPlayerListener.getBackLocation(player);
                if (l != null)
                    EssentialsUtil.setBackLocation(player, l);
            }
            if (Defaults.DEBUG_SPAWNS)
                Log.info("BattleArena ending teleport player=" + player.getDisplayName());
        } 
        catch (Exception e) {
            if (!Defaults.DEBUG_VIRTUAL) {
                Log.err("[BA Error] teleporting player=" + player.getDisplayName() + " to " + location + " " + giveBypassPerms);
                Log.printStackTrace(e);
            }
            return false;
        }
        return true;
    }

    /**
     * This prevents other plugins from cancelling the teleport removes the
     * player from the set after allowing the tp. Additionally as a
     *
     * @param event PlayerTeleportEvent
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (teleporting.remove( event.getPlayer().getUniqueId() ) ) {
            event.setCancelled(false);
            
            if (Defaults.ENABLE_TELEPORT_FIX)
                invisbleTeleportWorkaround(event.getPlayer());
        }
    }

    /// TODO remove these work around teleport hacks when bukkit fixes the invisibility on teleport issue
    /// modified from the teleportFix2 found online
    private void invisbleTeleportWorkaround( Player player ) {
        
        int visibleDistance = Bukkit.getViewDistance() * 16;
        // Fix the visibility issue one tick later
        Scheduler.scheduleSynchronousTask( 
                () -> { 
                        if (!player.isOnline()) return; 
                        // Hide every player
                        updateEntities(player, getPlayersWithinDistance(player, visibleDistance), false);
                        
                        // Then show them again
                        Scheduler.scheduleSynchronousTask( () -> 
                                updateEntities( player, getPlayersWithinDistance(player, visibleDistance), true ), 2);
         
                }, TELEPORT_FIX_DELAY);
    }

    void updateEntities( Player tpedPlayer, List<Player> players, boolean visible ) {
        // Hide or show every player to tpedPlayer and hide or show tpedPlayer to every player.
        for (Player player : players) {
            if (!player.isOnline()) continue;

            if (VanishNoPacketUtil.isVanished(player)) {
                if (!InArenaListener.inArena(player)) {
                    continue;
                }
                VanishNoPacketUtil.toggleVanish(player);
            }
            if (visible) {
                tpedPlayer.showPlayer(player);
                player.showPlayer(tpedPlayer);
            } 
            else {
                tpedPlayer.hidePlayer(player);
                player.hidePlayer(tpedPlayer);
            }
        }
    }

    List<Player> getPlayersWithinDistance( Player player, int distance ) {
        List<Player> res = new ArrayList<>();
        int d2 = distance * distance;
        UUID uid = player.getWorld().getUID();
        
        for ( Player p : Bukkit.getOnlinePlayers() ) {
            try {
                if (    p.getWorld().getUID() == uid
                        && p != player 
                        && p.getLocation().distanceSquared( player.getLocation() ) <= d2 ) {
                    res.add(p);
                }
            } 
            catch ( IllegalArgumentException e ) {
                Log.info( e.getMessage() );
            } 
        }
        return res;
    }
}
