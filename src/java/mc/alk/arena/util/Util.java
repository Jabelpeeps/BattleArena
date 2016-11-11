package mc.alk.arena.util;

import java.util.UUID;

import org.bukkit.Location;

import mc.alk.arena.objects.ArenaSize;

public abstract class Util {

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
    public static int toInt( String size ) {
        return toInt( size, 0 );
    }
    public static int toInt( String size, int defValue ) {
        if ( size == null || size.isEmpty() ) return defValue;
        if ( "infinite".equalsIgnoreCase( size ) ) return Integer.MAX_VALUE;
        try {
            return Integer.parseInt( size );
        }
        catch ( NumberFormatException e ) {
            return defValue;
        }
    }

    public static String rangeString( int min, int max ) {
    	if ( max == ArenaSize.MAX ) return min + "+"; /// Example: 2+
    	if ( min == max ) return min + ""; /// Example: 2
    	return min + "-" + max; //Example 2-4
    }

    public static String intToString( int i ) {
        if ( i == Integer.MAX_VALUE ) 
            return "infinite";
        return String.valueOf( i );
    }
}
