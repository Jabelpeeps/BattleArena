package mc.alk.arena;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.util.Log;

public class Permissions {
	/* 
	 *  BattleArena permissions
	 *  many of these are dynamically created and aren't included here
	 *  Examples
	 *		arena.<type>.add
	 */
    
	public static final String ADMIN_NODE = "arena.admin";

    public static final String TRACKER_ADMIN = "tracker.admin";
	public static final String DUEL_EXEMPT = "arena.duel.exempt";
	public static final String TELEPORT_BYPASS_PERM = "arena.teleport.bypass";

	/// Permissions for other plugins
	public static final String MULTI_INV_IGNORE_NODE = "multiinv.exempt";
	public static final String MULTIVERSE_INV_IGNORE_NODE = "mvinv.bypass.*";
	public static final String MULTIVERSE_CORE_IGNORE_NODE = "mv.bypass.gamemode.*";
	public static final String WORLDGUARD_BYPASS_NODE = "worldguard.region.bypass.";
	
    static final int ticks = 0;

    public static void givePlayerInventoryPerms(ArenaPlayer p) {
        givePlayerInventoryPerms(p.getPlayer());
    }

    public static void givePlayerInventoryPerms(Player p) {
        if (BattleArena.getSelf().isEnabled()) {
            if (Defaults.DEBUG_PERMS) Log.info("Giving inventory perms=" + p.getName());

            if (Defaults.PLUGIN_MULTI_INV) /// Give the multiinv permission node to ignore this player
                p.getPlayer().addAttachment(BattleArena.getSelf(), Permissions.MULTI_INV_IGNORE_NODE, true, ticks);
            
            if (Defaults.PLUGIN_MULITVERSE_CORE) /// Give the multiverse-core permission node to ignore this player
                p.getPlayer().addAttachment(BattleArena.getSelf(), Permissions.MULTIVERSE_CORE_IGNORE_NODE, true, ticks);
            
            if (Defaults.PLUGIN_MULITVERSE_INV) /// Give the multiverse-inventories permission node to ignore this player
                p.getPlayer().addAttachment(BattleArena.getSelf(), Permissions.MULTIVERSE_INV_IGNORE_NODE, true, ticks);
            
            if (Defaults.DEBUG_PERMS) Log.info("End giving inventory perms=" + p.getName());
        }
    }

    public static int getPriority(ArenaPlayer ap) {
        if (ap.hasPermission("arena.priority.lowest")){ return 1000;}
        else if (ap.hasPermission("arena.priority.low")){ return 800;}
        else if (ap.hasPermission("arena.priority.normal")){ return 600;}
        else if (ap.hasPermission("arena.priority.high")){ return 400;}
        else if (ap.hasPermission("arena.priority.highest")){ return 200;}
        return 1000;
    }

    public static boolean isAdmin(CommandSender sender){
        return     sender.isOp() || sender.hasPermission(Permissions.ADMIN_NODE);
    }

    public static boolean hasTeamPerm(CommandSender sender, MatchParams mp, Integer teamIndex) {
        return     sender.hasPermission( "arena.join.team.all" ) 
                || sender.hasPermission( "arena.join." + mp.getName().toLowerCase() + ".team.all" ) 
                || sender.hasPermission( "arena.join." + mp.getName().toLowerCase() + ".team." + ( teamIndex + 1 ) ) 
                || sender.hasPermission( "arena.join." + mp.getCommand().toLowerCase() + ".team." + ( teamIndex + 1 ) );
    }

    public static boolean hasMatchPerm(CommandSender sender, MatchParams mp, String perm) {
        return     sender.hasPermission( "arena." + mp.getName().toLowerCase() + "." + perm ) 
                || sender.hasPermission( "arena." + mp.getCommand().toLowerCase() + "." + perm ) 
                || sender.hasPermission( "arena." + perm + "." + mp.getName().toLowerCase() ) 
                || sender.hasPermission( "arena." + perm + "." + mp.getCommand().toLowerCase() );
    }
}
