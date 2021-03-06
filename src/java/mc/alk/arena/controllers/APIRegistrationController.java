package mc.alk.arena.controllers;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.executors.BAExecutor;
import mc.alk.arena.executors.CustomCommandExecutor;
import mc.alk.arena.executors.DuelExecutor;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.RegisteredCompetition;
import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.objects.arenas.ArenaType;
import mc.alk.arena.objects.exceptions.ConfigException;
import mc.alk.arena.objects.exceptions.InvalidOptionException;
import mc.alk.arena.serializers.ArenaSerializer;
import mc.alk.arena.serializers.ConfigSerializer;
import mc.alk.arena.serializers.MessageSerializer;
import mc.alk.arena.util.FileUtil;
import mc.alk.arena.util.Log;

public class APIRegistrationController {

    final static Set<String> delayedInits = Collections.synchronizedSet( new HashSet<String>() );

//    private static boolean createFile(File pluginFile, String name, String cmd, InputStream inputStream) {
//        String line;
//        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
//        BufferedWriter fw;
//        try {
//            fw = new BufferedWriter(new FileWriter(pluginFile));
//        } catch (IOException e) {
//            Log.printStackTrace(e);
//            return false;
//        }
//        try {
//            while ((line = br.readLine()) != null) {
//                line = line.replaceAll("<name>", name).replaceAll("<cmd>", cmd);
//                fw.write(line + "\n");
//            }
//        } catch (IOException e) {
//            Log.printStackTrace(e);
//            return false;
//        } finally {
//            try {
//                fw.close();
//            } catch (Exception e) {
//                Log.printStackTrace(e);
//            }
//            try {
//                br.close();
//            } catch (Exception e) {
//                Log.printStackTrace(e);
//            }
//        }
//        return true;
//    }

    public static boolean registerCompetition( JavaPlugin plugin, String name, String cmd, Class<? extends Arena> arena ) {
        return registerCompetition( plugin, name, cmd, arena, null );
    }

    public static boolean registerCompetition( JavaPlugin plugin, String name, String cmd, Class<? extends Arena> arena, 
                                               CustomCommandExecutor executor ) {
        File dir = plugin.getDataFolder();
        File configFile = new File( dir.getAbsolutePath() + "/" + name + "Config.yml" );
        File msgFile = new File( dir.getAbsolutePath() + "/" + name + "Messages.yml" );
        File defaultArenaFile = new File( dir.getAbsolutePath() + "/arenas.yml" );
        
        return registerCompetition( plugin, name, cmd, arena, executor, configFile, msgFile, defaultArenaFile );
    }

    static boolean registerCompetition( JavaPlugin plugin, String name, String cmd, Class<? extends Arena> arena, 
                                               CustomCommandExecutor executor, File configFile, File messageFile, 
                                               File defaultArenaFile ) {
        
        return registerCompetition( plugin, name, cmd, arena, executor, configFile, messageFile,
                new File( plugin.getDataFolder() + "default_files/competitions/" + name + "Config.yml" ), defaultArenaFile );
    }

    public static boolean registerCompetition( JavaPlugin plugin, String name, String cmd, Class<? extends Arena> arena, 
                                               CustomCommandExecutor executor, File configFile, File messageFile, 
                                               File defaultPluginConfigFile, File defaultArenaFile ) {
        try {
            return _registerCompetition( plugin, name, cmd, arena, executor, configFile, messageFile, 
                                                               defaultPluginConfigFile, defaultArenaFile );
        } catch ( Exception e ) {
            Log.err( "[BattleArena] could not register " + plugin.getName() + " " + name );
            Log.err( "[BattleArena] config " + configFile );
            Log.printStackTrace(e);
            return true;
        }
    }
    
