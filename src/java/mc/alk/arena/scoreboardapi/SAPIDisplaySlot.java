package mc.alk.arena.scoreboardapi;

public enum SAPIDisplaySlot {
	SIDEBAR, PLAYER_LIST, BELOW_NAME, NONE;

	public static SAPIDisplaySlot fromValue(String s){
		return SAPIDisplaySlot.valueOf(s.toUpperCase());
	}

	public SAPIDisplaySlot swap(){
		return SAPIDisplaySlot.swapValue(this);
	}

	public static SAPIDisplaySlot swapValue(SAPIDisplaySlot slot){
		switch(slot){
		case PLAYER_LIST:
			return SAPIDisplaySlot.SIDEBAR;
		case SIDEBAR:
			return SAPIDisplaySlot.PLAYER_LIST;
		case BELOW_NAME:
		case NONE:
		default:
			return null;
		}
	}
}
