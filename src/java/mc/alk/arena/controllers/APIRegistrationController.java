package mc.alk.arena.controllers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import lombok.Getter;
import mc.alk.arena.BattleArena;
import mc.alk.arena.executors.BAExecutor;
import mc.alk.arena.executors.CustomCommandExecutor;
import mc.alk.arena.executors.DuelExecutor;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.RegisteredCompetition;
import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.objects.arenas.ArenaFactory;
import mc.alk.arena.objects.arenas.ArenaType;
import mc.alk.arena.objects.exceptions.ConfigException;
import mc.alk.arena.objects.exceptions.InvalidOptionException;
import mc.alk.arena.serializers.ArenaSerializer;
import mc.alk.arena.serializers.ConfigSerializer;
import mc.alk.arena.serializers.MessageSerializer;
import mc.alk.arena.util.FileUpdater;
import mc.alk.arena.util.FileUtil;
import mc.alk.arena.util.Log;

public class APIRegistrationController {

    final static Set<String> delayedInits = Collections.synchronizedSet(new HashSet<String>());

    private static boolean loadFile(Plugin plugin, File fullFile, String fileName, String name, String cmd) throws IOException {
        if (fullFile.exists()) {
            return true;
        }
        try ( InputStream inputStream = FileUtil.getInputStream(plugin.getClass(), new File(fileName) ) ) {
            return inputStream != null && createFile(fullFile, name, cmd, inputStream);
        }
    }

    private static boolean loadFile(Plugin plugin, File defaultFile, File defaultPluginFile, File pluginFile,
                                                            String fullFileName, String name, String cmd) throws IOException {
        
        if ( pluginFile != null && pluginFile.exists() )  return true; 
        
        if ( name == null || name.isEmpty() || cmd == null ) return false;
        
        try ( InputStream inputStream = FileUtil.getInputStream(plugin.getClass(), new File(fullFileName)) ) {
            return createFile(pluginFile, name, cmd, inputStream);  
        }
        catch ( NullPointerException e ) {
            if ( defaultFile != null && defaultPluginFile != null ) {
                try ( InputStream inputStream = FileUtil.getInputStream(plugin.getClass(), defaultFile, defaultPluginFile) ) {
                    return createFile(pluginFile, name, cmd, inputStream);  
                }
            }
        }
        return false;
        
    }

    private static boolean createFile(File pluginFile, String name, String cmd, InputStream inputStream) {
        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        BufferedWriter fw;
        try {
            fw = new BufferedWriter(new FileWriter(pluginFile));
        } catch (IOException e) {
            Log.printStackTrace(e);
            return false;
        }
        try {
            while ((line = br.readLine()) != null) {
                line = line.replaceAll("<name>", name).replaceAll("<cmd>", cmd);
                fw.write(line + "\n");
            }
        } catch (IOException e) {
            Log.printStackTrace(e);
            return false;
        } finally {
            try {
                fw.close();
            } catch (Exception e) {
                Log.printStackTrace(e);
            }
            try {
                br.close();
            } catch (Exception e) {
                Log.printStackTrace(e);
            }
        }
        return true;
    }

    private static void setCommandToExecutor(JavaPlugin plugin, String wantedCommand, CommandExecutor executor) {
        if (!setCommandToExecutor(plugin, wantedCommand, executor, false)) {
            Log.info("[BattleArena] Now registering command " + wantedCommand + " dynamically with Bukkit commandMap.");
            List<String> aliases = new ArrayList<>();
            ArenaBukkitCommand arenaCommand = new ArenaBukkitCommand(wantedCommand, "", "", aliases, BattleArena.getSelf(), executor);
            CommandController.registerCommand(arenaCommand);
        }
    }

    private static boolean setCommandToExecutor(JavaPlugin plugin, String command, CommandExecutor executor, boolean displayError) {
        try {
            plugin.getCommand(command).setExecutor(executor);
            return true;
        } catch (Exception e) {
            if (displayError) {
                Log.err(plugin.getName() + " command " + command + " was not found in plugin.yml.");
            }
            return false;
        }
    }

    public static boolean registerCompetition( JavaPlugin plugin, String name, String cmd, ArenaFactory factory ) {
        return registerCompetition( plugin, name, cmd, factory, null );
    }

