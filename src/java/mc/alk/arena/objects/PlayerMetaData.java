package mc.alk.arena.objects;


import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.objects.options.JoinOptions;

@Getter @Setter
public class PlayerMetaData {
    
	private boolean joining;
    private JoinOptions joinOptions;
    private int livesLeft = -1;
    PlayerSave joinRequirements;

}
