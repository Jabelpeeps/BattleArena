package mc.alk.arena.listeners;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import mc.alk.arena.Defaults;
import mc.alk.arena.Permissions;
import mc.alk.arena.controllers.PlayerController;
import mc.alk.arena.objects.ArenaClass;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.signs.ArenaCommandSign;
import mc.alk.arena.objects.signs.ArenaStatusSign;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.MessageUtil;
import mc.alk.arena.util.SignUtil;

public class BASignListener implements Listener{
    SignUpdateListener sul;
    Map<String, ArenaCommandSign> signLocs = new HashMap<>();

    public BASignListener(SignUpdateListener _sul){
        sul = _sul;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        /// If this is an uninteresting block get out of here as quickly as we can
        if (event.getClickedBlock() == null ||
                !(event.getClickedBlock().getType().equals(Material.SIGN_POST) ||
                        event.getClickedBlock().getType().equals(Material.WALL_SIGN))) {
            return;
        }

        if (Permissions.isAdmin(event.getPlayer()) && (event.getAction() ==Action.LEFT_CLICK_BLOCK)){
            return;}

        if (event.getClickedBlock().getState() instanceof Sign) {
            String[] lines = ((Sign) event.getClickedBlock().getState()).getLines();
            if (!lines[0].startsWith("&") && !lines[0].startsWith("[") && !lines[0].startsWith("§")) {
                return;
            }
            ArenaCommandSign acs = signLocs.get(getKey(event.getClickedBlock().getLocation()));
            if (acs == null) {
                acs = SignUtil.getArenaCommandSign(((Sign) event.getClickedBlock().getState()),
                        ((Sign) event.getClickedBlock().getState()).getLines());
                if (acs != null){
                    signLocs.put(getKey(event.getClickedBlock().getLocation()), acs);}
            }
            if (acs == null) {
                return;
            }
            event.setCancelled(true);
            sul.addSign(acs);
            acs.performAction(PlayerController.toArenaPlayer(event.getPlayer()));
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event){
        if (Defaults.DEBUG_EVENTS) System.out.println("onSignChange Event");
        final Block block = event.getBlock();
        final Material type = block.getType();

        if (!(type.equals(Material.SIGN) || type.equals(Material.SIGN_POST) || type.equals(Material.WALL_SIGN))) {
            return;}

        Player p = event.getPlayer();

        /// Is the sign a arena class sign?
        final boolean admin = Permissions.isAdmin(p);
        String lines[] = event.getLines();
        ArenaClass ac = SignUtil.getArenaClassSign(lines);
        if (ac != null){
            if (!admin){
                cancelSignPlace(event,block);
                return;
            }
            makeArenaClassSign(event, ac, lines);
            return;
        }
        /// is the sign a command sign
        ArenaCommandSign acs = SignUtil.getArenaCommandSign((Sign)block.getState(), lines);
        if (acs != null){
            if (!admin){
                cancelSignPlace(event,block);
                return;
            }
            makeArenaCommandSign(event, acs, lines);
            return;
        }
        /// is the sign a command sign
        ArenaStatusSign ass = SignUtil.getArenaStatusSign(lines);
        if (ass != null){
            if (!admin){
                cancelSignPlace(event,block);
                return;
            }
            makeArenaStatusSign(event, ass, lines);
        }
    }

    private void makeArenaClassSign(SignChangeEvent event, ArenaClass ac, String[] lines) {
        if (ac == null)
            return;
        final Block block = event.getBlock();
        for (int i=1;i<lines.length;i++){
            if (!lines[i].isEmpty()) /// other text, not our sign
                return;
        }

        try{
            event.setLine(0, MessageUtil.colorChat("["+ac.getDisplayName()+"&0]"));
            MessageUtil.sendMessage(event.getPlayer(), "&2Arena class sign created");
        } catch (Exception e){
            MessageUtil.sendMessage(event.getPlayer(), "&cError creating Arena Class Sign");
            Log.printStackTrace(e);
            cancelSignPlace(event,block);
        }
    }

    private void makeArenaCommandSign(SignChangeEvent event, ArenaCommandSign acs, String[] lines) {
        if (acs == null)
            return;
        final Block block = event.getBlock();
        for (int i=3;i<lines.length;i++){
            if (!lines[i].isEmpty()) /// other text, not our sign
                return;
        }

        try{
            MatchParams params = acs.getMatchParams();
            String match = params.getName().toLowerCase();
            match = Character.toUpperCase(match.charAt(0)) + match.substring(1);
            String str;
            if ( params.getSignDisplayName() != null ) {
                str = MessageUtil.colorChat( params.getSignDisplayName() );
            } 
            else {
                str = MessageUtil.colorChat( "["+
                        MessageUtil.getFirstColor( params.getPrefix() ) + match + "&0]" );
            }
            if (str.length()>15){
                str = MessageUtil.colorChat( "["+
                        MessageUtil.getFirstColor( params.getPrefix() ) + params.getCommand().toLowerCase()+"&0]");
            }
            event.setLine(0, str);
            String cmd = acs.getCommand();
            cmd = Character.toUpperCase( cmd.charAt(0) ) + cmd.substring(1);
            event.setLine(1, MessageUtil.colorChat( ChatColor.GREEN+cmd.toLowerCase() ) );
            MessageUtil.sendMessage(event.getPlayer(), "&2Arena command sign created");
            sul.addSign(acs);
            signLocs.put(getKey(acs.getLocation()), acs);
        } 
        catch (Exception e){
            MessageUtil.sendMessage(event.getPlayer(), "&cError creating Arena Command Sign");
            Log.printStackTrace(e);
            cancelSignPlace(event,block);
        }
    }

    private String getKey(Location location) {
        return location.getWorld().getName()+":"+location.getBlockX()+":"+
                location.getBlockY()+":"+location.getBlockZ();
    }

    private void makeArenaStatusSign(SignChangeEvent event, ArenaStatusSign acs, String[] lines) {
        if (acs == null)
            return;
        final Block block = event.getBlock();
        for (int i=3;i<lines.length;i++){
            if (!lines[i].isEmpty()) /// other text, not our sign
                return;
        }

        try{
            String match = acs.getType().toLowerCase();
            match = Character.toUpperCase(match.charAt(0)) + match.substring(1);
            
            event.setLine(0, MessageUtil.colorChat( ChatColor.GOLD + Defaults.SIGN_PREFIX +
                    MessageUtil.getFirstColor( acs.getMatchParams().getPrefix() ) + match ));
            
            event.setLine(1, MessageUtil.colorChat( ""));
            acs.setLocation(event.getBlock().getLocation());

            MessageUtil.sendMessage(event.getPlayer(), "&2Arena status sign created");
        } 
        catch (Exception e){
            MessageUtil.sendMessage(event.getPlayer(), "&cError creating Arena Status Sign");
            Log.printStackTrace(e);
            cancelSignPlace(event,block);
        }
    }

    public static void cancelSignPlace(SignChangeEvent event, Block block){
        event.setCancelled(true);
        block.setType(Material.AIR);
        block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.SIGN, 1));
    }
}
