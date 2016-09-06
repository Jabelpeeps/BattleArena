package mc.alk.arena.objects.spawns;

import org.bukkit.Location;
import org.bukkit.World;

import lombok.Getter;
import lombok.Setter;

public abstract class SpawnInstance implements Spawnable, SpawnableInstance {
	static int classCount = 0;

	final Integer spawnId = classCount++;
	@Getter @Setter Location location;

	public SpawnInstance( Location loc ) { location = loc; }
	public World getWorld() { return location != null ? location.getWorld() : null; }
    public int getID() { return spawnId; }
}
