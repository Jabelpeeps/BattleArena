package mc.alk.arena.serializers;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import mc.alk.util.Log;

public class BaseConfig { 
	FileConfiguration config;
	File file = null;

    public BaseConfig(){}

    public BaseConfig( File _file ){
        setConfig( _file );
    }
    
	public int getInt(String node,int defaultValue) { return config.getInt(node, defaultValue); }
	public boolean getBoolean(String node, boolean defaultValue) { return config.getBoolean(node, false); }
	public double getDouble(String node, double defaultValue) { return config.getDouble(node, defaultValue); }
	public String getString(String node,String defaultValue) { return config.getString(node, defaultValue); }
	public ConfigurationSection getConfigurationSection(String node) { return config.getConfigurationSection(node); }
	public FileConfiguration getConfig() { return config; }
	public File getFile() { return file; }
	public boolean setConfig( String _file ) { return setConfig( new File(_file) ); }

	public boolean setConfig(File _file){
		file = _file;
		if (!_file.exists()){
			try {
				if (!_file.createNewFile()){
                    Log.err("Couldn't create the config file=" + _file);
                    return false;
                }
			} catch (IOException e) {
				Log.err("Couldn't create the config file=" + _file);
				Log.printStackTrace(e);
				return false;
			}
		}

		config = new YamlConfiguration();
		try {
			config.load(_file);
		} catch (Exception e) {
			Log.err("Couldn't load the config file=" + _file);
			Log.printStackTrace(e);
			return false;
		}
		return true;
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
