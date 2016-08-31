package mc.alk.arena.util;

import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import mc.alk.arena.Defaults;
import mc.alk.arena.Permissions;
import mc.alk.arena.objects.CommandLineString;
import mc.alk.arena.plugins.HeroesController;

public class PlayerUtil {

    public static void setHealthPercent(final Player player, final Double health) {
        setHealthPercent( player, health, false );
    }

    public static void setHealthPercent(final Player player, final Double health, boolean skipHeroes) {
        if (!skipHeroes && HeroesController.enabled()){
            HeroesController.setHealthP(player,health);
            return;
        }
        setHealth( player, player.getMaxHealth() * health / 100.0 );
    }

    @SuppressWarnings( "deprecation" )
    public static void setHealth(final Player player, final Double health) {
 
        final double oldHealth = player.getHealth();
        if (oldHealth > health){
            EntityDamageEvent event = new EntityDamageEvent(player,  DamageCause.CUSTOM, oldHealth - health );
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()){
                player.setLastDamageCause(event);
                final double dmg = Math.max( 0, oldHealth - event.getDamage());
                player.setHealth(dmg);
            }
        } 
        else if (oldHealth < health){
            EntityRegainHealthEvent event = new EntityRegainHealthEvent(player, health - oldHealth, RegainReason.CUSTOM);
            Bukkit.getPluginManager().callEvent(event);
            
            if (!event.isCancelled())
                player.setHealth( Math.min(oldHealth + event.getAmount(), player.getMaxHealth()) );
        }
    }

    public static void setInvulnerable(Player player, Integer invulnerableTime) {
        player.setNoDamageTicks(invulnerableTime);
        player.setLastDamage(Integer.MAX_VALUE);
    }

    public static void setGameMode(Player p, GameMode gameMode) {
        if ( p.getGameMode() != gameMode ) {
            Permissions.givePlayerInventoryPerms(p);
            p.setGameMode( gameMode );
        }
    }
    public static void doCommands(Player p, List<CommandLineString> doCommands) {
        String name = p.getName();
        for ( CommandLineString cls : doCommands ){
            try{
                CommandSender cs = cls.isConsoleSender() ? Bukkit.getConsoleSender() : p;
                if (Defaults.DEBUG_TRANSITIONS) 
                    Log.info( "BattleArena doing command '" + cls.getCommand(name) + "' as " + cs.getName() );
                
                Bukkit.dispatchCommand( cs, cls.getCommand(name) );
            } 
            catch (Exception e){
                Log.err("[BattleArena] Error executing command as console or player");
                Log.printStackTrace(e);
            }

        }
    }
    public static void setFlight(Player player, boolean enable) {
        player.setAllowFlight(enable);
        player.setFlying(enable);
    }

    public static OfflinePlayer getOfflinePlayer(String name) {
        OfflinePlayer op = PlayerUtil.map.get(name);
        
        if ( op == null ) {
            op = Bukkit.getPlayerExact( name );
            
            if ( op == null ) return null;
        }
        PlayerUtil.map.put(name, op);
        
        return op;
    }

    public static HashMap<String, OfflinePlayer> map = new HashMap<>();
}
