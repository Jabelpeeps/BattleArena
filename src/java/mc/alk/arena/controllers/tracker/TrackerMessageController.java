package mc.alk.arena.controllers.tracker;

import java.io.File;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import lombok.Getter;
import mc.alk.arena.objects.tracker.Stat.SpecialType;
import mc.alk.arena.util.InventoryUtil;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.MessageUtil;

/**
 *
 * @author alkarin
 *
 */
public class TrackerMessageController {

    @Getter private static YamlConfiguration config = new YamlConfiguration();
    @Getter static File file;
	static TrackerMessageController mc;
	static List<String> meleeMsgs = null;
	static List<String> rangeMsgs = null;
	static final Random r = new Random();
	static String TRACKER_PREFIX = "&5[Tracker]&f ";
	static String PVP_PREFIX = "&4[PvP]&f ";
	static String PVE_PREFIX = "&2[PvE]&f ";


	public static boolean setConfig(File f){
		file = f;
		return load();
	}

	public static String getMsg(String node, Object... varArgs) {
		return getMsg( TRACKER_PREFIX, "messages." + node, varArgs );
	}

	public static String getMsgNP(String node, Object... varArgs) {
		return getMsg( null, "messages." + node, varArgs );
	}

	private static String getMsg(String prefix, String node, Object... varArgs) {
	    
		if (!config.contains(node)){
			Log.err( "Message " + prefix + "." + node + " is not configured in the tracker_messages.yml");
			return null;
		}
		String msg = config.getString(node);
		StringBuilder buf = new StringBuilder();
		if (prefix != null)
			buf.append(prefix);
		try ( Formatter form = new Formatter(buf) ) {
			form.format(msg, varArgs);
		} 
		catch(Exception e){
		    Log.err( "Error getting message " + prefix + "." + node );
			for (Object o: varArgs){ Log.err( "argument=" + o ); }
			e.printStackTrace();
		}
		return MessageUtil.colorChat(buf.toString());
	}

	public static boolean sendMessage(Player p, String message){
		if (message ==null) return true;
		String[] msgs = message.split("\n");
		for (String msg: msgs){
			if (p == null){
				Log.info( MessageUtil.colorChat(msg));
			} else {
				p.sendMessage( MessageUtil.colorChat(msg));
			}
		}
		return true;
	}

	public static boolean sendMessage(CommandSender p, String message){
		if (message ==null) return true;
		if (p instanceof Player){
			if (((Player) p).isOnline())
				p.sendMessage( MessageUtil.colorChat(message));
		} else {
			p.sendMessage( MessageUtil.colorChat(message));
		}
		return true;
	}

	public static String getPvEMessage(boolean melee, String killer, String target, String weapon){
		String node=null;
		List<String> messages = null;
		if (killer != null){
			node = "pve." + killer.toLowerCase();
			messages = config.getStringList(node);
		}
		if (messages == null || messages.isEmpty()){
			node = melee ? "pve.meleeDeaths" : "pve.rangeDeaths";
			messages = config.getStringList(node);
		}
		if (messages == null || messages.isEmpty()){
		    Log.err( "[Tracker] getPvEMessage, message node="+node +"  args="+ killer +":" +target+":"+weapon);
			return null;
		}
		String msg = messages.get(r.nextInt(messages.size()));
		return formatMessage(PVE_PREFIX, msg,killer,target, weapon, null);
	}

	public static String getPvPMessage(boolean melee, String killer, String target, ItemStack weapon){
		String node=null;
		List<String> messages = null;
		String wpnName = null;
                List<String> wpnLore = null;
		if (weapon != null){
			node = "pvp."+ weapon.getType().name().toLowerCase();
			wpnName = InventoryUtil.getCommonName(weapon);
                        wpnLore = weapon.getItemMeta().getLore();
			messages = config.getStringList(node);
		}
		if (messages == null || messages.isEmpty()){
			node = melee ? "pvp.meleeDeaths" : "pvp.rangeDeaths";
			messages = config.getStringList(node);
		}
		if (messages == null || messages.isEmpty()){
		    Log.err( "[Tracker] getPvPMessage, message node="+node +"  args="+ killer +":" +target+":"+weapon);
			return null;
		}

		String msg = messages.get(r.nextInt(messages.size()));
		return formatMessage(PVP_PREFIX, msg,killer,target, wpnName, null, wpnLore);
	}

	public static String getSpecialMessage(SpecialType type, int nKills, String killer, String target, ItemStack weapon){
		String node = null;
		switch (type){
		case STREAK: node = "special.streak." + nKills; break;
		case RAMPAGE: node = "special.rampage." + nKills; break;
		}
		String message = config.getString(node);
		if (message == null || message.isEmpty()){
			switch (type){
			case STREAK: node = "special.streak.default"; break;
			case RAMPAGE: node = "special.rampage.default"; break;
			}
			message = config.getString(node);
		}
		return formatMessage(PVP_PREFIX, message,killer,target, null, nKills+"");
	}

    private static String formatMessage(String prefix, String msg, String killer, String target, String item, String times) {
        return formatMessage(prefix, msg, killer, target, item, times, new ArrayList<String>());
    }

    private static String formatMessage(String prefix, String msg, String killer, String target,
                                        String item, String times, List<String> lore) {
        try {
            if (killer != null) {
                msg = StringUtils.replace(msg, "%k", killer);
            } else {
                msg = StringUtils.replace(msg, "%k", "Unknown killer");
            }
            if (target != null) {
                msg = StringUtils.replace(msg, "%d", target);
            } else {
                msg = StringUtils.replace(msg, "%d", "unknown target");
            }
            if (item != null) {
                msg = StringUtils.replace(msg, "%i", item.replace('_', ' '));
            } else {
                msg = StringUtils.replace(msg, "%i", "unknown item");
            }
            if (times != null) {
                msg = StringUtils.replace(msg, "%n", times);
            } else {
                msg = StringUtils.replace(msg, "%n", "?");
            }
            if (lore != null && !lore.isEmpty()) {
                String slore = StringUtils.join(lore, ", ");
                msg = StringUtils.replace(msg, "%l", slore);
            } else {
                msg = StringUtils.replace(msg, "%l", "???");
            }
        } catch (Exception e) {
            Log.err( "Error getting message " + msg );
            e.printStackTrace();
        }
        return MessageUtil.colorChat(prefix + msg);
    }

	public static boolean load() {
		try {
			config.load(file);
		} catch (Exception e) {
			e.printStackTrace();
		}
		TRACKER_PREFIX = config.getString("messages.prefix", TRACKER_PREFIX);
		PVP_PREFIX = config.getString("pvp.prefix", PVP_PREFIX);
		PVE_PREFIX = config.getString("pve.prefix", PVE_PREFIX);
		return true;
	}

	public static boolean contains(String string) {
		return config.contains(string);
	}
}
