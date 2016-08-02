package mc.alk.arena.util.plugins;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.trc202.CombatTag.CombatTag;
import com.trc202.CombatTagApi.CombatTagApi;

import mc.alk.arena.plugins.combattag.CombatTagInterface;
import mc.alk.arena.plugins.combattag.TagsOff;
import mc.alk.arena.plugins.combattag.TagsOn;

public class CombatTagUtil {
    
    private static final CombatTagInterface tag;
    
    static {       
        Plugin plugin = Bukkit.getPluginManager().getPlugin("CombatTag");
        
        if (plugin != null) {
            CombatTagApi api = new CombatTagApi( (CombatTag) plugin );
            tag = new TagsOn(api);
        }
        else tag = new TagsOff();
    }
    
    public static boolean isTagged(Player player) {
        return tag.isInCombat(player);
    }
    
    public static void untag(Player player) {
        tag.untag(player);
    }
}
