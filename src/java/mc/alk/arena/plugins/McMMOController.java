package mc.alk.arena.plugins;

import java.util.Collection;
import java.util.HashSet;

import org.bukkit.event.Cancellable;

import com.gmail.nossr50.datatypes.skills.SkillType;
import com.gmail.nossr50.events.skills.McMMOPlayerSkillEvent;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.objects.arenas.ArenaListener;
import mc.alk.arena.objects.events.ArenaEventHandler;
import mc.alk.arena.util.Log;


public class McMMOController {
	@Getter @Setter static boolean enabled = false;

    static HashSet<SkillType> disabledSkills;
    
    public static void setDisabledSkills(Collection<String> disabledCommands) {
        if (disabledSkills == null)
            disabledSkills = new HashSet<>();
        
        for (String s: disabledCommands) {
            SkillType st = SkillType.getSkill(s);
            if (st == null){
                Log.err("mcMMO skill " + s + " was not found");
                continue;
            }
            disabledSkills.add(st);
        }
    }
    public static boolean hasDisabledSkills() {
        return disabledSkills != null && !disabledSkills.isEmpty();
    }    
    public static ArenaListener createNewListener() {
        return new McMMOListener();
    }
  
    public static class McMMOListener implements ArenaListener {
        @ArenaEventHandler
        public void skillDisabled(McMMOPlayerSkillEvent event){
            if (    hasDisabledSkills() 
                    && event.getPlayer() != null 
                    && (event instanceof Cancellable) 
                    && disabledSkills.contains(event.getSkill())) {
            ((Cancellable) event).setCancelled(true);
            }
        }
    }
}
