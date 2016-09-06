package mc.alk.arena.objects.spawns;

import org.bukkit.Material;
import org.bukkit.block.Block;

import lombok.Getter;
import lombok.Setter;


public class BlockSpawn extends SpawnInstance {
    @Getter @Setter Material material;
    @Getter @Setter Material despawnMaterial = Material.AIR;

	public BlockSpawn( Block block, boolean setMaterial ) {
		super( block.getLocation() );
        if ( setMaterial )
            material = block.getType();
    }
	
    @Override
    public void spawn() {
        Block b = location.getBlock();
        if ( material != null && b.getType() != material )
            b.setType( material );
    }

    @Override
	public void despawn() {
        Block b = location.getBlock();
        if ( despawnMaterial != null )
            b.setType( despawnMaterial );
	}

	@Override
	public String toString(){
		return "[BS "+material.name()+"]";
	}

    public Block getBlock() {
        return location.getBlock();
    }
}
