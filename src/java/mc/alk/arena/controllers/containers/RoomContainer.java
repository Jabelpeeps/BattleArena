package mc.alk.arena.controllers.containers;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.events.ArenaEventHandler;
import mc.alk.arena.objects.events.ArenaEventHandler.ArenaEventPriority;


public class RoomContainer extends AreaContainer {

    public RoomContainer( String _name, LocationType _type ) {
        super( _name, _type );
    }
    public RoomContainer( String _name, MatchParams _params, LocationType _type ) {
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
        if ( event.getInventory().getType() == InventoryType.ENDER_CHEST )
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
