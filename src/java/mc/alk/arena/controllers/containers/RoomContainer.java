package mc.alk.arena.controllers.containers;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import mc.alk.arena.listeners.PlayerHolder;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.events.ArenaEventHandler;
import mc.alk.arena.objects.events.ArenaEventPriority;
import mc.alk.arena.util.InventoryUtil;


public class RoomContainer extends AreaContainer{

    public RoomContainer( String _name, PlayerHolder.LocationType _type ) {
        super(_name, _type );
    }
    public RoomContainer( String _name, MatchParams _params, PlayerHolder.LocationType _type ) {
        super( _name, _params, _type );
    }

    @ArenaEventHandler( suppressCastWarnings = true, priority = ArenaEventPriority.LOW )
    public void onEntityDamageEvent( EntityDamageEvent event ) {
        event.setCancelled(true);
    }
    @ArenaEventHandler( priority = ArenaEventPriority.HIGH )
    public void onPlayerBlockPlace( BlockPlaceEvent event ) {
        event.setCancelled(true);
    }
    @ArenaEventHandler( priority = ArenaEventPriority.HIGH )
    public void onInventoryOpenEvent( InventoryOpenEvent event ) {
        if ( InventoryUtil.isEnderChest( event.getInventory().getType() ) )
            event.setCancelled(true);
    }
    @ArenaEventHandler( priority = ArenaEventPriority.HIGH )
    public void onPlayerBlockBreak( BlockBreakEvent event ) {
        event.setCancelled(true);
    }
    @ArenaEventHandler( priority = ArenaEventPriority.HIGH )
    public void onPlayerDropItem( PlayerDropItemEvent event ) {
        event.setCancelled(true);
    }
    @ArenaEventHandler( priority = ArenaEventPriority.HIGH )
    public void onPlayerTeleport( PlayerTeleportEvent event ) {
        event.setCancelled(true);
    }
    @ArenaEventHandler( priority = ArenaEventPriority.HIGH )
    public void onFoodLevelChangeEvent( FoodLevelChangeEvent event ) {
        event.setCancelled(true);
    }
}
