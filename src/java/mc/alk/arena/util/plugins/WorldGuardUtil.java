package mc.alk.arena.util.plugins;

import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import mc.alk.arena.controllers.plugins.WorldGuardController;
import mc.alk.arena.objects.exceptions.RegionNotFound;
import mc.alk.arena.objects.regions.WorldGuardRegion;
import mc.alk.arena.plugins.worldguard.WorldGuardInterface;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Stub class for future expansion
 *
 * @author alkarin
 */
@Deprecated
public class WorldGuardUtil {
    
    public static final WorldGuardInterface wg = WorldGuardInterface.newInstance();
    public static boolean hasWorldGuard = false;

    public static boolean hasWorldGuard() {
        return Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
    }

    public static ProtectedRegion getRegion(String world, String id) {
        return wg.getRegion(world, id);
    }

    public static ProtectedRegion getRegion(World w, String id) {
        return wg.getRegion(w, id);
    }

    public static boolean hasRegion(WorldGuardRegion region) {
        return WorldGuardController.hasRegion(region);
    }

    public static boolean hasRegion(World world, String id) {
        return WorldGuardController.hasRegion(world, id);
    }

    public static boolean hasRegion(String world, String id) {
        return WorldGuardController.hasRegion(world, id);
    }


    public static ProtectedRegion updateProtectedRegion(Player p, String id) throws Exception {
        return wg.updateProtectedRegion(p, id);
    }

    public static ProtectedRegion createProtectedRegion(Player p, String id) throws Exception {
        return wg.createProtectedRegion(p, id);
    }

    public static void clearRegion(WorldGuardRegion region) {
        WorldGuardController.clearRegion(region);
    }

    public static void clearRegion(String world, String id) {
        WorldGuardController.clearRegion(world, id);
    }

    public static boolean isLeavingArea(final Location from, final Location to, final World w, String id) {
        return WorldGuardController.isLeavingArea(from, to, w, id);
    }

    public static boolean setFlag(WorldGuardRegion region, String flag, boolean enable) {
        return WorldGuardController.setFlag(region, flag, enable);
    }

    public static Flag<?> getWGFlag(String flagString) {
        return wg.getWGFlag(flagString);
    }

    public static StateFlag getStateFlag(String flagString) {
        return wg.getStateFlag(flagString);
    }

    public static boolean setFlag(String worldName, String id, String flag, boolean enable) {
        return WorldGuardController.setFlag(worldName, id, flag, enable);
    }

    public static boolean setWorldGuard(Plugin plugin) {
        hasWorldGuard = true;
        return WorldGuardController.setWorldGuard(plugin);
    }

    public static boolean allowEntry(Player player, String regionWorld, String id) {
        return wg.allowEntry(player, regionWorld, id);
    }

    public static boolean addMember(String playerName, WorldGuardRegion region) {
        return wg.addMember(playerName, region);
    }

    public static boolean addMember(String playerName, String regionWorld, String id) {
        return wg.addMember(playerName, regionWorld, id);
    }

    public static boolean removeMember(String playerName, WorldGuardRegion region) {
        return wg.removeMember(playerName, region);
    }

    public static boolean removeMember(String playerName, String regionWorld, String id) {
        return wg.removeMember(playerName, regionWorld, id);
    }

    public static void deleteRegion(String worldName, String id) {
        WorldGuardController.deleteRegion(worldName, id);
    }


    public static boolean pasteSchematic(CommandSender consoleSender, String worldName, String id) {
        return wg.pasteSchematic(consoleSender, worldName, id);
    }

    public static boolean pasteSchematic(WorldGuardRegion region) {
        return wg.pasteSchematic(region);
    }

    public static boolean pasteSchematic(String worldName, String id) {
        return wg.pasteSchematic(worldName, id);
    }

    public static boolean pasteSchematic(CommandSender sender, ProtectedRegion pr, String schematic, World world) {
        return wg.pasteSchematic(sender, pr, schematic, world);
    }

    public static boolean saveSchematic(Player p, String schematicName) {
        return wg.saveSchematic(p, schematicName);
    }
    
    public static boolean contains(Location location, WorldGuardRegion region) {
        return wg.contains(location, region);
    }

    public static boolean hasPlayer(String playerName, WorldGuardRegion region) {
        return WorldGuardController.hasPlayer(playerName, region);
    }

    public static boolean trackRegion(WorldGuardRegion region) throws RegionNotFound {
        return WorldGuardController.trackRegion(region);
    }

    public static boolean trackRegion(String world, String id) throws RegionNotFound {
        return WorldGuardController.trackRegion(world, id);
    }

    public static int regionCount() {
        return WorldGuardController.regionCount();
    }

    public static WorldGuardRegion getContainingRegion(Location location) {
        return WorldGuardController.getContainingRegion(location);
    }
    
    /**
     * Deleted in order to hide the implementation details between WE v5 & v6.
     * 
     * Safely deleted since BattleArena never calls this method directly.
     * Commented out for possible future resurrection.
     * Anyone can still use this method by simply calling pasteSchematic() instead.
     */
    /*
    public static boolean loadAndPaste(CommandContext args, WorldEdit we,
                                       LocalSession session, com.sk89q.worldedit.LocalPlayer player, EditSession editSession, Vector pos) {
        return wg.loadAndPaste(args, we, session, player, editSession, pos);
    }
    */

}
