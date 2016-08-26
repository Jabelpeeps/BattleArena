package mc.alk.arena.plugins;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalPlayer;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.Polygonal2DSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.io.file.FilenameException;
import com.sk89q.worldedit.world.registry.LegacyWorldData;
import com.sk89q.worldedit.world.registry.WorldData;
import com.sk89q.worldguard.bukkit.RegionContainer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import mc.alk.arena.objects.exceptions.RegionNotFound;
import mc.alk.arena.objects.regions.ArenaRegion;
import mc.alk.arena.objects.regions.WorldGuardRegion;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.VersionFactory;

public class WorldGuardController {
  
    private static WorldGuardPlugin wgp;

    private static Map<String, Set<String>> trackedRegions = new ConcurrentHashMap<>(); 
    
    static {
        checkIfLoadedYet();
    }

    public static boolean checkIfLoadedYet() {
        wgp = (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin( "WorldGuard" );

        if ( !VersionFactory.getPluginVersion( "WorldGuard" ).isCompatible( "6" ) ) {
            wgp = null;
            Log.warn( "WorldGuard version 6 is the minimum that is supported." );
        }

        return hasWorldGuard();
    }
    
    public static boolean hasWorldGuard() {
        return wgp != null;
    }
    
    public static boolean saveSchematic( Player p, String schematicName) {
        
        WorldEditPlugin wep = WorldEditUtil.getWorldEditPlugin();
        LocalSession session = wep.getSession(p);
        LocalPlayer player = wep.wrapPlayer(p);
        Extent editSession = (Extent) session.createEditSession(player);
        
        try {
            Region region = session.getSelection( player.getWorld() );
            Clipboard cb = new BlockArrayClipboard(region);
            Operations.completeLegacy( new ForwardExtentCopy( editSession, region, cb, region.getMinimumPoint() ) );
            LocalConfiguration config = wep.getWorldEdit().getConfiguration();
            File dir = wep.getWorldEdit().getWorkingDirectoryFile( config.saveDir );
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    throw new IOException("Could not create directory " + config.saveDir);
                }
            }
            File schematicFile = new File(dir, schematicName + ".schematic");
            schematicFile.createNewFile();
            
            try ( ClipboardWriter writer = 
                    ClipboardFormat.SCHEMATIC.getWriter( 
                            new BufferedOutputStream( new FileOutputStream( schematicFile ) ) ); ) {
            
                writer.write( cb, LegacyWorldData.getInstance() );
                return true;
            }
        } catch ( IOException | IncompleteRegionException | MaxChangedBlocksException ex ) {
            ex.printStackTrace();
        } 
        return false;
    }

    public static boolean pasteSchematic(CommandSender sender, Vector position, String schematic, World world) {
        
        final WorldEditPlugin wep = WorldEditUtil.getWorldEditPlugin();
        final WorldEdit we = wep.getWorldEdit();
        LocalPlayer actor = wep.wrapCommandSender(sender);
        
        com.sk89q.worldedit.world.World w = (com.sk89q.worldedit.world.World) new BukkitWorld(world);
        
        WorldData wd = w.getWorldData();
        final LocalSession session = we.getSession( actor );
        session.setUseInventory(false);
        EditSession editSession = new EditSession((LocalWorld) w, -1);
        
        try {
            return loadAndPaste(schematic, we, session, wd, editSession, position);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * This is just copied and pasted from world edit source, with small changes
     * to also paste.
     *
     * @param schematic String filename
     * @param we WorldEdit
     * @param session LocalSession
     * @param worldData WorldData
     * @param editSession EditSession
     * @param location Vector
     * @return
     * @throws com.sk89q.worldedit.util.io.file.FilenameException
     */
    public static boolean loadAndPaste( String schematic, WorldEdit we, LocalSession session, WorldData worldData, 
                                    EditSession editSession, Vector location ) throws FilenameException {

        String filename = schematic + ".schematic";
        LocalConfiguration config = we.getConfiguration();
        File dir = we.getWorkingDirectoryFile(config.saveDir);
        File f = new File(dir, filename);

        if (!f.exists()) {
            System.out.println("Schematic " + filename + " does not exist!");
            return false;
        }

        ClipboardFormat fileFormat = ClipboardFormat.findByFile(f);
        ClipboardFormat aliasFormat = ClipboardFormat.findByAlias("mcedit");
        ClipboardFormat format = (fileFormat == null) ? aliasFormat : fileFormat;
        
        if (format == null) {
            System.out.println("Unknown schematic format for file " + f.getName());
            return false;
        }

        try {
            String filePath = f.getCanonicalPath();
            String dirPath = dir.getCanonicalPath();

            if (!filePath.substring(0, dirPath.length()).equals(dirPath)) {
                System.out.println("Clipboard file could not read or it does not exist.");
            } 
            else {
                try ( BufferedInputStream bis = new BufferedInputStream( new FileInputStream(f) ) ) {
                    Clipboard clipboard = format.getReader( bis ).read( worldData );
                    session.setClipboard( (CuboidClipboard) clipboard );
                }
            }

            CuboidClipboard holder = session.getClipboard();
            if (holder != null )
                holder.paste(editSession, location, false);

        } catch (IOException e) {
            System.out.println("Schematic could not be read or it does not exist:");
            e.printStackTrace();
        } catch (MaxChangedBlocksException e) {
            System.out.println("MaxChangedBlocksException");
            e.printStackTrace();
        } catch (EmptyClipboardException ex) { }
        return true;
    }

    public static void deleteRegion(String worldName, String id) {
        
        World w = Bukkit.getWorld(worldName);
        
        if ( w == null )  return;

        RegionManager mgr = wgp.getRegionManager(w);
        
        if ( mgr == null ) return;

        mgr.removeRegion(id);
    }

    public static ProtectedRegion getRegion(String world, String id) {
        return getRegion( Bukkit.getWorld( world ), id );
    }

    public static ProtectedRegion getRegion( World w, String id ) {
        if (w == null) {
            return null;
        }
        return wgp.getRegionManager( w ).getRegion( id );
    }

    public static boolean hasRegion( ArenaRegion region ) {
        return hasRegion( region.getWorldName(), region.getID() );
    }

    public static boolean hasRegion(String world, String id) {
        World w = Bukkit.getWorld( world );
        
        if ( w == null ) return false;

        return wgp.getRegionContainer().get( w ).hasRegion( id );
    }

    public static ProtectedRegion updateProtectedRegion(Player p, String id) throws Exception {
        return createRegion(p, id);
    }

    public static ProtectedRegion createRegion( Player p, String id ) throws Exception {
        Selection sel = WorldEditUtil.getSelection(p);
        World w = sel.getWorld();
        RegionContainer gmanager = wgp.getRegionContainer();
        RegionManager regionManager = gmanager.get(w);
        deleteRegion( w.getName(), id );
        ProtectedRegion region;
        
        // Detect the type of region from WorldEdit
        if ( sel instanceof Polygonal2DSelection ) {
            Polygonal2DSelection polySel = (Polygonal2DSelection) sel;
            int minY = polySel.getNativeMinimumPoint().getBlockY();
            int maxY = polySel.getNativeMaximumPoint().getBlockY();
            region = new ProtectedPolygonalRegion(id, polySel.getNativePoints(), minY, maxY);
        } 
        else { /// default everything to cuboid
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

    public static void clearRegion(WorldGuardRegion region) {
        clearRegion( region.getRegionWorld(), region.getID() );
    }

    public static void clearRegion(String world, String id) {
        World w = Bukkit.getWorld( world );
        
        if ( w == null ) return;

        ProtectedRegion region = getRegion( w, id );
        
        if ( region == null ) return;
        
        Location l;
        for ( Entity entity : w.getEntitiesByClasses( Item.class, Creature.class ) ) {
            l = entity.getLocation();
            if ( region.contains( l.getBlockX(), l.getBlockY(), l.getBlockZ() ) ) {
                entity.remove();
            }
        }
    }

    public static boolean isLeavingArea(final Location from, final Location to, final World w, String id) {
        ProtectedRegion pr = getRegion(w, id);
        return  pr != null
                && !pr.contains( to.getBlockX(), to.getBlockY(), to.getBlockZ() )
                && pr.contains( from.getBlockX(), from.getBlockY(), from.getBlockZ() );
    }

    public static boolean setFlag(WorldGuardRegion region, String flag, boolean enable) {
        return setFlag( region.getRegionWorld(), region.getID(), flag, enable );
    }

    public static StateFlag getStateFlag(String flagString) {
        for ( Flag<?> f : DefaultFlag.getFlags() ) {
            if ( f.getName().equalsIgnoreCase( flagString ) && f instanceof StateFlag ) {
                return (StateFlag) f;
            }
        }
        throw new IllegalStateException("Worldguard flag " + flagString + " not found");
    }

    public static boolean setFlag(String worldName, String id, String flag, boolean enable) {
        World w = Bukkit.getWorld( worldName );
        
        if ( w == null ) return false;

        ProtectedRegion pr = getRegion( w, id );
        
        if ( pr == null ) return false;

        StateFlag f = getStateFlag( flag );
        StateFlag.State newState = enable ? StateFlag.State.ALLOW 
                                          : StateFlag.State.DENY;
        StateFlag.State state = pr.getFlag(f);

        if ( state == null || state != newState ) {
            pr.setFlag( f, newState );
        }
        return true;
    }

    public static boolean addMember(String playerName, WorldGuardRegion region) {
        return addMember(playerName, region.getRegionWorld(), region.getID());
    }

    public static boolean addMember(String playerName, String regionWorld, String id) {
        return changeMember(playerName, regionWorld, id, true);
    }

    public static boolean removeMember(String playerName, WorldGuardRegion region) {
        return removeMember(playerName, region.getRegionWorld(), region.getID());
    }

    public static boolean removeMember(String playerName, String regionWorld, String id) {
        return changeMember(playerName, regionWorld, id, false);
    }

    private static boolean changeMember(String name, String regionWorld, String id, boolean add) {
        World w = Bukkit.getWorld(regionWorld);
        
        if ( w == null ) return false;
        
        ProtectedRegion pr = getRegion( w, id );
        
        if ( pr == null ) return false;

        DefaultDomain dd = pr.getMembers();
        
        if ( add ) 
            dd.addPlayer(name);
        else
            dd.removePlayer(name);
        
        pr.setMembers( dd );
        return true;
    }

    public boolean contains(Location location, WorldGuardRegion region) {
        ProtectedRegion pr = getRegion(region.getWorldName(), region.getID());
        return pr != null
                && pr.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static boolean hasPlayer(String playerName, WorldGuardRegion region) {
        ProtectedRegion pr = getRegion(region.getWorldName(), region.getID());
        
        if ( pr == null ) return true;

        DefaultDomain dd = pr.getMembers();
        
        if ( dd.contains(playerName) ) return true;

        dd = pr.getOwners();
        return dd.contains(playerName);
    }

    public static boolean trackRegion(ArenaRegion region) throws RegionNotFound {
        return trackRegion(region.getWorldName(), region.getID());
    }

    public static boolean trackRegion(String world, String id) throws RegionNotFound {
        ProtectedRegion pr = getRegion(world, id);
        if ( pr == null ) {
            throw new RegionNotFound("The region " + id + " not found in world " + world);
        }
        Set<String> regions = trackedRegions.get(world);
        if ( regions == null ) {
            regions = new CopyOnWriteArraySet<>();
            trackedRegions.put(world, regions);
        }
        return regions.add(id);
    }

    public static int regionCount() {
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

    public static WorldGuardRegion getContainingRegion(Location location) {
        for ( String world : trackedRegions.keySet() ) {
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
    
    public static boolean pasteSchematic(WorldGuardRegion region) {
        return pasteSchematic(region.getRegionWorld(), region.getID());
    }
    
    public static boolean pasteSchematic(String worldName, String id) {
        return pasteSchematic(Bukkit.getConsoleSender(), worldName, id);
    }

    public static boolean pasteSchematic(CommandSender consoleSender, String worldName, String id) {
        World w = Bukkit.getWorld(worldName);
        if (w == null) {
            return false;
        }
        ProtectedRegion pr = getRegion(w, id);
        return pr != null && pasteSchematic(consoleSender, pr, id, w);
    }
    
    public static boolean pasteSchematic(CommandSender sender, ProtectedRegion pr, String schematic, World world) {
        return pasteSchematic(sender, pr.getMinimumPoint(), schematic, world);
    }
}      
