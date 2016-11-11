package mc.alk.arena.util;

import java.util.UUID;

import org.bukkit.Location;

public class Util {

	static public String getLocString( Location l ) {
        return l.getWorld().getName() + "," + (int) l.getX() + "," + (int) l.getY() + "," + (int) l.getZ();
    }

    public static UUID fromString(String name){
        try {
            return UUID.fromString(name);
        } 
        catch ( IllegalArgumentException e ) {
            return new UUID( 0, name.hashCode() );
        }
    }
    public static boolean isInt(String i) { 
        try { 
            Integer.parseInt(i);
            return true;
        } 
        catch (Exception e) {
            return false;
        }
    }

}
