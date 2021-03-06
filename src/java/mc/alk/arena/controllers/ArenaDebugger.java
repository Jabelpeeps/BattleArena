package mc.alk.arena.controllers;

import java.util.HashMap;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import lombok.RequiredArgsConstructor;
import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.objects.spawns.SpawnLocation;
import mc.alk.arena.util.TeamUtil;

@RequiredArgsConstructor
public class ArenaDebugger {
	static HashMap<Arena,ArenaDebugger> arenas = null;
    final Arena arena;
    HashMap<Location, ItemStack> oldBlocks = new HashMap<>(); /// Used for debugging with show/hide spawns
    
	public static ArenaDebugger getDebugger(Arena arena){
		if (arenas == null){
			arenas = new HashMap<>();
		}

		ArenaDebugger ad = arenas.get(arena);
		if (ad == null){
			ad = new ArenaDebugger(arena);
			arenas.put(arena, ad);
		}
		return ad;
	}

	public static void removeDebugger(ArenaDebugger ad) {
		arenas.remove(ad.arena);
		if (arenas.isEmpty()){
			arenas = null;
		}
	}

	static public Location getLocKey(Location l){
        return new Location(l.getWorld(), l.getBlockX(), l.getBlockY(), l.getBlockZ());
	}

	public void hideSpawns(Player player) {
		for (Location l : oldBlocks.keySet()){
			ItemStack is = oldBlocks.get(l);
			player.sendBlockChange(l, is.getType(), (byte) is.getDurability());
		}
		oldBlocks.clear();
		SpawnController sc = arena.getSpawnController();
		if (sc != null){
			sc.stop();
		}
	}

	public void showSpawns(Player player) {
		oldBlocks = new HashMap<>();
		SpawnController sc = arena.getSpawnController();
		if (sc != null){
			sc.start();
		}
		List<List<SpawnLocation>> locs = arena.getSpawns();
		if (locs != null){
			for (int i=0;i<locs.size();i++){
                for (SpawnLocation l : locs.get(i)){
                    changeBlocks(player, l.getLocation(), TeamUtil.getTeamHead(i));
                }
            }
		}
		locs = arena.getWaitroom() != null ? arena.getWaitroom().getSpawns() : null;
		if (locs != null){
            for (int i=0;i<locs.size();i++){
                for (SpawnLocation l : locs.get(i)){
                    changeBlocks(player, l.getLocation(), TeamUtil.getTeamHead(i));
                }
            }
		}
		locs = arena.getSpectatorRoom() != null ? arena.getSpectatorRoom().getSpawns() : null;
		if (locs != null){
            for (int i=0;i<locs.size();i++){
                for (SpawnLocation l : locs.get(i)){
                    changeBlocks(player, l.getLocation(), TeamUtil.getTeamHead(i));
                }
            }
		}
	}

	private void changeBlocks(Player player, Location l, ItemStack is) {
		Location key = getLocKey(l);
		if (!oldBlocks.containsKey(key)){
			Block b = l.getBlock();
			player.sendBlockChange(l, is.getType(), (byte)is.getDurability());
			ItemStack item = new ItemStack( b.getType() );
		    item.setData( b.getState().getData() );
			oldBlocks.put(key, item );
		}
	}

}
