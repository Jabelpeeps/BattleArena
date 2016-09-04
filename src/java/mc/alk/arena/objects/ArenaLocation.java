package mc.alk.arena.objects;

import org.bukkit.Location;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.listeners.PlayerHolder;
import mc.alk.arena.listeners.PlayerHolder.LocationType;
import mc.alk.arena.util.SerializerUtil;

@AllArgsConstructor
public class ArenaLocation {
    
    @Getter final PlayerHolder playerHolder;
	@Getter @Setter Location location;
	@Getter final LocationType type;

	@Override
	public String toString(){
		return "[LocationType loc=" + SerializerUtil.getLocString(location) + " type=" + type + "]";
	}
}
