package mc.alk.arena.objects.scoreboard;

import org.bukkit.scoreboard.DisplaySlot;

public enum SAPIDisplaySlot {
	SIDEBAR, PLAYER_LIST, BELOW_NAME, NONE;

	public static SAPIDisplaySlot fromValue(String s){
		return SAPIDisplaySlot.valueOf( s.toUpperCase() );
	}
	public SAPIDisplaySlot swap() {
		switch( this ) {
    		case PLAYER_LIST:
    			return SAPIDisplaySlot.SIDEBAR;
    		case SIDEBAR:
    			return SAPIDisplaySlot.PLAYER_LIST;
    			
    		case BELOW_NAME: case NONE: default:
    			return null;
		}
	}
	public DisplaySlot toBukkitDisplaySlot() {
	    switch ( this ){
            case BELOW_NAME:
                return DisplaySlot.BELOW_NAME;
            case PLAYER_LIST:
                return DisplaySlot.PLAYER_LIST;
            case SIDEBAR:
                return DisplaySlot.SIDEBAR;  
                
            case NONE: default:
                return null;
        }
    }
}
