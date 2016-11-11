package mc.alk.arena.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Scoreboard;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.Defaults;
import mc.alk.arena.Permissions;
import mc.alk.arena.controllers.MoneyController;
import mc.alk.arena.listeners.BAPlayerListener;
import mc.alk.arena.plugins.EssentialsUtil;
import mc.alk.arena.plugins.HeroesController;
import mc.alk.arena.serializers.InventorySerializer;
import mc.alk.arena.util.EffectUtil;
import mc.alk.arena.util.ExpUtil;
import mc.alk.arena.util.InventoryUtil;
import mc.alk.arena.util.InventoryUtil.PInv;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.PlayerUtil;

/**
 * @author alkarin
 */
public class PlayerSave {
    final ArenaPlayer player;

    @Getter @Setter Integer exp;

    @Getter @Setter Double health;
    @Getter @Setter Double healthp;
    @Getter @Setter Integer hunger;
    @Getter @Setter Integer magic;
    @Getter @Setter Double magicp;
    @Getter @Setter PInv items;
    @Getter PInv matchItems;
    @Getter @Setter GameMode gamemode;
    @Getter @Setter Boolean godmode;
    @Getter @Setter Location location;
    @Getter @Setter Collection<PotionEffect> effects;
    @Getter @Setter Boolean flight;
    @Getter @Setter String arenaClass;
    @Getter @Setter String oldTeam;
    @Getter private Scoreboard scoreboard;
    @Getter @Setter Double money;

    public PlayerSave(ArenaPlayer _player) {
        player = _player;
    }
    public UUID getUUID() {
        return player.getUniqueId();
    }

    public int storeExperience() {
        Player p = player.getPlayer();
        int _exp = ExpUtil.getTotalExperience(p);
        if (_exp == 0)
            return 0;
        exp = exp == null ? _exp : exp + _exp;
        ExpUtil.setTotalExperience(p, 0);
 
        p.updateInventory();
        return _exp;
    }

    public void restoreExperience() {
        if (exp == null) return;
        ExpUtil.setTotalExperience(player.getPlayer(), exp);
        exp = null;
    }

    public Integer removeExperience() {
        Integer _exp = exp;
        exp = null;
        return _exp;
    }

    public void storeHealth() {
        if ( health != null ) return;
        health = player.getHealth();
        if (Defaults.DEBUG_STORAGE) Log.info("storing health=" + health + " for player=" + player.getName());
    }

    public void restoreHealth() {
        if (health == null || health <= 0) return;
        if (Defaults.DEBUG_STORAGE) Log.info("restoring health=" + health+" for player=" + player.getName());
        PlayerUtil.setHealth(player.getPlayer(),health);
        health=null;
    }

    public Double removeHealth() {
        Double rhealth = health;
        health = null;
        return rhealth;
    }

    public void storeHunger() {
        if ( hunger == null )
            hunger = player.getFoodLevel();
    }

    public void restoreHunger() {
        if (hunger == null || hunger <= 0) return;
        player.getPlayer().setFoodLevel( hunger );
        hunger = null;
    }
    public Integer removeHunger(){
        Integer ret = hunger;
        hunger = null;
        return ret;
    }

    public void storeEffects() {
        if (effects == null ) 
            effects = new ArrayList<>(player.getPlayer().getActivePotionEffects());
    }

    public void restoreEffects() {
        if (effects == null) return;
        EffectUtil.enchantPlayer(player.getPlayer(), effects);
        effects = null;
    }

    public Collection<PotionEffect> removeEffects() {
        Collection<PotionEffect> ret = effects;
        effects = null;
        return ret;
    }

    public void storeMagic() {
        if ( HeroesController.enabled() && magic == null ) 
            magic = HeroesController.getMagicLevel(player.getPlayer());
    }

    public void restoreMagic() {
        if (!HeroesController.enabled() || magic ==null) return;
        HeroesController.setMagicLevel(player.getPlayer(), magic);
        magic = null;
    }

    public Integer removeMagic() {
        Integer ret = magic;
        magic = null;
        return ret;
    }

    public void storeItems() {
        if ( items == null ) {
            player.getPlayer().closeInventory();
            items = new PInv(player.getInventory());
            InventorySerializer.saveInventory(player.getUniqueId(), items);
        }
    }

    public void restoreItems() {
        if (items == null) return;
        InventoryUtil.addToInventory( player.getPlayer(), items );
        items = null;
    }

    public PInv removeItems() {
        PInv ret = items;
        items = null;
        return ret;
    }

    public PlayerSave storeMatchItems() {
        player.getPlayer().closeInventory();
        PInv pinv = new PInv( player.getInventory() );
        
        if ( matchItems == null ) {
            InventorySerializer.saveInventory( player.getUniqueId(), pinv);
        }
        matchItems = pinv;
        BAPlayerListener.restoreMatchItemsOnReenter( player, pinv );
        return this;
    }

    public void restoreMatchItems() {
        if (matchItems==null) return;
        InventoryUtil.addToInventory(player.getPlayer(), matchItems);
        matchItems = null;
    }

    public PInv removeMatchItems() {
        PInv ret = matchItems;
        matchItems = null;
        return ret;
    }

    public void storeGamemode() {
        if (gamemode !=null) return;
        Permissions.givePlayerInventoryPerms(player.getPlayer());
        gamemode = player.getPlayer().getGameMode();
    }


    public void storeFlight() {
        if ( player.getPlayer().isFlying() )
            flight = true;
    }

    public void restoreFlight() {
        if ( flight == null ) return;
        player.getPlayer().setFlying( flight );
        flight = null;
    }

    public void storeGodmode() {
        if (!EssentialsUtil.isEnabled() || godmode != null) return;
        if (EssentialsUtil.isGod(player))
            godmode = true;
    }

    public void restoreGodmode() {
        if (godmode == null) return;
        EssentialsUtil.setGod(player, godmode);
        godmode = null;
    }

    public void restoreGamemode() {
        if (gamemode == null) return;
        PlayerUtil.setGameMode(player.getPlayer(), gamemode);
        gamemode = null;
    }

    public GameMode removeGamemode() {
        GameMode ret = gamemode;
        gamemode = null;
        return ret;
    }

    public void storeArenaClass() {
        if (!HeroesController.enabled() || arenaClass != null) return;
        arenaClass = HeroesController.getHeroClassName(player.getPlayer());
    }

    public void restoreArenaClass() {
        if (!HeroesController.enabled() || arenaClass==null) return;
        HeroesController.setHeroClass(player.getPlayer(), arenaClass);
    }

    public void storeScoreboard() {
        if ( scoreboard != null ) return;      
        scoreboard = player.getPlayer().getScoreboard();
    }

    public void restoreScoreboard() {
        if ( scoreboard == null ) return;
        player.getPlayer().setScoreboard( scoreboard );
    }

    public void restoreMoney() {
        if ( money == null ) return;
        MoneyController.add( player.getPlayer(), money );
        money = null;
    }
}
