package mc.alk.tracker.objects;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.SerializerUtil;

@RequiredArgsConstructor
public class StatSign implements ConfigurationSerializable{
	@Getter final String dBName;
	@Getter final Location location;
	@Getter final SignType signType;
	@Getter @Setter StatType statType = StatType.RATING;

	public enum SignType {
	    TOP, PERSONAL;

	    public static SignType fromName(String name) {
	        if (name ==null) return null;
	        
	        name = name.toUpperCase();
	        SignType gt = null;
	        try{
	            gt = SignType.valueOf(name);
	        } 
	        catch (Exception e){}
	        
	        if (gt == null){
	            StatType st = StatType.fromName(name);
	            if (st != null)
	                gt = SignType.PERSONAL;
	        }
	        return gt;
	    }
	}

	@Override
	public Map<String, Object> serialize() {
		HashMap<String,Object> map = new HashMap<>();
		map.put("location", SerializerUtil.getLocString(location));
		map.put("signType", signType.toString());
		map.put("dbName", dBName);
		if (statType != null)
			map.put("statType", statType.toString());
		return map;
	}

	public static StatSign valueOf(Map<String, Object> map) {
		return deserialize(map);
	}

	public static StatSign deserialize(Map<String, Object> map) {
		Object signTypeStr = map.get("signType");
		Object locStr = map.get("location");
		Object statStr = map.get("statType");
		Object dbStr = map.get("dbName");
		if (signTypeStr == null || locStr == null || dbStr == null) return null;
		
		SignType type = SignType.valueOf(signTypeStr.toString());
		Location l = null;
		try{
			l = SerializerUtil.getLocation(locStr.toString());
		} 
		catch (IllegalArgumentException e){
			Log.warn("BattleTracker error retrieving sign at " + locStr);
		}
		if (type == null || l == null)
			return null;
		StatSign ss = new StatSign(dbStr.toString(),l,type);
		if (statStr != null){
			StatType statType = StatType.fromName(statStr.toString());
			ss.setStatType(statType);
		}
		return ss;
	}

	public String getLocationString() {
		return SerializerUtil.getLocString(location);
	}
	public static String getLocationString(Location location){
		return SerializerUtil.getLocString(location);
	}
	@Override
	public String toString(){
		return "["+SerializerUtil.getLocString(location)+ " : "+signType+"]";
	}
}
