package mc.alk.util;

import org.bukkit.entity.EntityType;

public class EntityUtil {

	static final String TAMED = "tamed_";

	public static EntityType parseEntityType(String str) {

		if ( str.startsWith(TAMED) ) 
			str = str.substring(TAMED.length(), str.length());
		
        return EntityType.fromName(str);
	}
}
