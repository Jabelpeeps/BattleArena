package mc.alk.arena.objects.spawns;

import java.util.Collection;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;

import lombok.Getter;


public class ChestSpawn extends BlockSpawn {

    @Getter ItemStack[] items;

    public ChestSpawn( Block block, boolean setItems ) {
		super( block, setItems );
        if ( setItems && block.getState() instanceof Chest ) {
            Chest chest = (Chest) block.getState();
            ItemStack[] contents = chest.getInventory().getContents();
            items = new ItemStack[contents.length];
            for ( int i = 0; i < contents.length; i++ ) {
                items[i] = (contents[i] != null) ? contents[i].clone() : null;
            }
        }
    }

    public void setItems( Collection<ItemStack> _items ) {
        items = _items.toArray( new ItemStack[_items.size()] );
    }

    @Override
    public void spawn() {
        super.spawn();
        Chest chest = (Chest) location.getBlock().getState();
        chest.getInventory().clear();
        chest.getInventory().setContents(items);
        chest.update(true);
    }

    @Override
	public void despawn() {
        Block b = getLocation().getBlock();
        if ( b.getState() instanceof Chest ) {
            Chest chest = (Chest) location.getBlock().getState();
            chest.getInventory().clear();
            chest.update(true);
        }
        b.setType( despawnMaterial );
	}

	@Override
	public String toString() { return "[ChestSpawn " + material.name() + "]"; }

    public boolean isDoubleChest() {
        Block b = location.getBlock();
        return ( isChest(b.getRelative(BlockFace.NORTH)) ||
                 isChest(b.getRelative(BlockFace.SOUTH)) ||
                 isChest(b.getRelative(BlockFace.EAST))  ||
                 isChest(b.getRelative(BlockFace.WEST)) );
    }

    public static boolean isChest(Block block) { return block.getState() instanceof Chest; }
}
