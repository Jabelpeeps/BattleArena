package mc.alk.arena;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import mc.alk.arena.controllers.ArenaEditor;
import mc.alk.arena.controllers.BAEventController;
import mc.alk.arena.controllers.BattleArenaController;
import mc.alk.arena.controllers.DuelController;
import mc.alk.arena.controllers.EventController;
import mc.alk.arena.controllers.EventScheduler;
import mc.alk.arena.controllers.ParamController;
import mc.alk.arena.controllers.PlayerController;
import mc.alk.arena.controllers.RoomController;
import mc.alk.arena.controllers.Scheduler;
import mc.alk.arena.controllers.TeamController;
import mc.alk.arena.controllers.TeleportController;
import mc.alk.arena.controllers.WatchController;
import mc.alk.arena.executors.ArenaEditorExecutor;
import mc.alk.arena.executors.BAExecutor;
import mc.alk.arena.executors.BASchedulerExecutor;
import mc.alk.arena.executors.BattleArenaDebugExecutor;
import mc.alk.arena.executors.BattleArenaExecutor;
import mc.alk.arena.executors.ScoreboardExecutor;
import mc.alk.arena.executors.TeamExecutor;
import mc.alk.arena.listeners.BAPlayerListener;
import mc.alk.arena.listeners.BAPluginListener;
import mc.alk.arena.listeners.BASignListener;
import mc.alk.arena.listeners.SignUpdateListener;
import mc.alk.arena.listeners.competition.InArenaListener;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.objects.arenas.ArenaFactory;
import mc.alk.arena.objects.victoryconditions.AllKills;
import mc.alk.arena.objects.victoryconditions.Custom;
import mc.alk.arena.objects.victoryconditions.HighestKills;
import mc.alk.arena.objects.victoryconditions.InfiniteLives;
import mc.alk.arena.objects.victoryconditions.KillLimit;
import mc.alk.arena.objects.victoryconditions.LastManStanding;
import mc.alk.arena.objects.victoryconditions.MobKills;
import mc.alk.arena.objects.victoryconditions.NLives;
import mc.alk.arena.objects.victoryconditions.NoTeamsLeft;
import mc.alk.arena.objects.victoryconditions.OneTeamLeft;
import mc.alk.arena.objects.victoryconditions.PlayerKills;
import mc.alk.arena.objects.victoryconditions.TimeLimit;
import mc.alk.arena.objects.victoryconditions.VictoryType;
import mc.alk.arena.serializers.ArenaControllerSerializer;
import mc.alk.arena.serializers.ArenaSerializer;
import mc.alk.arena.serializers.BAClassesSerializer;
import mc.alk.arena.serializers.BAConfigSerializer;
import mc.alk.arena.serializers.BaseConfig;
import mc.alk.arena.serializers.EventScheduleSerializer;
import mc.alk.arena.serializers.MessageSerializer;
import mc.alk.arena.serializers.SignSerializer;
import mc.alk.arena.serializers.SpawnSerializer;
import mc.alk.arena.serializers.StateFlagSerializer;
import mc.alk.arena.serializers.TeamHeadSerializer;
import mc.alk.arena.serializers.YamlFileUpdater;
import mc.alk.util.FileLogger;
import mc.alk.util.FileUpdater;
import mc.alk.util.FileUtil;
import mc.alk.util.Log;
import mc.alk.util.MessageUtil;

public class BattleArena extends JavaPlugin {

    static private String pluginname;
    static private String version;
    static private BattleArena plugin;

    private final static SignUpdateListener signUpdateListener = new SignUpdateListener();
    private static BattleArenaController arenaController = new BattleArenaController(signUpdateListener);
    static BAEventController eventController = new BAEventController();
    private final static TeamController teamController = TeamController.INSTANCE;
    private final static EventController ec = new EventController();
    private final static ArenaEditor arenaEditor = new ArenaEditor();
    private final static DuelController duelController = new DuelController();
    private static BAExecutor commandExecutor;
    private ArenaEditorExecutor arenaEditorExecutor;
    private final BAPlayerListener playerListener = new BAPlayerListener(arenaController);
    private final BAPluginListener pluginListener = new BAPluginListener();
    private final BASignListener signListener = new BASignListener(signUpdateListener);
    private final WatchController watchController = new WatchController();

    private ArenaControllerSerializer arenaControllerSerializer;
    private static final BAConfigSerializer baConfigSerializer = new BAConfigSerializer();
    private static final BAClassesSerializer classesSerializer = new BAClassesSerializer();
    private static final EventScheduleSerializer eventSchedulerSerializer = new EventScheduleSerializer();
    private static final SignSerializer signSerializer = new SignSerializer();

