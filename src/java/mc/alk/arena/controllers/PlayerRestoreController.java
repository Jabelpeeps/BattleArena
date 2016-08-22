package mc.alk.arena.controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.Permissions;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.plugins.HeroesController;
import mc.alk.arena.util.EffectUtil;
import mc.alk.arena.util.ExpUtil;
import mc.alk.arena.util.InventoryUtil;
import mc.alk.arena.util.InventoryUtil.PInv;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.MessageUtil;
import mc.alk.arena.util.PlayerUtil;
import mc.alk.arena.util.Util;


public class PlayerRestoreController {
    final ArenaPlayer player;

    @Getter @Setter boolean kill;
    @Getter @Setter boolean clearInventory;
    @Getter @Setter int clearWool = -1; 
    @Getter @Setter Location teleportLocation; 
    @Getter @Setter Location tp2;
    @Getter @Setter Location lastLoc;
    @Getter @Setter Location backLocation;

    @Getter Integer exp;
    @Getter @Setter Double health;
    @Getter @Setter Integer hunger;
    @Getter @Setter Integer magic;
    @Getter @Setter GameMode gamemode;

    @Getter @Setter PInv item;
    @Getter @Setter PInv matchItems;
    @Getter @Setter List<ItemStack> removeItems;
    Collection<PotionEffect> effects;
    @Getter @Setter String message;
    boolean deEnchant;

    public PlayerRestoreController(ArenaPlayer _player) {
        player = _player;
    }

    public synchronized boolean handle(final Player p, PlayerRespawnEvent event) {
        if (message != null) MessageUtil.sendMessage(p, message);
        if (clearInventory) handleClearInventory(p);
        if (kill){
            handleKill(p);
            return stillHandling();
        }
        if (teleportLocation != null) handleTeleport(p, event);
        if (gamemode != null) handleGameMode();
        if (exp != null) handleExp();
        if (health != null) handleHealth();
        if (hunger != null) handleHunger();
        if (magic != null) handleMagic();
        if (item != null) handleItems();
        if (matchItems !=null) handleMatchItems();
        if (removeItems != null) handleRemoveItems();

        if (deEnchant){
            try { 
                EffectUtil.deEnchantAll(p);
            } catch (Exception e) { }
            
            HeroesController.deEnchant(p);
        }
        if (effects !=null) handleEffects();

        return stillHandling();
    }

    private void handleEffects() {
        final Collection<PotionEffect> efs = effects;
        effects = null;

        Scheduler.scheduleSynchronousTask( () -> {
                Player pl = player.regetPlayer();
                if (pl != null) 
                      EffectUtil.enchantPlayer(pl, efs);
        });
    }

    private void handleRemoveItems() {
        final List<ItemStack> items = removeItems;
        removeItems = null;
        
        Scheduler.scheduleSynchronousTask( () -> {
                Player pl = player.regetPlayer();
                if (pl != null)
                    PlayerStoreController.removeItems(PlayerController.toArenaPlayer(pl), items);
        });
    }

    private void handleMatchItems() {
        final PInv items = matchItems;
        matchItems = null;
        
        Scheduler.scheduleSynchronousTask( () -> {
                Player pl = player.regetPlayer();
                if (pl != null)
                    PlayerStoreController.setInventory(PlayerController.toArenaPlayer(pl), items);
        });
    }

    private void handleItems() {
        final PInv items = item;
        item = null;

        Scheduler.scheduleSynchronousTask( () -> {
                Player pl = player.regetPlayer();
                if (pl != null) 
                    PlayerStoreController.setInventory(PlayerController.toArenaPlayer(pl), items);
        });
    }

    private void handleMagic() {
        final int val = magic;
        magic = null;

        Scheduler.scheduleSynchronousTask( () -> {
                Player pl = player.regetPlayer();
                if (pl != null) 
                    HeroesController.setMagicLevel(pl, val);
        });
    }

    private void handleHunger() {
        final int val = hunger;
        hunger = null;
        
        Scheduler.scheduleSynchronousTask( () -> {
                Player pl = player.regetPlayer();
                if (pl != null) 
                    pl.setFoodLevel(val);
        });
    }

    private void handleHealth() {
        final Double val = health;
        health = null;

        Scheduler.scheduleSynchronousTask( () -> {
                Player pl = player.regetPlayer();
                if (pl != null) 
                    pl.setHealth(val);
        });
    }

    private void handleExp() {
        final int val = exp;
        exp = null;

        Scheduler.scheduleSynchronousTask( () -> {
                Player pl = player.regetPlayer();
                if (pl != null) 
                    ExpUtil.setTotalExperience(pl, val);
        });
    }

    private void handleGameMode() {
        final GameMode gm = gamemode;
        gamemode = null;

        Scheduler.scheduleSynchronousTask( () -> {
                Player pl = player.regetPlayer();
                if (pl != null) 
                    PlayerUtil.setGameMode(pl, gm);
        });
    }

    private void handleTeleport( Player p, PlayerRespawnEvent event) {
        Location loc = teleportLocation;
        tp2 = loc;
        teleportLocation = null;
        if (loc != null) {
            if (event == null){

                Scheduler.scheduleSynchronousTask( () -> {
                        Player pl = player.regetPlayer();
                        if (pl != null) 
                            TeleportController.teleport(pl, loc);
                        else 
                            Util.printStackTrace();
                });
            } 
            else {
                Permissions.givePlayerInventoryPerms(p);
                event.setRespawnLocation(loc);
                /// Set a timed event to check to make sure the player actually arrived. Then do a teleport if needed
                /// This can happen on servers where plugin conflicts prevent the respawn (somehow!!!)
                if (HeroesController.enabled()){

                    Scheduler.scheduleSynchronousTask( () -> {
                            Player pl = player.regetPlayer();
                            if (pl != null){
                                if (pl.getLocation().getWorld().getUID()!=loc.getWorld().getUID() ||
                                        pl.getLocation().distanceSquared(loc) > 100){
                                    TeleportController.teleport(p, loc);
                                }
                            }
                            else Util.printStackTrace();
                    },2L);
                }
            }
        } else { /// this is bad, how did they get a null tp loc
            Log.err(player.getName() + " respawn loc =null");
        }
    }

    private boolean stillHandling() {
        return clearInventory || kill ||clearWool!=-1||teleportLocation!=null || tp2 != null || lastLoc!=null||
                exp != null || health!=null || hunger!=null || magic !=null || gamemode!=null || item!=null ||
                matchItems!=null||removeItems!=null||message!=null || backLocation!=null || effects!=null;
    }

    private void handleKill(Player p) {
        MessageUtil.sendMessage(p, "&eYou have been killed by the Arena for not being online");
        p.setHealth(0);
    }

    private void handleClearInventory(Player p) {
        Log.warn("[BattleArena] clearing inventory for quitting during a match " + p.getName() );
        InventoryUtil.clearInventory(p);
    }
    public void addExp(Integer _exp) {
        if (exp == null)
            exp = _exp;
        else
            exp += _exp;
    }

    public void deEnchant() { deEnchant = true; }
    public void removeMatchItems() { matchItems = null; }

    public void addRemoveItem(ItemStack is) {
        if (removeItems == null){
            removeItems = new ArrayList<>();}
        removeItems.add(is);
    }

    public void addRemoveItem(List<ItemStack> itemsToRemove) {
        if (removeItems == null){
            removeItems = new ArrayList<>();}
        removeItems.addAll(itemsToRemove);
    }
 
    public void enchant( Collection<PotionEffect> _effects) { effects = _effects; }
    public UUID getUUID() { return player.getUniqueId(); }
}
