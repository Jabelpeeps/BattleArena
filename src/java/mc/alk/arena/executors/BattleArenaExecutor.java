package mc.alk.arena.executors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.controllers.ArenaClassController;
import mc.alk.arena.controllers.BAEventController;
import mc.alk.arena.controllers.CompetitionController;
import mc.alk.arena.controllers.PlayerController;
import mc.alk.arena.events.players.ArenaPlayerLeaveEvent;
import mc.alk.arena.objects.ArenaClass;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.arenas.ArenaType;
import mc.alk.arena.serializers.InventorySerializer;
import mc.alk.arena.util.InventoryUtil;
import mc.alk.arena.util.InventoryUtil.PInv;
import mc.alk.arena.util.MessageUtil;

public class BattleArenaExecutor extends CustomCommandExecutor {

    @MCCommand( cmds = {"listInv"}, admin = true )
    public void listSaves(CommandSender sender, OfflinePlayer p) {
        Collection<String> dates = InventorySerializer.getDates(p);

        if (dates == null) {
            MessageUtil.sendMessage(sender, "There are no inventory saves for this player");
            return;
        }
        int i = 0;
        MessageUtil.sendMessage(sender, "Most recent inventory saves");
        for (String date : dates) {
            MessageUtil.sendMessage(sender, ++i + " : " + date);
        }
    }

    @MCCommand( cmds = {"listInv"}, admin = true )
    public void listSave(CommandSender sender, OfflinePlayer p, Integer index) {
        if (index < 0 || index > Defaults.NUM_INV_SAVES) {
            MessageUtil.sendMessage(sender, "&c index must be between 1-" + Defaults.NUM_INV_SAVES);
            return;
        }
        PInv pinv = InventorySerializer.getInventory(p, index - 1);
        if (pinv == null) {
            MessageUtil.sendMessage(sender, "&cThis index doesn't have an inventory!");
            return;
        }
        MessageUtil.sendMessage(sender, "&6" + p.getName() + " inventory at save " + index);
        boolean has = false;
        for (ItemStack is : pinv.armor) {
            if (is == null || is.getType() == Material.AIR) continue;
            MessageUtil.sendMessage(sender, "&a armor: &6" + InventoryUtil.getItemString(is));
            has = true;
        }
        for (ItemStack is : pinv.contents) {
            if (is == null || is.getType() == Material.AIR) continue;
            MessageUtil.sendMessage(sender, "&b inv: &6" + InventoryUtil.getItemString(is));
            has = true;
        }
        if (!has) {
            MessageUtil.sendMessage(sender, "&cThis index doesn't have any items");
        }
    }

    @MCCommand( cmds = {"giveInv"}, admin = true )
    public void restoreInv(CommandSender sender, ArenaPlayer p, Integer index, Player other) {
        if (index < 0 || index > Defaults.NUM_INV_SAVES) 
            MessageUtil.sendMessage(sender, "&c index must be between 1-" + Defaults.NUM_INV_SAVES);
        else if (InventorySerializer.giveInventory(p, index - 1, other)) 
            MessageUtil.sendMessage(sender, "&2Player inventory given to " + other.getDisplayName());
        else 
            MessageUtil.sendMessage(sender, "&cPlayer inventory could not be given to " + other.getDisplayName());
    }

    @MCCommand( cmds = {"restoreInv"}, admin = true )
    public void restoreInv(CommandSender sender, ArenaPlayer p, Integer index) {
        if (index < 0 || index > Defaults.NUM_INV_SAVES)
            MessageUtil.sendMessage(sender, "&c index must be between 1-" + Defaults.NUM_INV_SAVES);
        else if (InventorySerializer.giveInventory(p, index - 1, p.getPlayer()))
            MessageUtil.sendMessage(sender, "&2Player inventory restored");
        else 
            MessageUtil.sendMessage(sender, "&cPlayer inventory could not be restored");
    }

    @MCCommand( cmds = {"version"}, admin = true )
    public void showVersion(CommandSender sender, String[] args) {
        
        MessageUtil.sendMessage( sender, "&6" + BattleArena.getNameAndVersion() );
        
        if ( args.length > 1 && args[1].equalsIgnoreCase("all") ) {
            
            HashMap<Plugin, List<ArenaType>> map = new HashMap<>();
            
            for ( ArenaType at : ArenaType.getTypes() ) {
                List<ArenaType> l = map.get(at.getPlugin());
                if (l == null) {
                    l = new ArrayList<>();
                    map.put(at.getPlugin(), l);
                }
                l.add(at);
            }
            for (Entry<Plugin, List<ArenaType>> entry : map.entrySet()) {
                MessageUtil.sendMessage( sender, 
                        "&6" + entry.getKey().getName() + " " + entry.getKey().getDescription().getVersion() +
                        "&e games: &f" + StringUtils.join(entry.getValue(), ", " ) );
            }
        } 
        else MessageUtil.sendMessage(sender, "&2For all game type versions, type &6/ba version all");
    }

    @MCCommand( cmds = {"reload"}, admin = true, perm = "arena.reload" )
    public void arenaReload(CommandSender sender) {
        BAEventController baec = BattleArena.getBAEventController();
        if (arenaController.hasRunningMatches() || !arenaController.isQueueEmpty() || baec.hasOpenEvent()) {
            MessageUtil.sendMessage(sender, "&cYou can't reload the config while matches are running or people are waiting in the queue");
            MessageUtil.sendMessage(sender, "&cYou can use &6/arena cancel all&c to cancel all matches and clear queues");
            return;
        }
        arenaController.stop();

        PlayerController.clearArenaPlayers();
        BattleArena.getSelf().reloadConfig();
        CompetitionController.reloadCompetitions();

        arenaController.resume();
        MessageUtil.sendMessage(sender, "&6BattleArena&e configuration reloaded");
    }

    @MCCommand( cmds = {"listClasses"}, admin = true )
    public void listArenaClasses(CommandSender sender) {
        Set<ArenaClass> classes = ArenaClassController.getClasses();
        MessageUtil.sendMessage(sender, "&2Registered classes");
        for (ArenaClass ac : classes) {
            if (ac.equals(ArenaClass.CHOSEN_CLASS) || ac.equals(ArenaClass.SELF_CLASS))
                continue;
            MessageUtil.sendMessage(sender, "&6" + ac.getName() + "&2 : " + ac.getDisplayName());
        }
    }

    @MCCommand( cmds = {"kick"}, admin = true, perm = "arena.kick" )
    public void arenaKick(CommandSender sender, ArenaPlayer player) {
        ArenaPlayerLeaveEvent event = new ArenaPlayerLeaveEvent(player, player.getTeam(),
                ArenaPlayerLeaveEvent.QuitReason.KICKED);
        event.callEvent();
        if (event.getMessages() != null && !event.getMessages().isEmpty()) {
            MessageUtil.sendMessage(event.getPlayer(), event.getMessages());
        }
        MessageUtil.sendMessage(sender, "&2You have kicked &6" + player.getName());
    }
}