    public static boolean registerCompetition( JavaPlugin plugin, String name, String cmd, ArenaFactory factory, 
                                               CustomCommandExecutor executor ) {
        File dir = plugin.getDataFolder();
        File configFile = new File(dir.getAbsoluteFile() + "/" + name + "Config.yml");
        File msgFile = new File(dir.getAbsoluteFile() + "/" + name + "Messages.yml");
        File defaultArenaFile = new File(dir.getAbsoluteFile() + "/arenas.yml");
        
        return registerCompetition(plugin, name, cmd, factory, executor, configFile, msgFile, defaultArenaFile);
    }

    public static boolean registerCompetition( JavaPlugin plugin, String name, String cmd, ArenaFactory factory, 
                                               CustomCommandExecutor executor, File configFile, File messageFile, 
                                               File defaultArenaFile ) {
        
        return registerCompetition( plugin, name, cmd, factory, executor, configFile, messageFile,
                            new File( plugin.getDataFolder() + "/" + name + "Config.yml" ), defaultArenaFile );
    }

    public static boolean registerCompetition( JavaPlugin plugin, String name, String cmd, ArenaFactory factory, 
                                               CustomCommandExecutor executor, File configFile, File messageFile, 
                                               File defaultPluginConfigFile, File defaultArenaFile ) {
        try {
            return _registerCompetition( plugin, name, cmd, factory, executor, configFile, messageFile, 
                                                   defaultPluginConfigFile, defaultArenaFile );
        } catch (Exception e) {
            Log.err("[BattleArena] could not register " + plugin.getName() + " " + name);
            Log.err("[BattleArena] config " + configFile);
            Log.printStackTrace(e);
            return true;
        }
    }
    
    private static boolean _registerCompetition( JavaPlugin plugin, String name, String cmd, ArenaFactory factory, 
                                                 CustomCommandExecutor executor, File configFile, File messageFile, 
                                                 File defaultPluginConfigFile, File defaultArenaFile ) 
                                                         throws IOException, ConfigException, InvalidOptionException {
        /// Create our plugin folder if its not there
        File dir = plugin.getDataFolder();
        FileUpdater.makeIfNotExists(dir);

        /// Define our config files
        String configFileName = name + "Config.yml";
        String defaultConfigFileName = "defaultConfig.yml";
        File compDir = configFile.getParentFile().getAbsoluteFile();

        File pluginFile = new File(compDir.getPath() + File.separator + configFileName);
        File defaultFile = new File("default_files/competitions/" + File.separator + defaultConfigFileName);

        /// Set a delayed init on this plugin and folder to load custom types
        if (!delayedInits.contains(plugin.getName())) {
            delayedInits.add(plugin.getName());
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
                    new DelayedRegistrationHandler(plugin, compDir, defaultArenaFile));
        }

        if (!loadFile(plugin, defaultFile, defaultPluginConfigFile, pluginFile,
                name + "Config.yml", name, cmd)) {
            Log.err(plugin.getName() + " " + pluginFile.getName() + " could not be loaded!");
            Log.err("defaultFile=" + defaultFile.getAbsolutePath());
            Log.err("defaultPluginFile=" + (defaultPluginConfigFile != null ? defaultPluginConfigFile.getAbsolutePath() : "null"));
            Log.err("pluginFile=" + pluginFile.getAbsolutePath());
            return false;
        }

        ConfigSerializer config = new ConfigSerializer(plugin, pluginFile, name);
        if (config.getConfigurationSection(name) == null) {
            Log.err(plugin.getName() + " " + pluginFile.getName() + " config file could not be loaded!");
            return false;
        }
        /// What is our game type ? spleef, ctf, etc
        final ArenaType gameType = ConfigSerializer.getArenaGameType(config.getConfigurationSection(name));
        
        if (factory == null) {
            if (gameType != null) {
                factory = ArenaType.getArenaFactory(gameType);
            } else {
                Class<? extends Arena> ac = ConfigSerializer.getArenaClass(config.getConfigurationSection(name));
                factory = BattleArena.createArenaFactory(ac);
            }
            if (factory == null) {
                factory = BattleArena.createArenaFactory(Arena.class);
            }
        }
        /// load or register our arena type
        /*
        if (arenaClass == null) {
            if (gameType != null) {
                arenaClass = ArenaType.getArenaClass(gameType);
            } else {
                arenaClass = ConfigSerializer.getArenaClass(config.getConfigurationSection(name));
            }
            if (arenaClass == null) {
                arenaClass = Arena.class;
            }
        }
        ArenaType at = ArenaType.register(name, arenaClass, plugin);
        */
        ArenaType at = ArenaType.register( name, factory, plugin );

