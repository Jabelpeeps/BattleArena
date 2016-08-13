package mc.alk.arena.objects.modules;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import lombok.Getter;
import mc.alk.arena.BattleArena;
import mc.alk.arena.objects.arenas.ArenaListener;
import mc.alk.util.FileUtil;
import mc.alk.util.Log;

public abstract class ArenaModule implements Listener, ArenaListener {
	@Getter private boolean enabled;
	protected FileConfiguration config;

	/**
	 * Called when the module is first created
	 */
	public void onEnable() { enabled = true; }

	/**
	 * Called when the module is being disabled
	 */
	public void onDisable() { enabled = false; }

	/**
	 * Return the Name of this module
	 * @return module name
	 */
	public abstract String getName();

	/**
	 * Return the version of this Module
	 * @return version
	 */
	public abstract String getVersion();

	/**
	 * Set the module to be enabled or not
	 * @param enable Whether to enable or disable
	 */
	public void setEnabled( boolean enable ) {
		if ( enabled != enable ) {
			if ( enable ){
				onEnable();
				Bukkit.getPluginManager().registerEvents( this, BattleArena.getSelf() );
			} 
			else {
				onDisable();
				HandlerList.unregisterAll( this );
			}
		}
		enabled = enable;
	}

	public void reloadConfig(){
		try {
			config = YamlConfiguration.loadConfiguration( getConfigFile() );
		} catch (Exception e) {
			Log.printStackTrace(e);
		}
	}

	protected File getConfigFile() {
		return new File( BattleArena.getSelf().getModuleDirectory() + "/" + getName() );
	}

	/**
	 * create or save the default config.yml
	 */
	protected void saveDefaultConfig() {
		File f = getConfigFile();
		
		if ( config == null || !f.exists() ) {
		    
			if ( FileUtil.hasResource( this.getClass(), "/config.yml" ) ) {
				f = FileUtil.load( this.getClass(), f.getPath(), "/config.yml" );
			} 
			else {
				try {
					f.createNewFile();
				} catch (IOException e) {
					Log.printStackTrace(e);
				}
			}
			return;
		}
		try {
			config.save(f);
		} catch (IOException e) {
			Log.printStackTrace(e);
		}
	}

	public FileConfiguration getConfig(){
		if (config == null){
			saveDefaultConfig();
		}
		return config;
	}

	@Override
	public String toString(){
		return "[" + getName() + "_" + getVersion() + "]";
	}
}
