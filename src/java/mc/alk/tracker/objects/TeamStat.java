package mc.alk.tracker.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;


public class TeamStat extends Stat implements Comparable<TeamStat>{

	public TeamStat(){ }

	public TeamStat( String _name, boolean id ){
		this( _name, id, 0 );
	}

	public TeamStat( String _name, boolean id, int teamSize ) {
		if (id) {
			strID = _name;
			count = teamSize;
		} 
		else {
			name = _name;
			strID = _name;
			
			int c = charCount(_name,',');
			count = c == 0 ? 0: c+1; 
		}
	}
    
    public TeamStat( Set<String> p ) {
        members = new ArrayList<>(p);
        createName();
        strID = TeamStat.getKey(this.members);
        count = p.size();
    }
    
    public int charCount(final String s, final Character testc) {
        int charCount =0;
        for ( int i = 0; i < s.length(); i++ )
            if ( testc.equals( s.charAt(i) ) )
                charCount++;
        return charCount;
    }

	public void setMembers(Collection<String> players){
		if (players == null)
			return;
		members = new LinkedList<>(players);
		strID = TeamStat.getKey(members);
		createName();
	}
}
