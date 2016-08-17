package mc.alk.arena.util;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.scoreboard.Scoreboard;

import mc.alk.arena.Defaults;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.CommandLineString;
import mc.alk.arena.plugins.EssentialsController;
import mc.alk.arena.plugins.HeroesController;

public class PlayerUtil {

//    public static int getHunger(final Player player) {
//        return player.getFoodLevel();
//    }

//    public static void setHunger(final Player player, final Integer hunger) {
//        player.setFoodLevel(hunger);
//    }
//
    public static void setHealthP(final Player player, final Double health) {
        setHealthP(player,health, false);
    }

    public static void setHealthP(final Player player, final Double health, boolean skipHeroes) {
        if (!skipHeroes && HeroesController.enabled()){
            HeroesController.setHealthP(player,health);
            return;
        }
        double val = (player.getMaxHealth() * health/100.0);
        setHealth(player,val);
    }

    public static void setHealth(final Player player, final Double health) {
 
        final double oldHealth = player.getHealth();
        if (oldHealth > health){
            EntityDamageEvent event = new EntityDamageEvent(player,  DamageCause.CUSTOM, oldHealth-health );
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()){
                player.setLastDamageCause(event);
                final double dmg = Math.max(0,oldHealth - event.getDamage());
                player.setHealth(dmg);
            }
        } else if (oldHealth < health){
            EntityRegainHealthEvent event = new EntityRegainHealthEvent(player, health-oldHealth, RegainReason.CUSTOM);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()){
                final double regen = Math.min(oldHealth + event.getAmount(),player.getMaxHealth());
                player.setHealth(regen);
            }
        }
    }

    public static void setInvulnerable(Player player, Integer invulnerableTime) {
        player.setNoDamageTicks(invulnerableTime);
        player.setLastDamage(Integer.MAX_VALUE);
    }

    public static void setGameMode(Player p, GameMode gameMode) {
        if (p.getGameMode() != gameMode){
            PermissionsUtil.givePlayerInventoryPerms(p);
            p.setGameMode(gameMode);
        }
    }

    public static void doCommands(Player p, List<CommandLineString> doCommands) {
        final String name = p.getName();
        for (CommandLineString cls: doCommands){
            try{
                CommandSender cs = cls.isConsoleSender() ? Bukkit.getConsoleSender() : p;
                if (Defaults.DEBUG_TRANSITIONS) {Log.info("BattleArena doing command '"+cls.getCommand(name)+"' as "+cs.getName());}
                Bukkit.dispatchCommand(cs,cls.getCommand(name));
            } catch (Exception e){
                Log.err("[BattleArena] Error executing command as console or player");
                Log.printStackTrace(e);
            }

        }
    }

    public static void setFlight(Player player, boolean enable) {
        if (player.getAllowFlight() != enable)
            player.setAllowFlight(enable);
        
        if (player.isFlying() != enable)
            player.setFlying(enable);
    }

    public static void setFlightSpeed(Player player, Float flightSpeed) {
        try {
            player.setFlySpeed(flightSpeed);
        } catch (Throwable e){
            /* ignore method not found problems */
        }
    }

    public static void setGod(Player player, boolean enable) {
        if (EssentialsController.enabled()){
            EssentialsController.setGod(player, enable);}
    }


    public static Object getScoreboard(Player player) {
        return player.getScoreboard();
    }

    public static void setScoreboard(Player player, Object scoreboard) {
        player.setScoreboard( (Scoreboard) scoreboard );
    }

    public static UUID getID(ArenaPlayer player) {
        return player.getPlayer().getUniqueId();
    }

    public static UUID getID(CommandSender sender) {
        if ( sender instanceof ArenaPlayer ) {
            return ((ArenaPlayer) sender).getPlayer().getUniqueId();
        } else if (sender instanceof Player){
            return ((Player) sender).getUniqueId();
        } else {
            return new UUID(0, sender.getName().hashCode());
        }
    }
}
