package mc.alk.tracker.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

import com.dthielke.herochat.Herochat;

import mc.alk.tracker.controllers.MessageController;

/**
 * 
 * @author alkarin
 *
 */
public class BTPluginListener implements Listener {

	@EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
		loadPlugins();
    }
	public static void loadPlugins() {
	    
		Herochat hc = MessageController.getHerochat();
        if (hc == null) {
        	Plugin plugin = Bukkit.getPluginManager().getPlugin("HeroChat");
        	
            hc = ((Herochat) plugin);
            if (hc != null){
            	MessageController.setHerochat(hc);        	
            }
        }		
	}
}