    /**
     * enable the BattleArena plugin
     */
    @Override
    public void onEnable() {
        BattleArena.plugin = this;
        PluginDescriptionFile pdfFile = this.getDescription();
        BattleArena.pluginname = pdfFile.getName();
        BattleArena.version = pdfFile.getVersion();
        Log.setLogger(getLogger());
        Class<?> clazz = this.getClass();
        ConsoleCommandSender sender = Bukkit.getConsoleSender();
        MessageUtil.sendMessage(sender, "&4[" + pluginname + "] &6v" + version + "&f enabling!");

        /// Create our plugin folder if its not there
        final File dir = getDataFolder();
        FileUpdater.makeIfNotExists(dir);
        FileUpdater.makeIfNotExists(new File(dir + "/competitions"));
        FileUpdater.makeIfNotExists(new File(dir + "/messages"));
        FileUpdater.makeIfNotExists(new File(dir + "/saves"));
        FileUpdater.makeIfNotExists(new File(dir + "/modules"));
        FileUpdater.makeIfNotExists(new File(dir + "/otherPluginConfigs"));
        FileUpdater.makeIfNotExists(new File(dir + "/victoryConditions"));

        for (String c : new String[]{"HeroesConfig", "McMMOConfig", "WorldGuardConfig"}){
            try{
                String source = "/default_files/otherPluginConfigs/"+c+".yml";
                String dest = dir.getPath() + "/otherPluginConfigs/"+c+".yml";
                File file = FileUtil.load(clazz, dest, source);
                new BaseConfig(file);
            } catch( Exception e ){
                Log.err("Couldn't load File " + dir.getPath() + "/otherPluginConfigs/"+c+".yml");
                Log.printStackTrace(e);
            }
        }

        for (String c : new String[]{"AllKills", "KillLimit", "MobKills","PlayerKills"}){
            try{
                new BaseConfig(FileUtil.load(clazz, dir.getPath() + "/victoryConditions/"+c+".yml",
                        "/default_files/victoryConditions/"+c+".yml"));
            } catch( Exception e ){
                Log.err("Couldn't load File " + dir.getPath() + "/otherPluginConfigs/"+c+".yml");
                Log.printStackTrace(e);
            }
        }
        
        /// For potential updates to default yml files
        YamlFileUpdater yfu = new YamlFileUpdater(this);

        /// Set up our messages first before other initialization needs messages
        MessageSerializer defaultMessages = new MessageSerializer("default", null);
        defaultMessages.setConfig(FileUtil.load(clazz, dir.getPath() + "/messages.yml", "/default_files/messages.yml"));
        yfu.updateMessageSerializer(plugin, defaultMessages); /// Update our config if necessary
        defaultMessages.loadAll();
        MessageSerializer.setDefaultConfig(defaultMessages);

        commandExecutor = new BAExecutor();

        pluginListener.loadAll(); /// try and load plugins we want

        arenaControllerSerializer = new ArenaControllerSerializer();

        // Register our events
        Bukkit.getPluginManager().registerEvents(playerListener, this);
        Bukkit.getPluginManager().registerEvents(pluginListener, this);
        Bukkit.getPluginManager().registerEvents(signListener, this);
        Bukkit.getPluginManager().registerEvents(teamController, this);
        Bukkit.getPluginManager().registerEvents(new TeleportController(), this);
        Bukkit.getPluginManager().registerEvents(InArenaListener.INSTANCE, this);
        Bukkit.getPluginManager().registerEvents(signUpdateListener, this);
        Bukkit.getPluginManager().registerEvents(arenaController, this);
        Bukkit.getPluginManager().registerEvents(arenaController.getArenaMatchQueue(), this);
        Bukkit.getPluginManager().registerEvents(eventController, this);
        
        /// Register our different Victory Types
        VictoryType.register(LastManStanding.class, this);
        VictoryType.register(NLives.class, this);
        VictoryType.register(InfiniteLives.class, this);
        VictoryType.register(TimeLimit.class, this);
        VictoryType.register(OneTeamLeft.class, this);
        VictoryType.register(NoTeamsLeft.class, this);
        VictoryType.register(PlayerKills.class, this);
        VictoryType.register(MobKills.class, this);
        VictoryType.register(AllKills.class, this);
        VictoryType.register(KillLimit.class, this);
        VictoryType.register(Custom.class, this);
        VictoryType.register(HighestKills.class, this);

        /// Load our configs, then arenas
        baConfigSerializer.setConfig(FileUtil.load(clazz, dir.getPath() + "/config.yml", "/config.yml"));
        try {
            YamlFileUpdater.updateBaseConfig(this, baConfigSerializer); /// Update our config if necessary
        } catch (Exception e) {
            Log.printStackTrace(e);
        }

        baConfigSerializer.loadDefaults(); /// Load our defaults for BattleArena, has to happen before classes are loaded

        classesSerializer.setConfig(FileUtil.load(clazz, dir.getPath() + "/classes.yml", "/default_files/classes.yml")); /// Load classes
        classesSerializer.loadAll();

        /// Spawn Groups need to be loaded before the arenas
        SpawnSerializer ss = new SpawnSerializer();
        ss.setConfig(FileUtil.load(clazz, dir.getPath() + "/spawns.yml", "/default_files/spawns.yml"));

        TeamHeadSerializer ts = new TeamHeadSerializer();
        ts.setConfig(FileUtil.load(clazz, dir.getPath() + "/teamConfig.yml", "/default_files/teamConfig.yml")); /// Load team Colors
        ts.loadAll();

        arenaEditorExecutor = new ArenaEditorExecutor();
        
        /// Set our commands
        getCommand("watch").setExecutor(commandExecutor);
        getCommand("arenateam").setExecutor(new TeamExecutor(commandExecutor));
        getCommand("arenaAlter").setExecutor(arenaEditorExecutor);
        getCommand("battleArena").setExecutor(new BattleArenaExecutor());
        getCommand("battleArenaDebug").setExecutor(new BattleArenaDebugExecutor());
        final EventScheduler es = new EventScheduler();
        getCommand("battleArenaScheduler").setExecutor(new BASchedulerExecutor(es));
        getCommand("arenaScoreboard").setExecutor(new ScoreboardExecutor(this, arenaController, Defaults.SB_MESSAGES));

        /// Reload our scheduled events
        eventSchedulerSerializer.setConfig(dir.getPath() + "/saves/scheduledEvents.yml");
        eventSchedulerSerializer.addScheduler(es);

        createMessageSerializers();
        FileLogger.init(); /// shrink down log size

        /// Load Competitions and Arenas after everything is loaded (plugins and worlds)
        /// Other plugins using BattleArena are going to be registering
        /// Lets hold off on loading the scheduled events until those plugins have registered
        Scheduler.scheduleSynchronousTask(this, () -> {

                        baConfigSerializer.loadVictoryConditions();
                        /// Load our competitions, has to happen after classes and teams
                        baConfigSerializer.loadCompetitions(); 
                        
                        /// persist our disabled arena types
                        StateFlagSerializer sfs = new StateFlagSerializer();
                        sfs.setConfig(dir.getPath() + "/saves/state.yml");
                        commandExecutor.setDisabled(sfs.loadEnabled());
                        ArenaSerializer.setBAC(arenaController);
        
                        sfs.loadLobbyStates(RoomController.getLobbies());
                        sfs.loadContainerStates(arenaController.getArenas());
        
                        arenaControllerSerializer.load();
        
                        /// Load up our signs
                        signSerializer.setConfig(dir.getPath() + "/saves/signs.yml");
                        signSerializer.loadAll(signUpdateListener);
                        signUpdateListener.updateAllSigns();
        
                        eventSchedulerSerializer.loadAll();
                        if (Defaults.START_NEXT)
                            es.startNext();
                        else if (Defaults.START_CONTINUOUS)
                            es.start();           
                });
        Log.info("&4[" + pluginname + "] &6v" + BattleArena.version + "&f enabled!");
    }