    private static boolean _registerCompetition( JavaPlugin plugin, String name, String cmd, Class<? extends Arena> arenaClass, 
                                                 CustomCommandExecutor executor, File configFile, File messageFile, 
                                                 File defaultPluginConfigFile, File defaultArenaFile ) 
                                                            throws ConfigException, InvalidOptionException { 
        
        FileUtil.makeIfNotExists( plugin.getDataFolder() );

        /// Set a delayed init on this plugin to load custom types
        if ( !delayedInits.contains( plugin.getName() ) ) {
            delayedInits.add( plugin.getName() );
            Scheduler.scheduleSynchronousTask( new DelayedRegistrationHandler( plugin, defaultArenaFile ) );
        }

        if ( FileUtil.load( plugin.getClass(), configFile.getPath(), defaultPluginConfigFile.getPath() ) == null ) {
            Log.err( plugin.getName() + " " + configFile.getName() + " could not be loaded!" );
            Log.err( "defaultFile=" + defaultPluginConfigFile.getPath() );
            return false;
        }

        ConfigSerializer config = new ConfigSerializer( plugin, configFile, name );
        ConfigurationSection cs = config.getConfig();
        
        if ( cs.getConfigurationSection( name ) == null ) {
            Log.err( plugin.getName() + " " + configFile.getName() + " config file could not be loaded!" );
            return false;
        }
        /// What is our game type ? spleef, ctf, etc
        ArenaType gameType = ConfigSerializer.getArenaGameType( cs.getConfigurationSection( name ) );
        
        ArenaType at = ArenaType.register( name, arenaClass, plugin );

        MatchParams mp = config.loadMatchParams();

        MessageSerializer ms = null;

        if ( FileUtil.load( plugin.getClass(), messageFile.getPath(), messageFile.getPath() ) != null ) {
            ms = new MessageSerializer( name );
        } 
        else if ( gameType != null ) {
            RegisteredCompetition regComp = CompetitionController.getCompetition( plugin, gameType.getName() );
            if (regComp != null) {
                ms = MessageSerializer.getMessageSerializer( gameType.getName() );
            }
        }
        if ( ms != null ) {
            ms.setConfig( messageFile );
            ms.initMessageOptions();
//            MessageSerializer.addMessageSerializer( name, ms );
        }
        /// Everything nearly successful, register our competition
        RegisteredCompetition rc = new RegisteredCompetition( plugin, name );

        if ( executor == null && gameType != null ) {
            RegisteredCompetition comp = CompetitionController.getCompetition( plugin, gameType.getName() );
            if ( comp != null ) 
                executor = comp.getCustomExecutor();
        } 
        else 
            rc.setCustomExecutor( executor );

        createExecutor(plugin, cmd, executor, mp);
        rc.setConfigSerializer(config);
        CompetitionController.addRegisteredCompetition( rc );

        /// Load our arenas
        ArenaSerializer as = new ArenaSerializer( plugin, defaultArenaFile ); 
        as.loadArenas( at );
        rc.setArenaSerializer( as );

        return true;
    }

    private static void createExecutor(JavaPlugin plugin, String cmd, CustomCommandExecutor executor, MatchParams mp) {
        CustomCommandExecutor exe;

        if ( mp.isDuelOnly() ) exe = new DuelExecutor();
        else exe = new BAExecutor();
        
        if ( executor != null ) 
            exe.addMethods( executor, executor.getClass().getMethods() );

        setCommandToExecutor( plugin, cmd.toLowerCase(), exe );
        
        if ( !mp.getCommand().equalsIgnoreCase( cmd ) )
            setCommandToExecutor( plugin, mp.getCommand().toLowerCase(), exe );
    }
    
    private static void setCommandToExecutor( JavaPlugin plugin, String wantedCommand, CommandExecutor executor ) {
        
        if ( !setCommandToExecutor( plugin, wantedCommand, executor, Defaults.DEBUG_COMMANDS ) ) {
            Log.info( "[BattleArena] Now registering command " + wantedCommand + " dynamically with Bukkit commandMap." );
            
            CommandController.registerCommand( 
                    new ArenaBukkitCommand( wantedCommand, "", "", BattleArena.getSelf(), executor ) );
        }
    }

    private static boolean setCommandToExecutor( JavaPlugin plugin, String command, CommandExecutor executor, boolean displayError ) {
        try {
            plugin.getCommand( command ).setExecutor( executor );
            return true;
        } catch ( NullPointerException e ) {
            if ( displayError ) {
                Log.err( plugin.getName() + " command " + command + " was not found in plugin.yml." );
            }
            return false;
        }
    }

    static class ArenaBukkitCommand extends Command implements PluginIdentifiableCommand {

        final CommandExecutor executor;
        @Getter final Plugin plugin;

        public ArenaBukkitCommand( String name, String _description, String _usageMessage, 
                                                Plugin _plugin, CommandExecutor _executor ) {
            super( name, _description, _usageMessage, new ArrayList<>() );
            plugin = _plugin;
            executor = _executor;
        }

        @Override
        public boolean execute( CommandSender sender, String commandLabel, String[] args ) {
            return executor.onCommand( sender, this, commandLabel, args );
        }
    }

    @AllArgsConstructor
    static class DelayedRegistrationHandler implements Runnable {

        final JavaPlugin plugin;
        final File arenaFile;

        @Override
        public void run() {
            if ( !plugin.isEnabled() ) return;
            
            File compDir = plugin.getDataFolder();
            
            if ( !compDir.exists() ) return;
            
            FileFilter fileFilter = (file) -> file.toString().matches( ".*Config.yml$" );
            
            for ( File file : compDir.listFiles( fileFilter ) ) {
                String name = file.getName().substring( 0, file.getName().length() - "Config.yml".length() );
                
                if ( ArenaType.contains(name) || name.contains(".") ) continue;
                                    /// we already loaded this type, or bad type
                
                if ( !APIRegistrationController.registerCompetition( plugin, name, name, null, null, 
                                                                     new File( compDir + "/" + name + "Config.yml" ), 
                                                                     new File( compDir + "/" + name + "Messages.yml" ), 
                                                                     arenaFile ) ) {
                    Log.err( "[BattleArena] Unable to load custom competition " + name );
                }
            }
        }
    }
}
