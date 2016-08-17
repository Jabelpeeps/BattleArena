package mc.alk.arena.controllers;

import java.lang.reflect.Field;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;

import mc.alk.arena.util.Log;

public class CommandController {

    private static CommandMap commandMap = getCommandMap(); 
    
    private static CommandMap getCommandMap() {

        Class<?> serverClass = Bukkit.getServer().getClass();
        try {
            if (serverClass.isAssignableFrom(Bukkit.getServer().getClass())) {
                final Field f = serverClass.getDeclaredField("commandMap");
                f.setAccessible(true);
                return (CommandMap) f.get(Bukkit.getServer());
            }
        } catch (final SecurityException e) {
            System.out.println("You will need to disable the security manager to use dynamic commands");
        } catch (final Exception e) {
            Log.printStackTrace(e);
        }
        return null;
    }
    public static void registerCommand(final Command command) {  
        if (commandMap != null) {
            commandMap.register("/", command);
        }
    }
}