    /**
     * Disable the BattleArena plugin
     */
    @Override
    public void onDisable() {
        arenaController.stop();
        arenaControllerSerializer.save();
        eventSchedulerSerializer.saveScheduledEvents();
        signSerializer.saveAll(signUpdateListener);
        
        /// Save the container states
        StateFlagSerializer sfs = new StateFlagSerializer();
        sfs.setConfig(getDataFolder().getPath() + "/saves/state.yml");
        sfs.save( commandExecutor.getDisabled(),
                  RoomController.getLobbies(),
                  arenaController.getArenas() );
        FileLogger.saveAll();
    }

    private void createMessageSerializers() {
        File f = new File(getDataFolder() + "/messages");
        if (!f.exists()) {
            try {
                if (!f.mkdir()) {
                    Log.err("Messages folder could not be created!");
                }
            } catch (Exception e) {
                Log.printStackTrace(e);
            }
        }
        for (MatchParams mp : ParamController.getAllParams()) {
            String fileName = "defaultMessages.yml";
            MessageSerializer ms = new MessageSerializer(mp.getName(), null);
            ms.setConfig(FileUtil.load(this.getClass(), f.getAbsolutePath() + "/" + mp.getName() + "Messages.yml", "/default_files/" + fileName));
            ms.loadAll();
            MessageSerializer.addMessageSerializer(mp.getName(), ms);
        }
    }

