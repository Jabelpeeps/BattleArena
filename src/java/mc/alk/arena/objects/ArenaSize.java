package mc.alk.arena.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.util.MinMax;
import mc.alk.arena.util.Util;

public class ArenaSize { 
    public static final int MAX = Integer.MAX_VALUE;
    
	@Getter @Setter int minTeamSize = 1;
	@Getter @Setter int maxTeamSize = MAX;
	@Getter @Setter int minTeams = 2;
	@Getter @Setter int maxTeams = MAX;

	public ArenaSize(){}

	public ArenaSize( ArenaSize size ) {
		minTeamSize = size.getMinTeamSize();
		maxTeamSize = size.getMaxTeamSize();
		minTeams = size.getMinTeams();
		maxTeams = size.getMaxTeams();
	}

	public int getMinPlayers() {
		return minTeams * minTeamSize;
	}
	public int getMaxPlayers() {
		return (maxTeams == MAX || maxTeamSize == MAX) ? MAX 
		                                               : maxTeams * maxTeamSize;
	}
	public boolean matchesTeamSize(int i) {
		return minTeamSize <= i && i <= maxTeamSize;
	}
	public boolean matches(ArenaSize size) {
		return matchesTeamSize( this, size ) && matchesNTeams( this, size );
	}
	public static boolean matchesTeamSize(ArenaSize size1, ArenaSize size2) {
        return size1 == null && size2 == null ||
                !(size1 == null || size2 == null) && size1.matchesTeamSize(size2);
    }
	public static boolean matchesNTeams(ArenaSize size1, ArenaSize size2) {
        return size1 == null && size2 == null ||
                !(size1 == null || size2 == null) && size1.matchesNTeams(size2);
    }
	public static boolean lower(MinMax child, MinMax parent) {
        return child == null || parent == null || child.max < parent.max;
    }
	public void setTeamSize( int size ) {
		minTeamSize = maxTeamSize = size;
	}
	public void setTeamSizes(MinMax mm) {
		minTeamSize = mm.min;
		maxTeamSize = mm.max;
	}
	public void setNTeams(MinMax mm) {
		minTeams = mm.min;
		maxTeams = mm.max;
	}
	public boolean matchesNTeams( ArenaSize csize ) {
		return Math.max( csize.getMinTeams(), minTeams ) <= Math.min( csize.getMaxTeams(), maxTeams );
	}
	public boolean matchesNTeams( int nteams ) {
		return minTeams <= nteams && nteams <= maxTeams;
	}
	public boolean matchesTeamSize( ArenaSize csize ) {
		return Math.max( csize.getMinTeamSize(), minTeamSize ) <= Math.min( csize.getMaxTeamSize(), maxTeamSize );
	}
	public static boolean intersect( ArenaSize size1, ArenaSize size2 ) {
		return size1.intersect( size2 );
	}
	public boolean intersect( ArenaSize csize ) {
		minTeams = Math.max( csize.getMinTeams(), minTeams );
		maxTeams = Math.min( csize.getMaxTeams(), maxTeams );
		minTeamSize = Math.max( csize.getMinTeamSize(), minTeamSize );
		maxTeamSize = Math.min( csize.getMaxTeamSize(), maxTeamSize );
		return ( minTeams <= maxTeams && minTeamSize <= maxTeamSize );
	}
	public boolean intersectMax( ArenaSize csize ) {
		maxTeams = Math.min( csize.getMaxTeams(), maxTeams );
		maxTeamSize = Math.min( csize.getMaxTeamSize(), maxTeamSize );
		return ( minTeams <= maxTeams && minTeamSize <= maxTeamSize );
	}
	public boolean intersectTeamSize( int size ) {
		if ( minTeamSize > size || maxTeamSize < size ) return false;
		
		minTeamSize = size;
		maxTeamSize = size;
		return true;
	}

    @Override
	public String toString(){
		return "[" + Util.rangeString( minTeamSize, maxTeamSize ) + " <-> " + Util.rangeString( minTeams, maxTeams ) + "]";
	}
	public boolean valid() {
		return minTeamSize <= maxTeamSize && minTeams <= maxTeams;
	}
	public Collection<String> getInvalidReasons() {
		List<String> reasons = new ArrayList<>();
		if ( minTeamSize <= 0 ) reasons.add( "Min Team Size is <= 0" );
		if ( maxTeamSize <= 0 ) reasons.add( "Max Team Size is <= 0" );
		if ( minTeamSize > maxTeamSize ) 
		    reasons.add( "Min Team Size is greater than Max Team Size " + minTeamSize + ":" + maxTeamSize );
		if ( minTeams > maxTeams ) 
		    reasons.add( "Min Teams is greater than Max Teams" + minTeams + ":" + maxTeams );
		return reasons;
	}
}
