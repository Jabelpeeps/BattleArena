package mc.alk.arena.util;

import mc.alk.arena.objects.ArenaSize;

public class MinMax {
    public int min = -1;
    public int max = -1;

    public MinMax() {}
    public MinMax( int size ) { min = size; max = size; }
    public MinMax( int _min, int _max ) { min = _min; max = _max; }
    public MinMax( MinMax mm ) { min = mm.min; max = mm.max; }

    public boolean contains( int i ) {
        return min <= i && max >= i;
    }
    public boolean intersect( MinMax mm ) {
        return Math.max( mm.min, min ) <= Math.min( mm.max, max );
    }
    public boolean valid() {
        return min <= max;
    }
    public static MinMax valueOf( String s ) throws NumberFormatException{
        if ( s == null ) throw new NumberFormatException("Number can not be null");
        if ( s.indexOf( '+' ) != -1 ) {
            return new MinMax( Integer.parseInt( s.substring( 0, s.indexOf( '+' ) ) ), ArenaSize.MAX );
        }
        if ( s.contains( "-" ) ) {
            String[] vals = s.split( "-" );
            return new MinMax( Integer.parseInt( vals[0] ), Integer.parseInt( vals[1] ) );
        }

        int i;
        if ( s.contains( "v" ) )
            i = Integer.parseInt( s.split("v")[0] );
        else
            i = Integer.parseInt( s );
        
        return new MinMax( i, i );
    }
    @Override
    public String toString() { return ArenaSize.rangeString( min, max ); } 
}
