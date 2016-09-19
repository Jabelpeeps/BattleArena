package mc.alk.arena;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import lombok.Getter;
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
import mc.alk.arena.controllers.TeamController.TeamListener;
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
import mc.alk.arena.listeners.BTEntityListener;
import mc.alk.arena.listeners.SignUpdateListener;
import mc.alk.arena.listeners.competition.InArenaListener;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.modules.ArenaModule;
import mc.alk.arena.objects.victoryconditions.AllKills;
import mc.alk.arena.objects.victoryconditions.Custom;
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
import mc.alk.arena.serializers.EventScheduleSerializer;
import mc.alk.arena.serializers.MessageSerializer;
import mc.alk.arena.serializers.SignSerializer;
import mc.alk.arena.serializers.SpawnSerializer;
import mc.alk.arena.serializers.StateFlagSerializer;
import mc.alk.arena.serializers.TeamHeadSerializer;
import mc.alk.arena.serializers.YamlFileUpdater;
import mc.alk.arena.tracker.Tracker;
import mc.alk.arena.util.FileLogger;
import mc.alk.arena.util.FileUtil;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.MessageUtil;

public class BattleArena extends JavaPlugin {

    @Getter static final private String pluginname = "BattleArena";
    static private String version;
    @Getter static private BattleArena self;

    private final static SignUpdateListener signUpdateListener = new SignUpdateListener();
    @Getter private static BattleArenaController bAController = new BattleArenaController(signUpdateListener);
    @Getter static BAEventController bAEventController = new BAEventController();
    @Getter private final static EventController eventController = new EventController();
    @Getter private final static ArenaEditor arenaEditor = new ArenaEditor();
    @Getter private final static DuelController duelController = new DuelController();
    @Getter private static BAExecutor bAExecutor;
    @Getter private static ArenaEditorExecutor arenaEditorExecutor;
    private final BAPlayerListener playerListener = new BAPlayerListener(bAController);
    private final BAPluginListener pluginListener = new BAPluginListener();
    private final BASignListener signListener = new BASignListener(signUpdateListener);
    @Getter private final WatchController watchController = new WatchController();

    private ArenaControllerSerializer arenaControllerSerializer;
    @Getter private static final BAConfigSerializer bAConfigSerializer = new BAConfigSerializer();
    private static final BAClassesSerializer classesSerializer = new BAClassesSerializer();
    private static final EventScheduleSerializer eventSchedulerSerializer = new EventScheduleSerializer();
    private static final SignSerializer signSerializer = new SignSerializer();
    

    private static final Map<String, ArenaModule> modules = new HashMap<>();

