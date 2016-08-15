package mc.alk.arena.plugins;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.objects.arenas.ArenaListener;
import mc.alk.util.Log;


public class McMMOController {
	@Getter @Setter static boolean enabled = false;

    public static void setDisabledSkills(List<String> disabled) {
        try { 
            McMMOListener.setDisabledSkills(disabled);
        }catch( Exception e ) {
            Log.printStackTrace(e);    
        }
    }

    public static boolean hasDisabledSkills() {
        return McMMOListener.hasDisabledSkills();
    }

    public static ArenaListener createNewListener() {
        return new McMMOListener();
    }
}
