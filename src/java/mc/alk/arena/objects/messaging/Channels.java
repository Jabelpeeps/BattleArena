package mc.alk.arena.objects.messaging;

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

    public static final Channel NullChannel = (msg) -> { /* yeah do nothing */ };

    public static final Channel ServerChannel = 
            (msg) -> {
                    if ( msg == null || msg.trim().isEmpty() ) return;
                    
                    try {
                        MessageUtil.broadcastMessage( MessageUtil.colorChat( msg ) );
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
