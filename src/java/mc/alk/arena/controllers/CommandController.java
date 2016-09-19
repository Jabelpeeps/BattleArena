package mc.alk.arena.controllers;

import java.lang.reflect.Field;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.google.common.base.Predicate;

import mc.alk.arena.Defaults;
import mc.alk.arena.Permissions;

public class CommandController {

    private static CommandMap commandMap = getCommandMap(); 
    
    private static CommandMap getCommandMap() {
        
        Class<?> serverClass = Bukkit.getServer().getClass();
        try {
            if ( serverClass.isAssignableFrom( Bukkit.getServer().getClass() ) ) {
                Field f = serverClass.getDeclaredField("commandMap");
                f.setAccessible(true);
                return (CommandMap) f.get( Bukkit.getServer() );
            }
        } 
        catch ( SecurityException e ) {
            System.out.println("You will need to disable the security manager to use dynamic commands");
        } 
        catch ( IllegalArgumentException | IllegalAccessException | NoSuchFieldException e ) {
            e.printStackTrace();
        } 
        return null;
    }
    
    public static void registerCommand( Command command ) { 
        if ( commandMap == null )
            getCommandMap();
        
        if ( commandMap != null ) {
            commandMap.register( "/", command);
        }
    }

    public static boolean shouldCancel( PlayerCommandPreprocessEvent event, final boolean allDisabled,
                                        final Set<String> disabledCommands, final Set<String> enabledCommands) {
    
        if ( Defaults.DEBUG_COMMANDS ) {
            event.getPlayer().sendMessage( "event Message=" + event.getMessage() + "   isCancelled=" + event.isCancelled());
        }
        
        // Make sure Admins can run commands:
        if ( Defaults.ALLOW_ADMIN_CMDS_IN_Q_OR_MATCH && Permissions.isAdmin(event.getPlayer() ) ) {
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
