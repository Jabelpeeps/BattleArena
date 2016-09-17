package mc.alk.arena.listeners.competition;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.player.PlayerMoveEvent;

import mc.alk.arena.listeners.PlayerHolder;
import mc.alk.arena.objects.arenas.ArenaListener;
import mc.alk.arena.objects.events.ArenaEventHandler;
import mc.alk.arena.objects.events.ArenaEventPriority;
import mc.alk.arena.objects.options.TransitionOption;
import mc.alk.arena.objects.regions.ArenaRegion;
import mc.alk.arena.plugins.WorldGuardController;

public class PlayerMoveListener implements ArenaListener{
    PlayerHolder holder;
    ArenaRegion region;
    World world;
    
	public PlayerMoveListener(PlayerHolder aHolder, ArenaRegion aRegion){
		holder = aHolder;
        region = aRegion;
        world = Bukkit.getWorld(aRegion.getWorldName());
    }

    @ArenaEventHandler( priority = ArenaEventPriority.HIGH )
    public void onPlayerMove(PlayerMoveEvent event){
        if (!event.isCancelled() && world.getUID() == event.getTo().getWorld().getUID() &&
                holder.hasOption(TransitionOption.WGNOLEAVE) &&
                WorldGuardController.hasWorldGuard()){
            /// Did we actually even move
            if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                    || event.getFrom().getBlockY() != event.getTo().getBlockY()
                    || event.getFrom().getBlockZ() != event.getTo().getBlockZ()){
                if (WorldGuardController.isLeavingArea(event.getFrom(), event.getTo(),world,region.getRegionName())){
                    event.setCancelled(true);}
            }
        }
    }
}
