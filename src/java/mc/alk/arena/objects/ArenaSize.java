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

    public ArenaSize() {}
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
		return ( maxTeams == MAX || maxTeamSize == MAX ) ? MAX : maxTeams * maxTeamSize;
	}
	public boolean matchesTeamSize( int i ) {
		return minTeamSize <= i && i <= maxTeamSize;
	}
	public boolean matches( ArenaSize size ) {
		return matchesTeamSize( size ) && matchesNTeams( size );
	}
	public void setTeamSize( int size ) {
		minTeamSize = maxTeamSize = size;
	}
	public void setTeamSizes( MinMax mm ) {
		minTeamSize = mm.min;
		maxTeamSize = mm.max;
	}
	public void resetTeamSizes() {
	    minTeamSize = 1;
	    maxTeamSize = MAX;
	}
	public void setNTeams( MinMax mm ) {
		minTeams = mm.min;
		maxTeams = mm.max;
	}
	public void resetNTeams() {
	    minTeams = 2;
	    maxTeams = MAX;
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
	public String getTeamSizeString() {
	    return Util.rangeString( minTeamSize, maxTeamSize );
	}
	public String getNumTeamsString() {
	    return Util.rangeString( minTeams, maxTeams );
	}
    @Override
    public String toString(){
        return "[ArenaSize: {TeamSize:" + getTeamSizeString() + "},{NumTeams:" + getNumTeamsString() + "}]";
    }
}
