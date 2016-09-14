package mc.alk.arena.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import mc.alk.arena.Permissions;
import mc.alk.arena.controllers.Scheduler;
import mc.alk.arena.objects.tracker.StatSign;
import mc.alk.arena.objects.tracker.StatSign.SignType;
import mc.alk.arena.objects.tracker.StatType;
import mc.alk.arena.tracker.SignController;
import mc.alk.arena.tracker.Tracker;
import mc.alk.arena.tracker.TrackerMessageController;
import mc.alk.arena.util.AutoClearingTimer;
import mc.alk.arena.util.MessageUtil;

public class BTSignListener implements Listener{
	SignController signController;
	AutoClearingTimer<String> timer = new AutoClearingTimer<>();
	public static final int SECONDS = 5;
	public BTSignListener(SignController _signController){
		signController = _signController;
		timer.setSaveEvery(61050); /// will auto clear records every minute, and 1sec
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.isCancelled() || event.getClickedBlock() == null)
            return;
		final Block block = event.getClickedBlock();
		final Material type = block.getType();
		if (!(type.equals(Material.SIGN) || type.equals(Material.SIGN_POST) || type.equals(Material.WALL_SIGN))) {
			return ;}
		StatSign ss = signController.getStatSign(event.getClickedBlock().getLocation());
		if (ss == null)
			return;
		if (timer.withinTime(event.getPlayer().getName(),SECONDS*1000L)){
			event.getPlayer().sendMessage(ChatColor.RED+"wait");
			return;
		}
        timer.put(event.getPlayer().getName());
        final Location l = block.getLocation();
        Scheduler.scheduleSynchronousTask( 
                                () -> l.getWorld().getBlockAt(l).getState().update(true), SECONDS * 20 );
        
		signController.clickedSign(event.getPlayer(), (Sign) block.getState(), ss);
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event){
		final Block block = event.getBlock();
		final Material type = block.getType();
		if (!(type.equals(Material.SIGN) || type.equals(Material.SIGN_POST) || type.equals(Material.WALL_SIGN))) {
			return;}
		Sign s = (Sign)block.getState();
		final String l = s.getLine(0);
		if (l == null || l.isEmpty() || l.charAt(0) != '[')
			return;
		signController.removeSignAt(s.getLocation());
	}

	@EventHandler
	public void onSignChange(SignChangeEvent event){
		final Block block = event.getBlock();
		final Material type = block.getType();
		if (!(type.equals(Material.SIGN) || type.equals(Material.SIGN_POST) || type.equals(Material.WALL_SIGN))) {
			return;}
		StatSign ss;
		try {
			ss = getStatSign(block.getLocation(), event.getLines());
		} catch (InvalidSignException e) {
			TrackerMessageController.sendMessage(event.getPlayer(), e.getMessage());
			return;
		}
		if (ss == null){
			return;}
		if (!event.getPlayer().hasPermission( Permissions.TRACKER_ADMIN ) && !event.getPlayer().isOp()){
			TrackerMessageController.sendMessage(event.getPlayer(), "&cYou don't have perms to create top signs");
			cancelSignPlace(event, block);
			return;
		}
		String lines[] = event.getLines();
		lines[0] = "[&e"+ss.getDBName()+"&0]";
		lines[1] = "[&e"+ss.getStatType()+"&0]";
		lines[2] = "&2Updating";
		for (int i=0;i<lines.length;i++){
			lines[i] = MessageUtil.colorChat(lines[i]);
		}
		signController.addSign(ss);
		Scheduler.scheduleSynchronousTask( 
		        () -> {
        				signController.updateSigns();
        				Tracker.saveConfig();
	                }, 40);
	}

	private StatSign getStatSign(Location l, String lines[]) throws InvalidSignException {
		/// Quick check to make sure this is even a stat sign
		/// make sure first two lines are not null or empty.. line 1 starts with '['
		if (lines.length < 2 ||
				lines[0] == null || lines[0].isEmpty() || lines[0].charAt(0) != '[' ||
				lines[1] == null || lines[1].isEmpty()){
			return null;}

		/// find the Sign Type, like top, personal
		String strType = lines[1].replace('[', ' ').replace(']', ' ').trim();
		SignType signType = SignType.fromName(strType);
		if (signType == null)
			return null;

		/// find the database
		String db = lines[0].replace('[', ' ').replace(']', ' ').trim();
		if (!Tracker.hasInterface(db)){
			throw new InvalidSignException("Tracker Database " + db +" not found");}
		StatType st = StatType.fromName(strType);
		if (st == null){
			return null;}
		StatSign ss = new StatSign(db, l, SignType.TOP); /// TODO change when we have more than 1 type
		ss.setStatType(st);
		return ss;
	}

	public static void cancelSignPlace(SignChangeEvent event, Block block){
		event.setCancelled(true);
		block.setType(Material.AIR);
		block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.SIGN, 1));
	}
	
	public class InvalidSignException extends Exception{ 
	    private static final long serialVersionUID = 1L;

	    public InvalidSignException(String message) {
	        super(message);
	    }
	}

}
