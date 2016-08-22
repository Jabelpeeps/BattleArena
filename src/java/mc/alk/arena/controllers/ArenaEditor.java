package mc.alk.arena.controllers;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.BattleArena;
import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.objects.options.SpawnOptions;
import mc.alk.arena.objects.spawns.BlockSpawn;
import mc.alk.arena.objects.spawns.ChestSpawn;
import mc.alk.arena.objects.spawns.TimedSpawn;
import mc.alk.arena.serializers.ArenaSerializer;
import mc.alk.arena.serializers.SpawnSerializer;
import mc.alk.arena.util.MessageUtil;

public class ArenaEditor implements Listener{
    int nListening=0;
    Integer timerID;
    HashMap<UUID, CurrentSelection> selections = new HashMap<>();

    public class CurrentSelection  {
        @Getter @Setter public long lastUsed;
        @Getter @Setter public Arena arena;
        public Long listeningIndex;
        public SpawnOptions listeningOptions;

        CurrentSelection(long used, Arena _arena) {
            lastUsed = used; arena = _arena;
        }
        public void updateCurrentSelection(){
            lastUsed = System.currentTimeMillis();
        }
        public void setStartListening(Long index, SpawnOptions po) {
            startListening();
            listeningIndex = index;
            listeningOptions = po;
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null || !selections.containsKey( event.getPlayer().getUniqueId() ) ) {
            return;
        }
        CurrentSelection cs = selections.get( event.getPlayer().getUniqueId() );
        if (cs.listeningIndex == null || cs.getArena() == null)
            return;
        
        if ( event.getPlayer().getGameMode()== GameMode.CREATIVE 
                && event.getAction()== Action.LEFT_CLICK_BLOCK )
            event.setCancelled(true );
        Arena a = cs.getArena();
        BlockSpawn bs;
        
        if (event.getClickedBlock().getState() instanceof Chest) 
            bs = new ChestSpawn(event.getClickedBlock(), true);
        else 
            bs = new BlockSpawn(event.getClickedBlock(), true);
        
        TimedSpawn ts = SpawnSerializer.createTimedSpawn(bs, cs.listeningOptions);

        a.addTimedSpawn(ts); // a.addTimedSpawn(cs.listeningIndex,ts);
        BattleArena.getBAController().updateArena(a);
        ArenaSerializer.saveArenas(a.getArenaType().getPlugin());
        MessageUtil.sendMessage(event.getPlayer(), "&2Added block spawn &6"+ ts +"&2 to index=&5"+cs.listeningIndex);
        cs.listeningIndex = null;
        cs.listeningOptions = null;
    }

    void startListening() {
        final ArenaEditor self = this;
        if (nListening == 0) {
            Bukkit.getPluginManager().registerEvents(this, BattleArena.getSelf());
        }
        nListening++;
        if (timerID != null) {
            Scheduler.cancelTask(timerID);
        }
        timerID = Scheduler.scheduleSynchronousTask( 
                () -> { HandlerList.unregisterAll(self);
                        nListening = 0;
                }, 20 * 30L );
    }

    public void setCurrentArena(CommandSender p, Arena arena) {
        if ( !( p instanceof Player )) return;
        
        UUID id = ((Player) p).getUniqueId();
        if (selections.containsKey(id)) {
            CurrentSelection cs = selections.get(id);
            cs.setLastUsed(System.currentTimeMillis());
            cs.setArena(arena);
        } 
        else {
            CurrentSelection cs = new CurrentSelection(System.currentTimeMillis(), arena);
            selections.put(id, cs);
        }
    }

    public Arena getArena(CommandSender p) {
        if ( !( p instanceof Player )) return null;
        
        CurrentSelection cs = selections.get( ((Player) p).getUniqueId() );
        if (cs == null) return null;
        
        return cs.arena;
    }

    public CurrentSelection getCurrentSelection(CommandSender sender) {
        if ( !( sender instanceof Player )) return null;
        return selections.get( ((Player) sender).getUniqueId() );
    }
}