    /**
     * enable the BattleArena plugin
     */
    @Override
    public void onEnable() {
        self = this;
        PluginDescriptionFile pdfFile = getDescription();
        version = pdfFile.getVersion();
        Log.setLogger(getLogger());
        
        MessageUtil.sendMessage(
                Bukkit.getConsoleSender(), "&4[" + pluginname + "] &6v" + version + "&f enabling!");

        /// Create our plugin folder if its not there
        File dir = getDataFolder();
        FileUtil.makeIfNotExists(dir);
        FileUtil.makeIfNotExists(new File(dir + "/competitions"));
        FileUtil.makeIfNotExists(new File(dir + "/messages"));
        FileUtil.makeIfNotExists(new File(dir + "/saves"));
        FileUtil.makeIfNotExists(new File(dir + "/modules"));
        FileUtil.makeIfNotExists(new File(dir + "/otherPluginConfigs"));
        FileUtil.makeIfNotExists(new File(dir + "/victoryConditions"));
        Tracker.loadConfigs();
      
        for ( String each : new String[]{ "HeroesConfig", "McMMOConfig", "WorldGuardConfig" } ) {
     
            FileUtil.load( dir.getPath() + "/otherPluginConfigs/" + each + ".yml", 
                           "/default_files/otherPluginConfigs/" + each + ".yml" );
        }
        
        for ( String each : new String[]{ "AllKills", "KillLimit", "MobKills","PlayerKills" } ) {
            FileUtil.load( dir.getPath() + "/victoryConditions/" + each + ".yml",
                           "/default_files/victoryConditions/" + each + ".yml" );
        }

        MessageSerializer defaultMessages = new MessageSerializer( "default", null );
        defaultMessages.setConfig( FileUtil.load( dir.getPath() + "/messages.yml", "/default_files/messages.yml" ) );
        
        new YamlFileUpdater(this).updateMessageSerializer(self, defaultMessages);
        
        defaultMessages.initMessageOptions();
        MessageSerializer.setDefaultMessages(defaultMessages);

        bAExecutor = new BAExecutor();
        pluginListener.loadAll(); 
        arenaControllerSerializer = new ArenaControllerSerializer();

        Bukkit.getPluginManager().registerEvents(playerListener, this);
        Bukkit.getPluginManager().registerEvents(pluginListener, this);
        Bukkit.getPluginManager().registerEvents(signListener, this);
        Bukkit.getPluginManager().registerEvents(new TeamListener(), this);
        Bukkit.getPluginManager().registerEvents(new TeleportController(), this);
        Bukkit.getPluginManager().registerEvents(InArenaListener.INSTANCE, this);
        Bukkit.getPluginManager().registerEvents(signUpdateListener, this);
        Bukkit.getPluginManager().registerEvents(bAController, this);
        Bukkit.getPluginManager().registerEvents(bAController.getArenaMatchQueue(), this);
        Bukkit.getPluginManager().registerEvents(bAEventController, this);
        Bukkit.getPluginManager().registerEvents(new BTEntityListener(), this);
        
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

        /// Load our configs, then arenas
        bAConfigSerializer.setConfig(FileUtil.load( dir.getPath() + "/config.yml", "/config.yml"));
        try {
            YamlFileUpdater.updateBaseConfig( this, bAConfigSerializer );
        } 
        catch (Exception e) {
            Log.printStackTrace(e);
        }

        bAConfigSerializer.loadDefaults(); /// Load our defaults for BattleArena, has to happen before classes are loaded

        classesSerializer.setConfig(FileUtil.load( dir.getPath() + "/classes.yml", "/default_files/classes.yml"));
        classesSerializer.loadAll();

        /// Spawn Groups need to be loaded before the arenas
        SpawnSerializer ss = new SpawnSerializer();
        ss.setConfig( FileUtil.load( dir.getPath() + "/spawns.yml", "/default_files/spawns.yml" ) );

        TeamHeadSerializer ts = new TeamHeadSerializer();
        ts.setConfig( FileUtil.load( dir.getPath() + "/teamConfig.yml", "/default_files/teamConfig.yml" ) );
        ts.loadAll();

        arenaEditorExecutor = new ArenaEditorExecutor();
        
        /// Set our commands
        getCommand("watch").setExecutor(bAExecutor);
        getCommand("arenateam").setExecutor(new TeamExecutor());
        getCommand("arenaAlter").setExecutor(arenaEditorExecutor);
        getCommand("battleArena").setExecutor(new BattleArenaExecutor());
        getCommand("battleArenaDebug").setExecutor(new BattleArenaDebugExecutor());
        EventScheduler es = new EventScheduler();
        getCommand("battleArenaScheduler").setExecutor(new BASchedulerExecutor(es));
        getCommand("arenaScoreboard").setExecutor(new ScoreboardExecutor(this, bAController, Defaults.SB_MESSAGES));

        /// Reload our scheduled events
        eventSchedulerSerializer.setConfig(dir.getPath() + "/saves/scheduledEvents.yml");
        eventSchedulerSerializer.addScheduler(es);

        createMessageSerializers();
        FileLogger.init(); /// shrink down log size

        /// Load Competitions and Arenas after everything is loaded (plugins and worlds)
        /// Other plugins using BattleArena are going to be registering
        /// Lets hold off on loading the scheduled events until those plugins have registered
        Scheduler.scheduleSynchronousTask( () -> {

                        bAConfigSerializer.loadVictoryConditions();
                        /// Load our competitions, has to happen after classes and teams
                        bAConfigSerializer.loadCompetitions(); 
                        
                        /// persist our disabled arena types
                        StateFlagSerializer sfs = new StateFlagSerializer();
                        sfs.setConfig(dir.getPath() + "/saves/state.yml");
                        bAExecutor.setDisabled(sfs.loadEnabled());
                        ArenaSerializer.setBAC(bAController);
        
                        sfs.loadLobbyStates(RoomController.getLobbies());
                        sfs.loadContainerStates(BattleArenaController.getAllArenas());
        
                        arenaControllerSerializer.load();
        
                        /// Load up our signs
                        signSerializer.setConfig(dir.getPath() + "/saves/signs.yml");
                        signSerializer.loadAll(signUpdateListener);
        
                        eventSchedulerSerializer.loadAll();
                        if (Defaults.START_NEXT)
                            es.startNext();
                        else if (Defaults.START_CONTINUOUS)
                            es.start();           
                }, 40 );
        Log.info("&4[" + pluginname + "] &6v" + BattleArena.version + "&f enabled!");
    }

    /**
     * Disable the BattleArena plugin
     */
    @Override
    public void onDisable() {
        bAController.stop();
        arenaControllerSerializer.save();
        eventSchedulerSerializer.saveScheduledEvents();
        signSerializer.saveAll(signUpdateListener);
        Tracker.saveConfig();
        
        /// Save the container states
        StateFlagSerializer sfs = new StateFlagSerializer();
        sfs.setConfig(getDataFolder().getPath() + "/saves/state.yml");
        sfs.save( bAExecutor.getDisabled(),
                  RoomController.getLobbies(),
                  BattleArenaController.getAllArenas() );
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
            ms.setConfig( FileUtil.load( f.getAbsolutePath() + "/" + mp.getName() + "Messages.yml", "/default_files/" + fileName));
            ms.initMessageOptions();;
            MessageSerializer.addMessageSerializer(mp.getName(), ms);
        }
    }

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
    public static boolean inSystem( Player player, boolean showReasons ) {
        return !bAExecutor.canJoin( PlayerController.toArenaPlayer(player), showReasons );
    }

    /**
     * Reload our own config
     */
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        bAConfigSerializer.loadDefaults();
        classesSerializer.loadAll();
        MessageSerializer.loadDefaults();
        Tracker.loadConfigs();
    }

    /**
     * Get the a versioning String
     *
     * @return [BattleArena_versionString]
     */
    public static String getNameAndVersion() {
        return "[" + BattleArena.pluginname + "_v" + BattleArena.version + "]";
    }

    /**
     * Get the module directory
     * @return File: Module Directory
     */
    public File getModuleDirectory() { return new File( getDataFolder() + "/modules"); }
    
    public static void addModule( ArenaModule mod ) { modules.put( mod.getName().toUpperCase(), mod ); }
    
    public static ArenaModule getModule( String name ) { return modules.get( name.toUpperCase() ); }
}
