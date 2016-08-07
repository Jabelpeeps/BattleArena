package mc.alk.arena.plugins.worldguard;

import com.sk89q.worldedit.LocalPlayer;
import com.sk89q.worldedit.bukkit.selections.Polygonal2DSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.GlobalRegionManager;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import mc.alk.arena.objects.exceptions.RegionNotFound;
import mc.alk.arena.objects.regions.ArenaRegion;
import mc.alk.arena.objects.regions.WorldGuardRegion;
import mc.alk.arena.plugins.worldedit.WorldEditUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * 
 * 
 * @author alkarin
 */
public abstract class WorldGuardAbstraction extends WorldGuardInterface {
    
    protected final WorldGuardPlugin wgp = (WorldGuardPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
    boolean hasWorldGuard = (wgp != null);

    Map<String, Set<String>> trackedRegions = new ConcurrentHashMap<String, Set<String>>();
    
    /**
     * Provides legacy support.
     */
    @Override
    public boolean setWorldGuard(Plugin plugin) {
        if (plugin != null) {
            hasWorldGuard = true;
        }
        return hasWorldGuard();
    }

    @Override
    public boolean hasWorldGuard() {
        
        return WorldEditUtil.hasWorldEdit() && hasWorldGuard;
    }

    @Override
    public ProtectedRegion getRegion(String world, String id) {
        World w = Bukkit.getWorld(world);
        return getRegion(w, id);
    }

    @Override
    public ProtectedRegion getRegion(World w, String id) {
        if (w == null) {
            return null;
        }
        return wgp.getRegionManager(w).getRegion(id);
    }

    @Override
    public boolean hasRegion(ArenaRegion region) {
        return hasRegion(region.getWorldName(), region.getID());
    }

    @Override
    public boolean hasRegion(World world, String id) {
        RegionManager mgr = wgp.getGlobalRegionManager().get(world);
        return mgr.hasRegion(id);
    }

    @Override
    public boolean hasRegion(String world, String id) {
        World w = Bukkit.getWorld(world);
        if (w == null) {
            return false;
        }
        RegionManager mgr = wgp.getGlobalRegionManager().get(w);
        return mgr.hasRegion(id);
    }

    @Override
    public ProtectedRegion updateProtectedRegion(Player p, String id) throws Exception {
        return createRegion(p, id);
    }

    @Override
    public ProtectedRegion createProtectedRegion(Player p, String id) throws Exception {
        return createRegion(p, id);
    }

    private ProtectedRegion createRegion(Player p, String id) throws Exception {
        Selection sel = WorldEditUtil.getSelection(p);
        World w = sel.getWorld();
        GlobalRegionManager gmanager = wgp.getGlobalRegionManager();
        RegionManager regionManager = gmanager.get(w);
        deleteRegion(w.getName(), id);
        ProtectedRegion region;
        // Detect the type of region from WorldEdit
        if (sel instanceof Polygonal2DSelection) {
            Polygonal2DSelection polySel = (Polygonal2DSelection) sel;
            int minY = polySel.getNativeMinimumPoint().getBlockY();
            int maxY = polySel.getNativeMaximumPoint().getBlockY();
            region = new ProtectedPolygonalRegion(id, polySel.getNativePoints(), minY, maxY);
        } else { /// default everything to cuboid
            region = new ProtectedCuboidRegion(id,
                    sel.getNativeMinimumPoint().toBlockVector(),
                    sel.getNativeMaximumPoint().toBlockVector());
        }
        region.setPriority(11); /// some relatively high priority
        region.setFlag(DefaultFlag.PVP, StateFlag.State.ALLOW);
        regionManager.addRegion(region);
        regionManager.save();
        return region;
    }

    @Override
    public void clearRegion(WorldGuardRegion region) {
        clearRegion(region.getRegionWorld(), region.getID());
    }

    @Override
    public void clearRegion(String world, String id) {
        World w = Bukkit.getWorld(world);
        if (w == null) {
            return;
        }
        ProtectedRegion region = getRegion(w, id);
        if (region == null) {
            return;
        }

        Location l;
        for (Entity entity : w.getEntitiesByClasses(Item.class, Creature.class)) {
            l = entity.getLocation();
            if (region.contains(l.getBlockX(), l.getBlockY(), l.getBlockZ())) {
                entity.remove();
            }
        }
    }

    @Override
    public boolean isLeavingArea(final Location from, final Location to, final ArenaRegion region) {
        return isLeavingArea(from, to, Bukkit.getWorld(region.getWorldName()), region.getID());
    }

    @Override
    public boolean isLeavingArea(final Location from, final Location to, final World w, String id) {
        ProtectedRegion pr = getRegion(w, id);
        return pr != null
                && (!pr.contains(to.getBlockX(), to.getBlockY(), to.getBlockZ())
                && pr.contains(from.getBlockX(), from.getBlockY(), from.getBlockZ()));
    }

    @Override
    public boolean setFlag(WorldGuardRegion region, String flag, boolean enable) {
        return setFlag(region.getRegionWorld(), region.getID(), flag, enable);
    }

    @Override
    public Flag<?> getWGFlag(String flagString) {
        for (Flag<?> f : DefaultFlag.getFlags()) {
            if (f.getName().equalsIgnoreCase(flagString)) {
                return f;
            }
        }
        throw new IllegalStateException("Worldguard flag " + flagString + " not found");
    }

    @Override
    public StateFlag getStateFlag(String flagString) {
        for (Flag<?> f : DefaultFlag.getFlags()) {
            if (f.getName().equalsIgnoreCase(flagString) && f instanceof StateFlag) {
                return (StateFlag) f;
            }
        }
        throw new IllegalStateException("Worldguard flag " + flagString + " not found");
    }

    @Override
    public boolean setFlag(String worldName, String id, String flag, boolean enable) {
        World w = Bukkit.getWorld(worldName);
        if (w == null) {
            return false;
        }
        ProtectedRegion pr = getRegion(w, id);
        if (pr == null) {
            return false;
        }
        StateFlag f = getStateFlag(flag);
        StateFlag.State newState = enable ? StateFlag.State.ALLOW : StateFlag.State.DENY;
        StateFlag.State state = pr.getFlag(f);

        if (state == null || state != newState) {
            pr.setFlag(f, newState);
        }
        return true;
    }

    @Override
    public boolean allowEntry(Player player, String regionWorld, String id) {
        World w = Bukkit.getWorld(regionWorld);
        if (w == null) {
            return false;
        }
        ProtectedRegion pr = getRegion(w, id);
        if (pr == null) {
            return false;
        }
        DefaultDomain dd = pr.getMembers();
        dd.addPlayer(player.getName());
        pr.setMembers(dd);
        return true;
    }

    @Override
    public boolean addMember(String playerName, WorldGuardRegion region) {
        return addMember(playerName, region.getRegionWorld(), region.getID());
    }

    @Override
    public boolean addMember(String playerName, String regionWorld, String id) {
        return changeMember(playerName, regionWorld, id, true);
    }

    @Override
    public boolean removeMember(String playerName, WorldGuardRegion region) {
        return removeMember(playerName, region.getRegionWorld(), region.getID());
    }

    @Override
    public boolean removeMember(String playerName, String regionWorld, String id) {
        return changeMember(playerName, regionWorld, id, false);
    }

    private boolean changeMember(String name, String regionWorld, String id, boolean add) {
        World w = Bukkit.getWorld(regionWorld);
        if (w == null) {
            return false;
        }
        ProtectedRegion pr = getRegion(w, id);
        if (pr == null) {
            return false;
        }

        DefaultDomain dd = pr.getMembers();
        if (add) {
            dd.addPlayer(name);
        } else {
            dd.removePlayer(name);
        }
        pr.setMembers(dd);
        return true;
    }
    
    protected void printError(LocalPlayer player, String msg) {
        if (player == null) {
            System.out.println(msg);
        } else {
            player.printError(msg);
        }
    }

    @Override
    public boolean contains(Location location, WorldGuardRegion region) {
        ProtectedRegion pr = getRegion(region.getWorldName(), region.getID());
        return pr != null
                && pr.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    public boolean hasPlayer(String playerName, WorldGuardRegion region) {
        ProtectedRegion pr = getRegion(region.getWorldName(), region.getID());
        if (pr == null) {
            return true;
        }
        DefaultDomain dd = pr.getMembers();
        if (dd.contains(playerName)) {
            return true;
        }
        dd = pr.getOwners();
        return dd.contains(playerName);
    }

    @Override
    public boolean trackRegion(ArenaRegion region) throws RegionNotFound {
        return trackRegion(region.getWorldName(), region.getID());
    }

    @Override
    public boolean trackRegion(String world, String id) throws RegionNotFound {
        ProtectedRegion pr = getRegion(world, id);
        if (pr == null) {
            throw new RegionNotFound("The region " + id + " not found in world " + world);
        }
        Set<String> regions = trackedRegions.get(world);
        if (regions == null) {
            regions = new CopyOnWriteArraySet<String>();
            trackedRegions.put(world, regions);
        }
        return regions.add(id);
    }

    @Override
    public int regionCount() {
        if (trackedRegions.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String world : trackedRegions.keySet()) {
            Set<String> sets = trackedRegions.get(world);
            if (sets != null) {
                count += sets.size();
            }
        }
        return count;
    }

    @Override
    public WorldGuardRegion getContainingRegion(Location location) {
        for (String world : trackedRegions.keySet()) {
            World w = Bukkit.getWorld(world);
            if (w == null || location.getWorld().getUID() != w.getUID()) {
                continue;
            }
            for (String id : trackedRegions.get(world)) {
                ProtectedRegion pr = getRegion(w, id);
                if (pr == null) {
                    continue;
                }
                if (pr.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ())) {
                    return new WorldGuardRegion(world, id);
                }
            }
        }
        return null;
    }
    
    @Override
    public boolean pasteSchematic(WorldGuardRegion region) {
        return pasteSchematic(region.getRegionWorld(), region.getID());
    }
    
    @Override
    public boolean pasteSchematic(String worldName, String id) {
        return pasteSchematic(Bukkit.getConsoleSender(), worldName, id);
    }

    @Override
    public boolean pasteSchematic(CommandSender consoleSender, String worldName, String id) {
        World w = Bukkit.getWorld(worldName);
        if (w == null) {
            return false;
        }
        ProtectedRegion pr = getRegion(w, id);
        return pr != null && pasteSchematic(consoleSender, pr, id, w);
    }
    
    @Override
    public boolean pasteSchematic(CommandSender sender, ProtectedRegion pr, String schematic, World world) {
        return pasteSchematic(sender, pr.getMinimumPoint(), schematic, world);
    }
    
}
