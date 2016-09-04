package mc.alk.arena.util;

import java.util.UUID;

import org.bukkit.Location;

import mc.alk.arena.Defaults;
import mc.alk.arena.objects.spawns.SpawnLocation;

public class Util {

	static public void printStackTrace(){
		/// I've left in this accidentally too many times, make sure DEBUGGING is now on before printing
		if (Defaults.DEBUG)
			for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
				System.out.println(ste);}
	}

    static public String getLocString(Location l){
        return l.getWorld().getName() +"," + (int)l.getX() + "," + (int)l.getY() + "," + (int)l.getZ();
    }

    static public String getLocString(SpawnLocation l){
        return getLocString( l.getLocation() );
    }

    /**
     * Mimic the Object toString Method
     * @param o Object
     * @return Object toString
     */
    public static String toString(Object o) {
        return o == null ? null : o.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }

    public static UUID fromString(String name){
        try {
            return UUID.fromString(name);
        } 
        catch ( IllegalArgumentException e ) {
            return new UUID( 0, name.hashCode() );
        }
    }

}
