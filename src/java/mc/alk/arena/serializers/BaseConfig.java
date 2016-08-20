package mc.alk.arena.serializers;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import lombok.Getter;
import mc.alk.arena.util.Log;

public class BaseConfig { 
	@Getter protected FileConfiguration config;
	@Getter File file = null;
 
	public int getInt(String node,int defaultValue) { return config.getInt(node, defaultValue); }
	public boolean getBoolean(String node, boolean defaultValue) { return config.getBoolean(node, false); }
	public double getDouble(String node, double defaultValue) { return config.getDouble(node, defaultValue); }
	public String getString(String node,String defaultValue) { return config.getString(node, defaultValue); }
	public ConfigurationSection getConfigurationSection(String node) { return config.getConfigurationSection(node); }
	public BaseConfig setConfig( String _file ) { return setConfig( new File(_file) ); }

	public BaseConfig setConfig(File _file){
		file = _file;
		if (!_file.exists()){
			try {
				if (!_file.createNewFile()){
                    Log.err("Couldn't create the config file=" + _file);
                    return null;
                }
			} catch (IOException e) {
				Log.err("Couldn't create the config file=" + _file);
				Log.printStackTrace(e);
				return null;
			}
		}

		config = new YamlConfiguration();
		try {
			config.load(_file);
		} catch (Exception e) {
			Log.err("Couldn't load the config file=" + _file);
			Log.printStackTrace(e);
			return null;
		}
		return this;
	}

	public void reloadFile(){
		try {
			config.load(file);
		} catch (Exception e) {
			Log.printStackTrace(e);
		}
	}
	public void save() {
		if (config == null)
			return;
		try {
			config.save(file);
		} catch (IOException e) {
			Log.printStackTrace(e);
		}
	}

	public List<String> getStringList(String node) {
		return config.getStringList(node);
	}

	public void load(File _file) {
		file = _file;
		reloadFile();
	}
}
