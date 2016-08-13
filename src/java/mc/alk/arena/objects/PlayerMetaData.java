package mc.alk.arena.objects;


import mc.alk.arena.objects.options.JoinOptions;

public class PlayerMetaData {
    
	private boolean joining;
    private JoinOptions joinOptions;
    private int livesLeft = -1;
    PlayerSave joinRequirements;

    public boolean isJoining() { return joining; }
	public void setJoining(boolean _joining) { joining = _joining; }
    public JoinOptions getJoinOptions() { return joinOptions; }
    public void setJoinOptions(JoinOptions jo) { joinOptions = jo; }
    public int getLivesLeft() { return livesLeft; }
    public void setLivesLeft(int _livesLeft) { livesLeft = _livesLeft; }
    public PlayerSave getJoinRequirements() { return joinRequirements; }
    public void setJoinRequirements(PlayerSave _joinRequirements) { joinRequirements = _joinRequirements; }
}
