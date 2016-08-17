package mc.alk.arena.util;

import com.google.common.base.Predicate;

import java.util.Set;

import mc.alk.arena.Defaults;

import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandUtil {

    public static boolean shouldCancel(PlayerCommandPreprocessEvent event, final boolean allDisabled,
            final Set<String> disabledCommands, final Set<String> enabledCommands) {
        if (Defaults.DEBUG_COMMANDS) {
            event.getPlayer().sendMessage("event Message=" + event.getMessage() + "   isCancelled=" + event.isCancelled());
        }
        
        // Make sure Admins can run commands:
        if (Defaults.ALLOW_ADMIN_CMDS_IN_Q_OR_MATCH && PermissionsUtil.isAdmin(event.getPlayer())) {
            return false;
        }
        
        String cmd = event.getMessage().toLowerCase();
//        final int index = cmd.indexOf(' ');
//        if (index != -1) {
//            cmd = cmd.substring(0, index);
//        }
//        cmd = cmd.toLowerCase();
        
        Predicate<String> isCmdDisabled = new Predicate<String>() {

            @Override
            public boolean apply(String command) {
                return (allDisabled || disabledCommandsContains(command));
            }
            
            public boolean disabledCommandsContains(String command) {
                for (String c : disabledCommands) {
                    if (command.startsWith(c)) {
                        return true;
                    }
                }
                return false;
            }
        };
        
        Predicate<String> isCmdEnabled = new Predicate<String>() {

            @Override
            public boolean apply(String command) {
                if (command.startsWith("/bad")
                        || command.startsWith("/battleArenaDebug".toLowerCase())) {
                    return true;
                }
                for (String c : enabledCommands) {
                    if (command.startsWith(c.toLowerCase())) {
                        return true;
                    }
                }
                return false;
            }
        };
        
        // enabledCommands should override disabledCommands:
        if (isCmdDisabled.apply(cmd) && !isCmdEnabled.apply(cmd)) {
            return true;
        }
        return false; // by default, no command should be cancelled.
    }

}
