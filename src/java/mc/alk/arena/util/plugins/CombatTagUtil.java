package mc.alk.arena.util.plugins;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.minelink.ctplus.CombatTagPlus;
import net.minelink.ctplus.TagManager;

public class CombatTagUtil {
    
    private static final TagManager tag;
    
    static {       
        Plugin plugin = Bukkit.getPluginManager().getPlugin("CombatTag");
        
        if (plugin != null) {
            tag = ((CombatTagPlus) plugin).getTagManager();
        }
        else tag = null;
    }
    
    public static boolean isTagged(Player player) {
        if ( tag != null )
            return tag.isTagged( player.getUniqueId() );
        
        return false;
    }
    
    public static void untag(Player player) {
        if ( tag != null )
            tag.untag( player.getUniqueId() );
    }
}
