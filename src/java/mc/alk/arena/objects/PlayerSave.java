package mc.alk.arena.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.Defaults;
import mc.alk.arena.controllers.MoneyController;
import mc.alk.arena.listeners.BAPlayerListener;
import mc.alk.arena.plugins.EssentialsController;
import mc.alk.arena.plugins.HeroesController;
import mc.alk.arena.serializers.InventorySerializer;
import mc.alk.arena.util.EffectUtil;
import mc.alk.arena.util.ExpUtil;
import mc.alk.arena.util.InventoryUtil;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.PermissionsUtil;
import mc.alk.arena.util.PlayerUtil;
import mc.alk.arena.util.InventoryUtil.PInv;

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
    @Getter private Object scoreboard;
    @Getter @Setter Double money;

    public PlayerSave(ArenaPlayer _player) {
        player = _player;
    }
    public String getName() {
        return player.getName();
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
        if (health!=null)
            return;

        health = player.getHealth();
        if (Defaults.DEBUG_STORAGE) Log.info("storing health=" + health + " for player=" + player.getName());
    }

    public void restoreHealth() {
        if (health == null || health <= 0)
            return;
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
        if (hunger !=null)
            return;
        hunger = player.getFoodLevel();
    }

    public void restoreHunger() {
        if (hunger == null || hunger <= 0)
            return;
        player.getPlayer().setFoodLevel( hunger );
        hunger = null;
    }
    public Integer removeHunger(){
        Integer ret = hunger;
        hunger = null;
        return ret;
    }

    public void storeEffects() {
        if (effects !=null)
            return;
        effects = new ArrayList<>(player.getPlayer().getActivePotionEffects());
    }

    public void restoreEffects() {
        if (effects == null)
            return;
        EffectUtil.enchantPlayer(player.getPlayer(), effects);
        effects = null;
    }

    public Collection<PotionEffect> removeEffects() {
        Collection<PotionEffect> ret = effects;
        effects = null;
        return ret;
    }

    public void storeMagic() {
        if (!HeroesController.enabled() || magic != null)
            return;
        magic = HeroesController.getMagicLevel(player.getPlayer());
    }

    public void restoreMagic() {
        if (!HeroesController.enabled() || magic ==null)
            return;
        HeroesController.setMagicLevel(player.getPlayer(), magic);
        magic = null;
    }

    public Integer removeMagic() {
        Integer ret = magic;
        magic = null;
        return ret;
    }

    public void storeItems() {
        if (items != null) return;
        
        player.getPlayer().closeInventory();
        items = new PInv(player.getInventory());
        InventorySerializer.saveInventory(player.getUniqueId(), items);
    }

    public void restoreItems() {
        if (items ==null) return;
        
        InventoryUtil.addToInventory(player.getPlayer(), items);
        items = null;
    }

    public PInv removeItems() {
        PInv ret = items;
        items = null;
        return ret;
    }

    public void storeMatchItems() {
        final UUID id = player.getUniqueId();
        player.getPlayer().closeInventory();
        final PInv pinv = new PInv(player.getInventory());
        
        if (matchItems == null) {
            InventorySerializer.saveInventory(id, pinv);
        }
        matchItems = pinv;
        BAPlayerListener.restoreMatchItemsOnReenter(player, pinv);
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
        
        PermissionsUtil.givePlayerInventoryPerms(player.getPlayer());
        gamemode = player.getPlayer().getGameMode();
    }


    public void storeFlight() {
        if (!EssentialsController.enabled() || flight != null) return;

        if (EssentialsController.isFlying(player))
            flight = true;
    }

    public void restoreFlight() {
        if (flight == null) return;
        
        EssentialsController.setFlight(player.getPlayer(), flight);
        flight = null;
    }


    public void storeGodmode() {
        if (!EssentialsController.enabled() || godmode != null) return;

        if (EssentialsController.isGod(player))
            godmode = true;
    }

    public void restoreGodmode() {
        if (godmode == null)
            return;
        EssentialsController.setGod(player.getPlayer(), godmode);
        godmode = null;
    }

    public void restoreGamemode() {
        if (gamemode == null)
            return;
        PlayerUtil.setGameMode(player.getPlayer(), gamemode);
        gamemode = null;
    }

    public GameMode removeGamemode() {
        GameMode ret = gamemode;
        gamemode = null;
        return ret;
    }

    public void storeArenaClass() {
        if (!HeroesController.enabled() || arenaClass != null)
            return;
        arenaClass = HeroesController.getHeroClassName(player.getPlayer());
    }

    public void restoreArenaClass() {
        if (!HeroesController.enabled() || arenaClass==null)
            return;
        HeroesController.setHeroClass(player.getPlayer(), arenaClass);
    }

    public void storeScoreboard() {
        if (scoreboard != null)
            return;
        scoreboard = PlayerUtil.getScoreboard(player.getPlayer());
    }

    public void restoreScoreboard() {
        if (scoreboard==null)
            return;
        PlayerUtil.setScoreboard(player.getPlayer(), scoreboard);
    }

    public void restoreMoney() {
        if (money == null)
            return;
        MoneyController.add(player.getName(), money);
        money = null;
    }

    public UUID getID() {
        return player.getUniqueId();
    }
}
