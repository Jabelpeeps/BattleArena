package mc.alk.arena.controllers;

import java.lang.reflect.Field;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;

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
        if ( commandMap != null ) {
            commandMap.register("/", command);
        }
    }
}
