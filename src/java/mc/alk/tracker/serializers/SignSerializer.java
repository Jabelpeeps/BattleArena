package mc.alk.tracker.serializers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;

import lombok.AllArgsConstructor;
import mc.alk.arena.serializers.BaseConfig;
import mc.alk.tracker.controllers.SignController;
import mc.alk.tracker.objects.StatSign;
import mc.alk.util.SerializerUtil;

@AllArgsConstructor
public class SignSerializer extends BaseConfig {
	SignController sc;

	public void saveAll() {
		Map<String,StatSign> map = sc.getTopSigns();
		if (map != null){
			List<StatSign> l = new ArrayList<>(map.values());
			config.set("topSigns", l);
		}
		map = sc.getPersonalSigns();
		if (map != null){
			List<StatSign> l = new ArrayList<>(map.values());
			config.set("personalSigns", l);
		}

		save();
	}

	public void loadAll(){
		String[] types = new String[]{"topSigns","personalSigns"};
		sc.clearSigns();
		for (String type: types){
			List<?> signs = config.getList(type);
			if (signs == null)
				continue;
			for (Object o : signs){
				if (o == null || !(o instanceof StatSign))
					continue;
				if (!stillSign((StatSign)o))
					continue;
				sc.addSign((StatSign) o);
			}
		}
	}

	public static boolean stillSign(StatSign o) {
		String l = o.getLocationString();
		if (l == null)
			return false;
		try{
			Location loc = SerializerUtil.getLocation(l);
			if (loc == null)
				return false;
			Material mat = loc.getWorld().getBlockAt(loc).getType();
			if ( mat != Material.SIGN && mat != Material.SIGN_POST && mat != Material.WALL_SIGN)
				return false;
		} catch( Exception e){
			return false;
		}
		return true;
	}
}
