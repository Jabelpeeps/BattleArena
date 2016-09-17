package mc.alk.arena.tracker;


import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import lombok.Getter;
import mc.alk.arena.Defaults;
import mc.alk.arena.listeners.BTEntityListener;
/**
 *
 * @author alkarin
 *
 */
public class TrackerConfigController {
	@Getter static YamlConfiguration config = new YamlConfiguration();
	@Getter static File file = null;

	public static boolean getBoolean(String node, boolean b) { return config.getBoolean( node, b ); }	
	public static int getInt(String node, int i) { return config.getInt( node, i ); }
	public static double getDouble(String node, double d) { return config.getDouble( node, d ); }

	public static void setConfig(File f){
		file = f;
		loadAll();
	}

	public static void loadAll(){
		try { 
		    config.load(file); 
		} 
		catch ( IOException | InvalidConfigurationException e ) { e.printStackTrace(); }
		
		Defaults.RAMPAGE_TIME = config.getInt( "rampageTime", 7 );
		Defaults.STREAK_EVERY = config.getInt( "streakMessagesEvery", 15 );
		Defaults.PVE_MESSAGES = config.getBoolean( "sendPVEDeathMessages", Defaults.PVE_MESSAGES );
		Defaults.PVP_MESSAGES = config.getBoolean( "sendPVPDeathMessages", Defaults.PVP_MESSAGES );
		Defaults.BUKKIT_PVE_MESSAGES = config.getBoolean( "showBukkitPVEMessages", Defaults.BUKKIT_PVE_MESSAGES );
		Defaults.BUKKIT_PVP_MESSAGES = config.getBoolean( "showBukkitPVPMessages", Defaults.BUKKIT_PVP_MESSAGES );
		Defaults.INVOLVED_PVE_MESSAGES = config.getBoolean( "sendInvolvedPvEMessages", Defaults.INVOLVED_PVE_MESSAGES );
		Defaults.INVOLVED_PVP_MESSAGES= config.getBoolean( "sendInvolvedPvPMessages", Defaults.INVOLVED_PVP_MESSAGES );
		Defaults.RADIUS 	= config.getInt( "msgRadius", 0 );
		Defaults.MSG_TOP_HEADER = config.getString( "topHeaderMsg", Defaults.MSG_TOP_HEADER );
		Defaults.MSG_TOP_BODY = config.getString( "topBodyMsg", Defaults.MSG_TOP_BODY );
		Defaults.USE_SIGNS = config.getBoolean( "useSigns", Defaults.USE_SIGNS );

        BTEntityListener.setIgnoreEntities( config.getStringList( "ignoreEntities" ) );
		BTEntityListener.setIgnoreWorlds( config.getStringList( "ignoreWorlds" ) );
	}
}
