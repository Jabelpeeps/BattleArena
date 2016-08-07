package mc.alk.arena.controllers.plugins;

import mc.alk.arena.objects.exceptions.RegionNotFound;
import mc.alk.arena.objects.regions.ArenaRegion;
import mc.alk.arena.objects.regions.WorldGuardRegion;
import mc.alk.arena.plugins.worldedit.WorldEditUtil;
import mc.alk.arena.plugins.worldguard.WorldGuardInterface;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * @author alkarin
 *
 * The key to these optional dependencies(OD) seem to be there can be no direct
 * function call to a method that USES any of the OD classes. So this entire
 * class is just a wrapper for functions. Also other classes should not declare
 * variables of the OD as a class variable
 *
 */
public class WorldGuardController {

    static boolean hasWorldGuard = false;
    static boolean hasWorldEdit = false;
    
    public static final WorldGuardInterface wg = WorldGuardInterface.newInstance();
    
    public static class WorldGuardException extends Exception {

        private static final long serialVersionUID = 1L;

        public WorldGuardException(String msg) {
            super(msg);
        }
    }

    public static boolean hasWorldGuard() {
        return Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
    }

    public static boolean hasWorldEdit() {
        return Bukkit.getPluginManager().getPlugin("WorldEdit") != null;
    }

    public boolean addRegion(Player sender, String id) throws Exception {
        return wg.createProtectedRegion(sender, id) != null;
    }

    public static boolean hasRegion(ArenaRegion region) {
        return wg.hasRegion(region);
    }

    public static boolean hasRegion(World world, String id) {
        return wg.hasRegion(world, id);
    }

    public static boolean hasRegion(String world, String id) {
        return wg.hasRegion(world, id);
    }

    public static void updateProtectedRegion(Player p, String id) throws Exception {
        wg.updateProtectedRegion(p, id);
    }

    public static WorldGuardRegion createProtectedRegion(Player p, String id) throws Exception {
        wg.createProtectedRegion(p, id);
        return wg.hasRegion(p.getWorld(), id)
                ? new WorldGuardRegion(p.getWorld().getName(), id) : null;
    }

    public static void clearRegion(String world, String id) {
        wg.clearRegion(world, id);
    }

    public static void clearRegion(WorldGuardRegion region) {
        wg.clearRegion(region);
    }

    public static boolean isLeavingArea(final Location from, final Location to, ArenaRegion region) {
        return wg.isLeavingArea(from, to, region);
    }

    public static boolean isLeavingArea(final Location from, final Location to, World w, String id) {
        return wg.isLeavingArea(from, to, w, id);
    }

    public static boolean setWorldGuard(Plugin plugin) {
        hasWorldGuard = wg.setWorldGuard(plugin);
        return hasWorldGuard;
    }

    public static boolean setWorldEdit(Plugin plugin) {
        hasWorldEdit = WorldEditUtil.setWorldEdit(plugin);
        return hasWorldEdit;
    }

    public static boolean setFlag(WorldGuardRegion region, String flag, boolean enable) {
        return wg.setFlag(region, flag, enable);
    }

    public static boolean setFlag(String worldName, String id, String flag, boolean enable) {
        return wg.setFlag(worldName, id, flag, enable);
    }

    public static void allowEntry(Player player, String regionWorld, String id) {
        wg.allowEntry(player, regionWorld, id);
    }

    public static void addMember(String playerName, WorldGuardRegion region) {
        wg.addMember(playerName, region);
    }

    public static void addMember(String playerName, String regionWorld, String id) {
        wg.addMember(playerName, regionWorld, id);
    }

    public static void removeMember(String playerName, WorldGuardRegion region) {
        wg.removeMember(playerName, region);
    }

    public static void removeMember(String playerName, String regionWorld, String id) {
        wg.removeMember(playerName, regionWorld, id);
    }

    public static void deleteRegion(String worldName, String id) {
        wg.deleteRegion(worldName, id);
    }

    public static void saveSchematic(Player p, String id) {
        wg.saveSchematic(p, id);
    }

    public static void pasteSchematic(CommandSender sender, String regionWorld, String id) {
        wg.pasteSchematic(sender, regionWorld, id);
    }

    public static void pasteSchematic(String regionWorld, String id) {
        wg.pasteSchematic(regionWorld, id);
    }

    public static void pasteSchematic(WorldGuardRegion region) {
        wg.pasteSchematic(region);
    }

    public static boolean regionContains(Location location, WorldGuardRegion region) {
        return wg.contains(location, region);
    }

    public static boolean hasPlayer(String playerName, WorldGuardRegion region) {
        return wg.hasPlayer(playerName, region);
    }

    public static int regionCount() {
        return wg.regionCount();
    }

    public static WorldGuardRegion getContainingRegion(Location location) {
        return wg.getContainingRegion(location);
    }

    public static boolean trackRegion(WorldGuardRegion region) throws RegionNotFound {
        return wg.trackRegion(region);
    }

    public static boolean trackRegion(String world, String id) throws RegionNotFound {
        return wg.trackRegion(world, id);
    }
}