    /**
     * Return the watch controller
     * @return WatchController
     */
    public WatchController getWatchController() { return watchController; }
    /**
     * Return the BattleArena plugin instance
     *
     * @return BattleArena
     */
    public static BattleArena getSelf() { return plugin; }
    /**
     * Return the BattleArenaController, which handles queuing and arenas
     *
     * @return BattleArenaController
     */
    public static BattleArenaController getBAController() { return arenaController; }
    /**
     * Return the BAEventController, which handles Events
     *
     * @return BAEventController
     */
    public static BAEventController getBAEventController() { return eventController; }
    /**
     * Get the TeamController, deals with self made teams
     *
     * @return TeamController
     */
    public static TeamController getTeamController() { return teamController; }
    /**
     * Get the DuelController, deals with who is currently trying to duel other people/teams
     *
     * @return DuelController
     */
    public static DuelController getDuelController() { return duelController; }
    /**
     * Get the EventController, deals with what events can be run
     *
     * @return EventController
     */
    public static EventController getEventController() { return ec;  }
    /**
     * Get the Arena Editor, deals with Altering and changing Arenas
     *
     * @return ArenaEditor
     */
    public static ArenaEditor getArenaEditor() { return arenaEditor; }
    /**
     * Get the BAExecutor, deals with the Arena related commands
     *
     * @return BAExecutor
     */
    public static BAExecutor getBAExecutor() { return commandExecutor; }
    /**
     * The main serializer for the config.yml
     * @return BAConfigSerializer
     */
    public BAConfigSerializer getBAConfigSerializer() { return baConfigSerializer; }
    /**
     * Return the Arena Editor Executor
     * @return ArenaEditorExecutor
     */
    public ArenaEditorExecutor getArenaEditorExecutor() { return arenaEditorExecutor; }
    
    /**
     * Is the player inside of the BattleArena system
     * This means one of the following
     * Player is in a queue, in a competition, being challenged, inside MobArena,
     * being challenged to a duel, being invited to a team
     * <p/>
     * If a player is in an Arena or in a Competition this is always true
     *
     * @param player:      the player you want to check
     * @param showReasons: if player is in system, show the player a message about how to exit
     * @return true or false: whether they are in the system
     */
    public static boolean inSystem(Player player, boolean showReasons) {
        return !getBAExecutor().canJoin(PlayerController.toArenaPlayer(player), showReasons);
    }

    /**
     * Reload our own config
     */
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        baConfigSerializer.loadDefaults();
        classesSerializer.loadAll();
        MessageSerializer.loadDefaults();
    }

    /**
     * Get the a versioning String
     *
     * @return [BattleArena_versionString]
     */
    public static String getNameAndVersion() {
        return "[" + BattleArena.pluginname + "_v" + BattleArena.version + "]";
    }

    public static ArenaFactory createArenaFactory(final Class<? extends Arena> arenaClass) {
        if (arenaClass == null) return null;
        return new ArenaFactory() {

            @Override
            public Arena newArena() {
                Class<?>[] args = {};
                try {
                    Constructor<?> constructor = arenaClass.getConstructor(args);
                    Arena  arena = (Arena) constructor.newInstance((Object[]) args);                  
                    return arena;
                } catch (NoSuchMethodException ex) {
                    Log.err("If you have custom constructors for your class you must also have a public default constructor");
                    Log.err("Add the following line to your Arena Class '" + arenaClass.getSimpleName() + ".java'");
                    Log.err("public " + arenaClass.getSimpleName() + "(){}");
                    Log.err("Or you can create your own ArenaFactory to support custom constructors");
                    Log.printStackTrace(ex);
                } catch ( IllegalAccessException | InstantiationException 
                        | IllegalArgumentException | InvocationTargetException ex ) {
                    Log.printStackTrace(ex);
                }
                return null;
            }
        };
    }
    
    /**
     * Get the module directory
     * @return File: Module Directory
     */
    public File getModuleDirectory() {
        return new File( getDataFolder() + "/modules");
    }
}
