package mc.alk.arena.util;

import java.util.Set;

import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.google.common.base.Predicate;

import mc.alk.arena.Defaults;

public class CommandUtil {

    public static boolean shouldCancel( PlayerCommandPreprocessEvent event, final boolean allDisabled,
                                        final Set<String> disabledCommands, final Set<String> enabledCommands) {
       
        if ( Defaults.DEBUG_COMMANDS ) {
            event.getPlayer().sendMessage( "event Message=" + event.getMessage() + "   isCancelled=" + event.isCancelled());
        }
        
        // Make sure Admins can run commands:
        if (Defaults.ALLOW_ADMIN_CMDS_IN_Q_OR_MATCH && PermissionsUtil.isAdmin(event.getPlayer())) {
            return false;
        }
        
        String cmd = event.getMessage().toLowerCase();
        
        Predicate<String> isCmdDisabled = 
                ( command ) -> { return allDisabled 
                                        || disabledCommands.parallelStream()
                                                           .anyMatch( c -> command.startsWith( c ) );
                };
        
        Predicate<String> isCmdEnabled = 
                ( command ) -> { if (   command.startsWith("/bad")
                                        || command.startsWith("/battleArenaDebug".toLowerCase())) {
                                    return true;
                                }
                                for ( String c : enabledCommands ) {
                                    if ( command.startsWith( c.toLowerCase() ) ) {
                                        return true;
                                    }
                                }
                                return false;
                };
        
        // enabledCommands should override disabledCommands:
        if ( isCmdDisabled.apply( cmd ) && !isCmdEnabled.apply( cmd ) ) {
            return true;
        }
        return false; // by default, no command should be cancelled.
    }

}
