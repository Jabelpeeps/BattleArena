package mc.alk.arena.objects.messaging;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import lombok.AllArgsConstructor;
import mc.alk.arena.Defaults;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.MessageUtil;

/**
 * @author alkarin
 */
public class Channels {

    public static final Channel NullChannel = (msg) -> { /* do nothing */ };

    public static final Channel ServerChannel = 
            (msg) -> {
                    if ( msg == null || msg.trim().isEmpty() ) return;
                    
                    try {
                        Bukkit.broadcastMessage( MessageUtil.colorChat( msg ) );
                    } catch (Throwable e) {
                        if ( !Defaults.DEBUG_STRESS ) 
                            Log.printStackTrace(e);
                    }
            };
            
    @AllArgsConstructor
    public static class WorldChannel implements Channel {
        final World world;

        @Override
        public void broadcast(String msg) {
            if ( msg == null || msg.trim().isEmpty() ) return;
            
            msg = MessageUtil.colorChat(msg);
            
            for ( Player p : world.getPlayers() ) {
                p.sendMessage( msg );
            }
        }
    }
}
