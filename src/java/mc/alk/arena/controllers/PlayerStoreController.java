package mc.alk.arena.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import mc.alk.arena.Defaults;
import mc.alk.arena.listeners.BAPlayerListener;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.PlayerSave;
import mc.alk.arena.objects.regions.WorldGuardRegion;
import mc.alk.arena.plugins.HeroesController;
import mc.alk.arena.plugins.WorldGuardController;
import mc.alk.arena.util.EffectUtil;
import mc.alk.arena.util.InventoryUtil;
import mc.alk.arena.util.InventoryUtil.PInv;
import mc.alk.arena.util.Log;

public class PlayerStoreController {
    public static final PlayerStoreController INSTANCE = new PlayerStoreController();
    private final static HashMap<UUID, PlayerSave> saves = new HashMap<>();
    
    private PlayerStoreController(){}

    public static PlayerStoreController save( PlayerSave save ) {
        saves.put( save.getUUID(), save );
        return INSTANCE;
    }
    
    private PlayerSave getOrCreateSave( ArenaPlayer player ) {
        PlayerSave save = saves.get( player.getUniqueId() );
        if ( save != null ) return save;
        
        save = new PlayerSave(player);
        saves.put( player.getUniqueId(), save );
        return save;
    }

    public void restoreAll( ArenaPlayer player ) {
        restoreEffects(player);
        restoreExperience(player);
        restoreFlight(player);
        restoreGamemode(player);
        restoreHealth(player);
        restoreHeroClass(player);
        restoreHunger(player);
        restoreItems(player);
        restoreMagic(player);
        restoreMoney(player);
        restoreMatchItems(player);
        restoreScoreboard(player);
    }

    private PlayerSave getSave( ArenaPlayer player ) { return saves.get( player.getUniqueId() ); }
    private static boolean restoreable( ArenaPlayer p ) { return p.isOnline() && !p.isDead(); }
    public int storeExperience( ArenaPlayer player ) { return getOrCreateSave( player ).storeExperience(); }
    public void storeHealth( ArenaPlayer player ) { getOrCreateSave( player ).storeHealth(); }
    public void storeHunger( ArenaPlayer player ) { getOrCreateSave( player ).storeHunger(); }
    public void storeEffects( ArenaPlayer player ) { getOrCreateSave( player ).storeEffects(); }
    public void storeMagic( ArenaPlayer player ) { getOrCreateSave( player ).storeMagic(); }
    public void storeItems( ArenaPlayer player ) { getOrCreateSave( player ).storeItems(); }
    /**
     * Warning!!! Unlike most other methods in the StoreController, this one
     * overwrites previous values
     * @param player ArenaPlayer
     */
    public void storeMatchItems( ArenaPlayer player ) { getOrCreateSave( player ).storeMatchItems(); }
    public void storeFlight( ArenaPlayer p ) { getOrCreateSave( p ).storeFlight(); }
    public void storeGodmode( ArenaPlayer p ) { getOrCreateSave( p ).storeGodmode(); }
    public void storeGamemode( ArenaPlayer p ) { getOrCreateSave( p ).storeGamemode(); }
    public void storeHeroClass( ArenaPlayer player ) { getOrCreateSave( player ).storeArenaClass(); }
    public void storeScoreboard( ArenaPlayer player ) { getOrCreateSave( player ).storeScoreboard(); }

    public void restoreExperience(ArenaPlayer p) {
        PlayerSave save = getSave(p);
        if (save == null || save.getExp() == null)
            return;
        if (restoreable(p))
            save.restoreExperience();
        else
            BAPlayerListener.restoreExpOnReenter(p, save.removeExperience());
    }

    public void restoreHealth(ArenaPlayer p) {
        PlayerSave save = getSave(p);
        if (save == null || save.getHealth()==null)
            return;
        if (restoreable(p))
            save.restoreHealth();
        else
            BAPlayerListener.restoreHealthOnReenter(p, save.removeHealth());
    }

    public void restoreHunger(ArenaPlayer p) {
        PlayerSave save = getSave(p);
        if (save == null || save.getHunger()==null)
            return;
        if (restoreable(p))
            save.restoreHunger();
        else
            BAPlayerListener.restoreHungerOnReenter(p, save.removeHunger());
    }

    public void restoreEffects(ArenaPlayer p) {
        PlayerSave save = getSave(p);
        if (save == null || save.getEffects()==null)
            return;
        if (restoreable(p))
            save.restoreEffects();
        else
            BAPlayerListener.restoreEffectsOnReenter(p, save.removeEffects());
    }

    public void restoreMoney(ArenaPlayer p) {
        PlayerSave save = getSave(p);
        if (save == null || save.getMoney()==null)
            return;
        save.restoreMoney();
    }