        MatchParams mp = config.loadMatchParams();

        MessageSerializer ms = null;

        if ( loadFile( plugin, messageFile, name + "Messages.yml", name, cmd) ) {
            ms = new MessageSerializer(name, mp);
        } else if (gameType != null) {
            RegisteredCompetition regComp = CompetitionController.getCompetition(plugin, gameType.getName());
            if (regComp != null) {
                ms = MessageSerializer.getMessageSerializer(gameType.getName());
            }
        }
        if (ms != null) {
            ms.setConfig(messageFile);
            ms.loadAll();
            MessageSerializer.addMessageSerializer(name, ms);
        }

        /// Everything nearly successful, register our competition
        RegisteredCompetition rc = new RegisteredCompetition(plugin, name);

        if (executor == null && gameType != null) {
            RegisteredCompetition comp = CompetitionController.getCompetition(plugin, gameType.getName());
            if (comp != null) {
                executor = comp.getCustomExecutor();
            }
        } else {
            rc.setCustomExecutor(executor);
        }

        createExecutor(plugin, cmd, executor, mp);
        rc.setConfigSerializer(config);
        CompetitionController.addRegisteredCompetition(rc);

        /// Load our arenas
        ArenaSerializer as = new ArenaSerializer( plugin, defaultArenaFile ); 
        as.loadArenas( at );
        rc.setArenaSerializer(as);

        return true;
    }

    private static void createExecutor(JavaPlugin plugin, String cmd, CustomCommandExecutor executor, MatchParams mp) {
        CustomCommandExecutor exe;

        if (mp.isDuelOnly()) {
            exe = new DuelExecutor();
        } else {
            exe = new BAExecutor();
        }

        if (executor != null) {
            exe.addMethods(executor, executor.getClass().getMethods());
        }

        /// Set command to the executor
        setCommandToExecutor(plugin, cmd.toLowerCase(), exe);
        if (!mp.getCommand().equalsIgnoreCase(cmd)) {
            setCommandToExecutor(plugin, mp.getCommand().toLowerCase(), exe);
        }
    }

    static class ArenaBukkitCommand extends Command implements PluginIdentifiableCommand {

        final CommandExecutor executor;
        @Getter final Plugin plugin;

        public ArenaBukkitCommand( String name, String _description, String _usageMessage, List<String> aliases, 
                                                Plugin _plugin, CommandExecutor _executor) {
            super( name, _description, _usageMessage, aliases );
            plugin = _plugin;
            executor = _executor;
        }

        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            return executor.onCommand(sender, this, commandLabel, args);
        }
    }

    static class DelayedRegistrationHandler implements Runnable {

        final JavaPlugin plugin;
        final File compDir;
        final File arenaFile;

        DelayedRegistrationHandler(JavaPlugin _plugin, File _compDir, File _arenaFile) {
            plugin = _plugin;
            compDir = _compDir;
            arenaFile = _arenaFile;
        }

        @Override
        public void run() {
            if (!plugin.isEnabled()) {
                return;
            }
            FileFilter fileFilter = new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.toString().matches(".*Config.yml$");
                }
            };
            if (!compDir.exists()) {
                return;
            }
            for (File file : compDir.listFiles(fileFilter)) {
                String n = file.getName().substring(0, file.getName().length() - "Config.yml".length());
                if (ArenaType.contains(n) || n.contains(".")) { /// we already loaded this type, or bad type
                    continue;
                }
                File configFile = new File(compDir + "/" + n + "Config.yml");
                File msgFile = new File(compDir + "/" + n + "Messages.yml");
                if (!APIRegistrationController.registerCompetition(
                        plugin, n /*name*/, n /*command*/, null /*Arena class*/,
                        null /*executor*/, configFile, msgFile, null, arenaFile)) {
                    Log.err("[BattleArena] Unable to load custom competition " + n);
                }
            }
        }
    }

}
