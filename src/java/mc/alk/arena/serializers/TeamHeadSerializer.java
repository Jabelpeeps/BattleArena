package mc.alk.arena.serializers;

import java.io.IOException;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.inventory.ItemStack;

import mc.alk.arena.BattleArena;
import mc.alk.arena.objects.exceptions.ConfigException;
import mc.alk.arena.util.InventoryUtil;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.MessageUtil;
import mc.alk.arena.util.TeamUtil;
import mc.alk.arena.util.TeamUtil.TeamHead;

public class TeamHeadSerializer extends BaseConfig{

	public void loadAll(){
		try {
		    config.load(file); 
		} 
		catch (IOException | InvalidConfigurationException e){ Log.printStackTrace(e); }
		
		loadTeams(config);
	}

	public static void loadTeams(ConfigurationSection cs) {
		if (cs == null) {
			Log.info(BattleArena.getNameAndVersion() +" has no teamColors");
			return;
		}
		List<String> keys = cs.getStringList("teams");
		boolean first = true;
		for (String teamStr : keys){
			try {
				addTeamHead(teamStr);
			} catch (Exception e) {
				Log.err("Error parsing teamHead " + teamStr);
				Log.printStackTrace(e);
				continue;
			}
			if (first) first = false;
		}
		if (first){
			Log.info(BattleArena.getNameAndVersion() + " no predefined teamColors found. inside of " + cs.getCurrentPath());
		}
	}

	private static String addTeamHead(String str) throws ConfigException {
	    
		String[] split = str.split(",");
		if (split.length != 5){
			throw new ConfigException("Team Colors must be in format 'Name,ItemStack,R,G,B'");
		}
		String name = MessageUtil.decolorChat(split[0]);
		if (name.isEmpty()){
			throw new ConfigException("Team Name must not be empty 'Name,ItemStack'");
		}
		ItemStack item = InventoryUtil.parseItem(split[1]);
		item.setAmount(1);
		int r = Integer.parseInt(split[2]);
		int g = Integer.parseInt(split[3]);
		int b = Integer.parseInt(split[4]);
		
		TeamUtil.addTeamHead(name, new TeamHead( item, split[0], Color.fromRGB( r, g, b) ) );
		return name;
	}
}