    public void restoreMagic(ArenaPlayer p) {
        PlayerSave save = getSave(p);
        if ( save == null || save.getMagic() == null ) return;
        
        if ( restoreable( p ) )
            save.restoreMagic();
        else 
            BAPlayerListener.restoreMagicOnReenter(p, save.removeMagic());
    }

    public void clearMatchItems(ArenaPlayer p) {
        PlayerSave save = getSave(p);
        if (save == null || save.getMatchItems()==null)
            return;
        if (restoreable(p))
            save.removeMatchItems();
    }

    public void restoreItems(ArenaPlayer p) {
        PlayerSave save = getSave(p);
        if (save == null || save.getItems()==null)
            return;
        if (restoreable(p))
            save.restoreItems();
        else
            BAPlayerListener.restoreItemsOnReenter(p, save.removeItems());
    }

    public void restoreMatchItems(ArenaPlayer p){
        PlayerSave save = getSave(p);
        if (save == null || save.getMatchItems()==null)
            return;
        if (restoreable(p))
            save.restoreMatchItems();
        else
            BAPlayerListener.restoreItemsOnReenter(p, save.removeMatchItems());
    }

    public static void setMatchInventory(ArenaPlayer p, PInv pinv) {
        if (Defaults.DEBUG_STORAGE) Log.info("restoring match items for " + p.getName() +"= "+" o="+p.isOnline() +"  dead="+p.isDead() +" h=" + p.getHealth()+"");
        if (p.isOnline() && !p.isDead())
            InventoryUtil.addToInventory(p.getPlayer(), pinv);
        else
            BAPlayerListener.restoreItemsOnReenter(p, pinv);
    }

    public static void setInventory(ArenaPlayer p, PInv pinv) {
        if (Defaults.DEBUG_STORAGE) Log.info("setInventory items to " + p.getName() +"= "+" o="+p.isOnline() +"  dead="+p.isDead() +" h=" + p.getHealth()+"");
        if (p.isOnline() && !p.isDead())
            InventoryUtil.addToInventory(p.getPlayer(), pinv);
        else
            BAPlayerListener.restoreItemsOnReenter(p, pinv);
    }

    public void restoreFlight(ArenaPlayer p) {
        PlayerSave save = getSave(p);
        if (save == null || save.getFlight()==null)
            return;
        if (restoreable(p))
            save.restoreFlight();
    }

    public void restoreGodmode(ArenaPlayer p) {
        PlayerSave save = getSave(p);
        if (save == null || save.getGodmode()==null)
            return;
        if (restoreable(p))
            save.restoreGodmode();
    }

    public void restoreGamemode(ArenaPlayer p) {
        PlayerSave save = getSave(p);
        if (save == null || save.getGamemode()==null)
            return;
        if (restoreable(p))
            save.restoreGamemode();
        else
            BAPlayerListener.restoreGameModeOnEnter(p, save.removeGamemode());
    }

    public void clearInv(ArenaPlayer player) {
        if (player.isOnline())
            InventoryUtil.clearInventory(player.getPlayer());
        else
            BAPlayerListener.clearInventoryOnReenter(player);
    }

    public static void removeItem(ArenaPlayer p, ItemStack is) {
        if (p.isOnline())
            InventoryUtil.removeItems(p.getInventory(),is);
        else
            BAPlayerListener.removeItemOnEnter(p,is);
    }

    public static void removeItems(ArenaPlayer p, List<ItemStack> items) {
        if (p.isOnline())
            InventoryUtil.removeItems(p.getInventory(),items);
        else
            BAPlayerListener.removeItemsOnEnter(p,items);
    }

    public void addMember(ArenaPlayer p, WorldGuardRegion region) {
        WorldGuardController.addMember(p.getName(), region);
    }
    public void removeMember(ArenaPlayer p, WorldGuardRegion region) {
        WorldGuardController.removeMember(p.getName(), region);
    }

    public void restoreHeroClass(ArenaPlayer p) {
        PlayerSave save = getSave(p);
        if (save == null || save.getArenaClass()==null) return;
        
        if (restoreable(p))
            save.restoreArenaClass();
    }

    public void cancelExpLoss(ArenaPlayer p, boolean cancel) {
        if (!HeroesController.enabled()) return;
        
        HeroesController.cancelExpLoss(p.getPlayer(),cancel);
    }

    public void deEnchant(Player p) {
        try{ EffectUtil.deEnchantAll(p);} catch (Exception e){/* do nothing */}
        HeroesController.deEnchant(p);
        if (!p.isOnline() || p.isDead())
            BAPlayerListener.deEnchantOnEnter(PlayerController.toArenaPlayer(p));
    }

    public void restoreScoreboard(ArenaPlayer player) {
        PlayerSave save = getSave(player);
        if (save == null || save.getScoreboard()==null) return;
        
        if (restoreable(player))
            save.restoreScoreboard();
    }
}
