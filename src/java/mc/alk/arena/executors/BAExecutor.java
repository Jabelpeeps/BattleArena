package mc.alk.arena.executors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import lombok.Getter;
import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.Permissions;
import mc.alk.arena.competition.AbstractComp;
import mc.alk.arena.competition.Competition;
import mc.alk.arena.competition.Match;
import mc.alk.arena.controllers.ArenaAlterController;
import mc.alk.arena.controllers.ArenaAlterController.ArenaOptionPair;
import mc.alk.arena.controllers.ArenaAlterController.ChangeType;
import mc.alk.arena.controllers.ArenaClassController;
import mc.alk.arena.controllers.BAEventController;
import mc.alk.arena.controllers.CompetitionController;
import mc.alk.arena.controllers.DuelController;
import mc.alk.arena.controllers.EventController;
import mc.alk.arena.controllers.MoneyController;
import mc.alk.arena.controllers.ParamAlterController;
import mc.alk.arena.controllers.ParamController;
import mc.alk.arena.controllers.PlayerController;
import mc.alk.arena.controllers.RoomController;
import mc.alk.arena.controllers.TeamController;
import mc.alk.arena.controllers.WatchController;
import mc.alk.arena.controllers.containers.LobbyContainer;
import mc.alk.arena.controllers.containers.RoomContainer;
import mc.alk.arena.controllers.joining.AbstractJoinHandler;
import mc.alk.arena.controllers.messaging.MessageHandler;
import mc.alk.arena.controllers.tracker.TrackerInterface;
import mc.alk.arena.events.ArenaCreateEvent;
import mc.alk.arena.events.ArenaDeleteEvent;
import mc.alk.arena.events.players.ArenaPlayerJoinEvent;
import mc.alk.arena.events.players.ArenaPlayerLeaveEvent;
import mc.alk.arena.events.players.ArenaPlayerLeaveEvent.QuitReason;
import mc.alk.arena.listeners.PlayerHolder;
import mc.alk.arena.listeners.competition.InArenaListener;
import mc.alk.arena.objects.ArenaClass;
import mc.alk.arena.objects.ArenaLocation;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.ArenaSize;
import mc.alk.arena.objects.CompetitionSize;
import mc.alk.arena.objects.CompetitionState;
import mc.alk.arena.objects.ContainerState;
import mc.alk.arena.objects.Duel;
import mc.alk.arena.objects.LocationType;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.MatchState;
import mc.alk.arena.objects.PlayerSave;
import mc.alk.arena.objects.StateGraph;
import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.objects.arenas.ArenaControllerInterface;
import mc.alk.arena.objects.arenas.ArenaType;
import mc.alk.arena.objects.exceptions.InvalidOptionException;
import mc.alk.arena.objects.joining.TeamJoinObject;
import mc.alk.arena.objects.messaging.AnnouncementOptions;
import mc.alk.arena.objects.messaging.Channel;
import mc.alk.arena.objects.options.AlterParamOption;
import mc.alk.arena.objects.options.DuelOptions;
import mc.alk.arena.objects.options.DuelOptions.DuelOption;
import mc.alk.arena.objects.options.EventOpenOptions;
import mc.alk.arena.objects.options.JoinOptions;
import mc.alk.arena.objects.options.StateOptions;
import mc.alk.arena.objects.options.TransitionOption;
import mc.alk.arena.objects.pairs.JoinResult;
import mc.alk.arena.objects.pairs.ParamAlterOptionPair;
import mc.alk.arena.objects.pairs.TransitionOptionTuple;
import mc.alk.arena.objects.spawns.FixedLocation;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.objects.teams.FormingTeam;
import mc.alk.arena.objects.teams.TeamFactory;
import mc.alk.arena.objects.teams.TeamIndex;
import mc.alk.arena.objects.tracker.StatType;
import mc.alk.arena.plugins.CombatTagUtil;
import mc.alk.arena.plugins.EssentialsUtil;
import mc.alk.arena.plugins.HeroesController;
import mc.alk.arena.plugins.MobArenaUtil;
import mc.alk.arena.serializers.ArenaSerializer;
import mc.alk.arena.tracker.Tracker;
import mc.alk.arena.util.InventoryUtil;
import mc.alk.arena.util.InventoryUtil.PInv;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.MessageUtil;
import mc.alk.arena.util.MinMax;
import mc.alk.arena.util.ServerUtil;
import mc.alk.arena.util.TeamUtil;
import mc.alk.arena.util.TimeUtil;

/**
 *
 * @author alkarin
 *
 */
public class BAExecutor extends CustomCommandExecutor {

    @Getter Set<String> disabled = new HashSet<>();

    final TeamController teamController;
    final DuelController duelController;
    final WatchController watchController;

    public BAExecutor() {
        super();
        teamController = BattleArena.getTeamController();
        duelController = BattleArena.getDuelController();
        watchController = BattleArena.getSelf().getWatchController();
    }

    @MCCommand( cmds = {"enable"}, admin = true, perm = "arena.enable", usage = "enable" )
    public boolean arenaEnable(CommandSender sender, MatchParams mp, String[] args) {
        
        if (args.length > 1 && args[1].equalsIgnoreCase("all")) {
            
            Set<String> set = new HashSet<>(); 

            for (MatchParams param : ParamController.getAllParams()) {
                disabled.remove(param.getName());
                set.add(param.getName());
            }
            for (String s : set) {
                MessageUtil.sendSystemMessage(sender, "type_enabled", s);
            }
            return true;
        }
        disabled.remove(mp.getName());
        return MessageUtil.sendSystemMessage(sender, "type_enabled", mp.getName());
    }

    @MCCommand( cmds = {"disable"}, admin = true, perm = "arena.enable", usage = "disable" )
    public boolean arenaDisable(CommandSender sender, MatchParams mp, String[] args) {
        
        if (args.length > 1 && args[1].equalsIgnoreCase("all")) {
            Set<String> set = new HashSet<>(); 
            
            for (MatchParams param : ParamController.getAllParams()) {
                disabled.add(param.getName());
                set.add(param.getName());
            }
            for (String s : set) {
                MessageUtil.sendSystemMessage(sender, "type_disabled", s);
            }
            return true;
        }
        disabled.add(mp.getName());
        return MessageUtil.sendSystemMessage(sender, "type_disabled", mp.getName());
    }

    @MCCommand( cmds = {"enabled"}, admin = true )
    public boolean arenaCheckArenaTypes(CommandSender sender) {
        String types = ArenaType.getValidList();
        MessageUtil.sendMessage(sender, "&e valid types are = &6" + types);
        return MessageUtil.sendMessage(sender, "&5Enabled types = &6 " + ParamController.getAvaibleTypes(disabled));
    }

    @MCCommand( cmds = {"join", "j"}, usage = "add [options]", helpOrder = 1 )
    public boolean join(ArenaPlayer player, MatchParams mp, String args[]) {
        return join(player, mp, args, false);
    }

    public boolean join(ArenaPlayer player, final MatchParams omp, String args[], boolean adminJoin) {
        
        JoinOptions jp;
        try {
            jp = JoinOptions.parseOptions(omp, player, Arrays.copyOfRange(args, 1, args.length));
        } 
        catch (InvalidOptionException e) {
            return MessageUtil.sendMessage(player, e.getMessage());
        } 
        catch (Exception e) {
            Log.printStackTrace(e);
            return MessageUtil.sendMessage(player, e.getMessage());
        }
        return join(player, omp, jp, adminJoin);
    }

    public boolean join(ArenaPlayer player, final MatchParams omp, JoinOptions jp, boolean adminJoin) {

        final StateGraph ops = omp.getStateGraph();
        if (ops == null) {
            return MessageUtil.sendMessage(player, "&cThis match type has no valid options, contact an admin to fix");
        }

        if (isDisabled(player.getPlayer(), omp) && !Permissions.isAdmin(player.getPlayer())) {
            return true;
        }

        if (!adminJoin && !Permissions.hasMatchPerm(player.getPlayer(), omp, "join")) {
            return MessageUtil.sendSystemMessage(player, "no_join_perms", omp.getCommand());
        }

        if (!canJoin(player)) {
            return true;
        }

        ArenaPlayerJoinEvent event = new ArenaPlayerJoinEvent(player);
        event.callEvent();
        if (event.isCancelled()) {

            if (event.getMessage() != null && !event.getMessage().isEmpty()) {
                return MessageUtil.sendMessage(player, event.getMessage());
            }
            return true;
        }

        ArenaTeam t = teamController.getSelfFormedTeam(player);
        if (t == null) {
            t = TeamController.createTeam(omp, player);
        }

        if (!canJoin(t, true)) {
            MessageUtil.sendSystemMessage(player, "teammate_cant_join", omp.getName());
            return MessageUtil.sendMessage(player, "&6/team leave: &cto leave the team");
        }

        MatchParams mp = jp.getMatchParams();
        Arena arena = arenaController.getArenaByMatchParams(jp);
        
        if (arena == null) {
            if (!jp.hasWantedTeamSize()) {
                arena = arenaController.getArenaByNearbyMatchParams(jp);
                if (arena != null) {
                    mp.setMinTeamSize(arena.getParams().getMinTeamSize());
                    mp.setMaxTeamSize(arena.getParams().getMaxTeamSize());
                }
            }
            if (arena == null) {
                Map<Arena, List<String>> reasons = arenaController.getNotMachingArenaReasons(jp);
                if (!reasons.isEmpty()) {
                    for (Arena a : reasons.keySet()) {
                        List<String> rs = reasons.get(a);
                        if (!rs.isEmpty()) {
                            return MessageUtil.sendMessage(player, "&c" + rs.get(0));
                        }
                    }
                }
                return MessageUtil.sendSystemMessage(player, "valid_arena_not_built", mp.getName());
            }
        }

        if (!arena.isJoinable(mp)) {
            return MessageUtil.sendMessage(player,
                    "&c" + arena.getName() + " can't be joined at this time.\n"
                    + arena.getNotJoinableReasons(mp));
        }

        /// Check for lobbies
        if (ops.hasAnyOption(TransitionOption.TELEPORTLOBBY)
                && !RoomController.hasLobby(mp.getType())) {
            return MessageUtil.sendMessage(player,
                    "&cThis match has no lobby and needs one! contact an admin to fix");
        }

        /// Check if the team is ready
        if (!ops.teamReady(t, null)) {
            t.sendMessage(ops.getRequiredString(MessageHandler.getSystemMessage("need_the_following") + "\n"));
            return true;
        }

        /// Check entrance fee
        if (!checkAndRemoveFee(mp, t)) {
            return true;
        }

        TeamJoinObject tqo = new TeamJoinObject(t, mp, jp);
        JoinResult jr;
        try {
            /// Add them to the queue
            jr = arenaController.wantsToJoin(tqo);
        } catch (IllegalStateException e) {
            return MessageUtil.sendMessage(player, "&c" + e.getMessage());
        }
        AnnouncementOptions ao = mp.getAnnouncementOptions();

        /// switch over to the joined params
        mp = jr.params;
        /// Annouce to the server if they have the option set

        Channel channel = ao != null ? ao.getChannel(true, MatchState.ONENTERQUEUE)
                                     : AnnouncementOptions.getDefaultChannel(true, MatchState.ONENTERQUEUE);
        
        String neededPlayers = jr.maxPlayers == CompetitionSize.MAX ? "inf" : jr.maxPlayers + "";
        List<Object> vars = new ArrayList<>();
        vars.add(mp);
        vars.add(t);
        channel.broadcast(MessageHandler.getSystemMessage(
                vars, "server_joined_the_queue", mp.getPrefix(),
                player.getDisplayName(), jr.playersInQueue, neededPlayers));
        String sysmsg;
        
        switch (jr.status) {
            case ADDED_TO_EXISTING_MATCH:
                if (t.size() == 1) {
                    t.sendMessage(MessageHandler.getSystemMessage(
                            "you_joined_event", mp.getName()));
                } else {
                    t.sendMessage(MessageHandler
                            .getSystemMessage("you_added_to_team"));
                }
                break;
            case ADDED_TO_QUEUE:
                sysmsg = MessageHandler.getSystemMessage("joined_the_queue",
                        mp.getName(), jr.pos, neededPlayers);

                StringBuilder msg = new StringBuilder(sysmsg != null ? sysmsg
                        : "&eYou joined the &6" + mp.getName() + "&e queue");
                if (jr.maxPlayers != CompetitionSize.MAX) {
                    String posmsg = MessageHandler.getSystemMessage(
                            "position_in_queue", jr.pos, neededPlayers);
                    msg.append(posmsg != null ? posmsg : "");
                }
                if (jr.time != null) {
                    Long time = jr.time - System.currentTimeMillis();
                    msg.append(constructMessage(jr.params, time, jr.playersInQueue, jr.pos));
                }

                t.sendMessage(msg.toString());
                break;
            default:
                break;
        }
        return true;
    }

    public static String constructMessage(MatchParams mp, long millisRemaining, int playersInQ, Integer position) {
        
        StringBuilder msg = new StringBuilder();
        if (millisRemaining <= 0) {
            String max = mp.getMaxPlayers() == ArenaSize.MAX ? "\u221E" : mp.getMaxPlayers() + "";
            msg.append("\n").append( MessageHandler.getSystemMessage( "match_starts_immediately",
                                        mp.getMinPlayers() - playersInQ, playersInQ, max ) );
        } 
        else {
            if (mp.getMaxPlayers() == CompetitionSize.MAX) {
                
                if (playersInQ < mp.getMinPlayers())
                    msg.append("\n").append( MessageHandler.getSystemMessage( "match_starts_players_or_time2", 
                                                                      TimeUtil.convertMillisToString(millisRemaining), 
                                                                      mp.getMinPlayers() - playersInQ ) );
                
                else if (playersInQ < mp.getMaxPlayers()) 
                    msg.append("\n").append( MessageHandler.getSystemMessage( "match_starts_when_time", 
                                                                        TimeUtil.convertMillisToString(millisRemaining)));
                else 
                    msg.append("\n").append( MessageHandler.getSystemMessage("you_start_when_free") );
            } 
            else if (mp.getMinPlayers() == mp.getMaxPlayers() ) {
                
                if (playersInQ < mp.getMinPlayers()) 
                    msg.append("\n").append( MessageHandler.getSystemMessage( "match_starts_immediately",
                                                                              mp.getMaxPlayers() - playersInQ, 
                                                                              playersInQ, 
                                                                              mp.getMinPlayers()));
                else 
                    msg.append("\n").append( MessageHandler.getSystemMessage("you_start_when_free"));
            } 
            else if (playersInQ < mp.getMinPlayers()) {
                msg.append("\n").append( MessageHandler.getSystemMessage( "match_starts_players_or_time", 
                                                                          mp.getMaxPlayers() - playersInQ, 
                                                                          TimeUtil.convertMillisToString(millisRemaining),
                                                                          mp.getMinPlayers() ) );
            } 
            else if (playersInQ < mp.getMaxPlayers()) {
                msg.append("\n").append( MessageHandler.getSystemMessage( "match_starts_players_or_time3",
                                                                          mp.getMaxPlayers() - playersInQ, 
                                                                          TimeUtil.convertMillisToString(millisRemaining)));
            } 
            else if (position == null) 
                msg.append("\n").append( MessageHandler.getSystemMessage("you_start_when_free"));
            else 
                msg.append("\n").append( MessageHandler.getSystemMessage("you_start_when_free_pos", position));
            
        }
        return msg.toString();
    }

    protected boolean isDisabled(CommandSender sender, MatchParams mp) {
        if (disabled.contains(mp.getName())) {
            
            MessageUtil.sendSystemMessage(sender, "match_disabled", mp.getName());
            final String enabled = ParamController.getAvaibleTypes(disabled);
            
            if (enabled.isEmpty()) {
                return MessageUtil.sendSystemMessage(sender, "all_disabled");
            }
            return MessageUtil.sendSystemMessage(sender, "currently_enabled", enabled);
        }
        return false;
    }

    @MCCommand( cmds = {"leave", "l"}, usage = "leave", helpOrder = 2 )
    public boolean leave(ArenaPlayer p, MatchParams mp) {
        return leave(p, mp, false);
    }

    @MCCommand( cmds = {"switch"}, perm = "arena.switch" )
    public boolean switchTeam(ArenaPlayer p, MatchParams mp, String teamStr) {
        
        Integer index = TeamUtil.getFromHumanTeamIndex(teamStr);
        if (index == null) {
            return MessageUtil.sendMessage(p, "&cBad team index");
        }

        ArenaLocation loc = p.getCurLocation();
        PlayerHolder ph = loc.getPlayerHolder();
        Competition c = p.getCompetition();
        
        if (c == null && (ph == null || loc.getType() == LocationType.HOME)) {
            if (arenaController.isInQue(p)) {
                JoinOptions jo = p.getMetaData().getJoinOptions();
                if (jo != null) {
                    jo.setOption(JoinOptions.JoinOption.TEAM, index);
                    return MessageUtil.sendMessage(p, "&eSwitched to team &6" + index);
                }
            }
        } 
        else if (c == null) {
            if (ph instanceof RoomContainer) {
                /// they are not in a match, but are in the waitroom beforehand

            } 
            else {
                /// They aren't in anywhere
            }
        } 
        else { /// they are in a competition
            if (c instanceof Match) {
                AbstractJoinHandler tjh = ((Match) c).getTeamJoinHandler();
                tjh.switchTeams(p, index, true);
            } 
            else {
                /// Not a match (like a tournament), they can't switch
            }
        }
        return true;
    }

    public boolean leave(ArenaPlayer p, MatchParams mp, boolean adminLeave) {
        if (!adminLeave && !p.hasPermission("arena.leave")
                && !Permissions.hasMatchPerm(p.getPlayer(), mp, "leave")) {
            return true;
        }

        ArenaPlayerLeaveEvent event = new ArenaPlayerLeaveEvent(p, p.getTeam(), QuitReason.QUITCOMMAND);
        event.callEvent();
        if (event.getMessages() != null && !event.getMessages().isEmpty()) {
            MessageUtil.sendMessage(event.getPlayer(), event.getMessages());
        } else {
            MessageUtil.sendSystemMessage(p, "you_not_in_queue");
        }
        return true;
    }

    // @MCCommand(cmds={"ready","r"}, inGame=true)
    // public boolean ready(ArenaPlayer player) {
    // boolean wasReady = player.isReady();
    // if (wasReady){
    // return sendMessage(player,"&cYou are already ready");
    // }
    // player.setReady(true);
    // sendMessage(player,"&2You are now ready");
    // new PlayerReadyEvent(player,player.isReady()).callEvent();
    // return true;
    // }
    
    @MCCommand( cmds = {"cancel"}, admin = true, usage = "cancel <arenaname or player>" )
    public boolean arenaCancel(CommandSender sender, MatchParams params, String[] args) {
        if (args.length > 1 && args[1].equalsIgnoreCase("all")) {
            return cancelAll(sender);
        }
        List<Match> matches = arenaController.getRunningMatches(params);
        if (!matches.isEmpty()) {
            for (Match m : matches) {
                m.cancelMatch();
                if (m.getState() == MatchState.ONCANCEL) {
                    Arena arena = m.getArena();
                    arenaController.removeArena(arena);
                    arenaController.addArena(arena);
                }
            }
            return MessageUtil.sendMessage(sender, "&2You have canceled the matches for &6" + params.getType());
        }
        if (args.length < 2) {
            return MessageUtil.sendMessage(sender, "cancel <arenaname or player>");
        }
        Player player = ServerUtil.findPlayer(args[1]);
        if (player != null) {
            ArenaPlayer ap = PlayerController.toArenaPlayer(player);
            if (arenaController.cancelMatch(ap)) {
                return MessageUtil.sendMessage( sender, "&2You have canceled the match for &6" + player.getName());
            }
            return MessageUtil.sendMessage(sender, "&cMatch couldnt be found for &6" + player.getName());
        }
        String arenaName = args[1];
        Arena arena = arenaController.getArena(arenaName);
        if (arena == null) {
            return MessageUtil.sendMessage(sender, "&cArena " + arenaName + " not found");
        }
        if (arenaController.cancelMatch(arena)) {
            return MessageUtil.sendMessage(sender, "&2You have canceled the match in arena &6" + arenaName);
        }
        return MessageUtil.sendMessage(sender, "&cError cancelling arena match");
    }

    private boolean cancelAll(CommandSender sender) {
        Collection<ArenaTeam> teams = arenaController.purgeQueue();
        for (ArenaTeam t : teams) {
            t.sendMessage("&cYou have been removed from the queue");
        }
        arenaController.cancelAllArenas();
        eventController.cancelAll();
        RoomController.cancelAll();
        return MessageUtil.sendMessage(sender, "&2You have cancelled all matches/events and cleared the queue");
    }

    @MCCommand(cmds = {"status"}, admin = true, min = 2, usage = "status <arena or player>")
    public boolean arenaStatus(CommandSender sender, String[] args) {
        Match am;
        String pormatch = args[1];
        Arena a = arenaController.getArena(pormatch);
        Player player;
        if (a == null) {
            player = ServerUtil.findPlayer(pormatch);
            if (player == null) {
                return MessageUtil.sendMessage(sender, "&eCouldnt find arena or player=" + pormatch);
            }
            ArenaPlayer ap = PlayerController.toArenaPlayer(player);
            am = arenaController.getMatch(ap);
            if (am == null) {
                return MessageUtil.sendMessage(sender, "&ePlayer " + pormatch + " is not in a match");
            }
        } else {
            am = arenaController.getArenaMatch(a);
            if (am == null) {
                return MessageUtil.sendMessage(sender, "&earena " + pormatch + " is not being used in a match");
            }
        }
        return MessageUtil.sendMessage(sender, am.getMatchInfo());
    }

    @MCCommand(cmds = {"winner"}, admin = true, min = 2, usage = "winner <player>")
    public boolean arenaSetVictor(CommandSender sender, ArenaPlayer ap) {
        Match am = arenaController.getMatch(ap);
        if (am == null) {
            return MessageUtil.sendMessage(sender, "&ePlayer " + ap.getName() + " is not in a match");
        }
        am.setVictor(ap);
        return MessageUtil.sendMessage(sender, "&6" + ap.getName() + " has now won the match!");
    }

    @MCCommand(cmds = {"resetElo"}, op = true, usage = "resetElo")
    public boolean resetElo(CommandSender sender, MatchParams mp) {
        if (!Tracker.hasInterface(mp.getName())) {
            return MessageUtil.sendMessage(sender, "&eThere is no tracking for " + mp.getName());
        }
        TrackerInterface sc = Tracker.getInterface( mp );
        sc.resetStats();
        return MessageUtil.sendMessage(sender, mp.getPrefix() + " &2Elo's and stats for &6" + mp.getName() + "&2 now reset");
    }

    @MCCommand(cmds = {"setRating"}, admin = true, usage = "setRating <player> <rating>")
    public boolean setElo(CommandSender sender, MatchParams mp,
            OfflinePlayer player, int rating) {
        if (!Tracker.hasInterface(mp.getName())) {
            return MessageUtil.sendMessage(sender, "&eThere is no tracking for " + mp.getName());
        }
        TrackerInterface sc = Tracker.getInterface( mp );
        if (sc.setRating(player, rating)) {
            return MessageUtil.sendMessage(sender, "&6" + player.getName() + "&e now has &6" + rating + "&e rating");
        }
        return MessageUtil.sendMessage(sender, "&6Error setting rating");
    }

    @MCCommand(cmds = {"rank"}, helpOrder = 3)
    public boolean rank(Player sender, MatchParams mp) {
        if (!Tracker.hasInterface(mp.getName())) {
            return MessageUtil.sendMessage(sender, "&cThere is no tracking for &6" + mp.getName());
        }
        TrackerInterface sc = Tracker.getInterface( mp );
        String rankMsg = sc.getRankMessage(sender);
        return MessageUtil.sendMessage(sender, rankMsg);
    }

    @MCCommand(cmds = {"rank"}, helpOrder = 4)
    public boolean rankOther(CommandSender sender, MatchParams mp, OfflinePlayer player) {

        if (!Tracker.hasInterface(mp.getName())) {
            return MessageUtil.sendMessage(sender, "&cThere is no tracking for " + mp.getName());
        }
        TrackerInterface sc = Tracker.getInterface( mp );
        String rankMsg = sc.getRankMessage(player);
        return MessageUtil.sendMessage(sender, rankMsg);
    }

    @MCCommand(cmds = {"top"}, helpOrder = 5)
    public boolean top(CommandSender sender, MatchParams mp, String[] args) {
        final int length = args.length;
        int teamSize = 1;
        int x = 5;
        if (length > 1) {
            try {
                x = Integer.valueOf(args[1]);
            } catch (Exception e) {
                return MessageUtil.sendMessage(sender, "&e top length " + args[1] + " is not a number");
            }
        }
        if (length > 2) {
            try {
                teamSize = Integer.valueOf(args[length - 1]);
            } catch (Exception e) {
                return MessageUtil.sendMessage(sender, "&e team size " + args[length - 1] + " is not a number");
            }
        }
        MatchParams top = new MatchParams(mp.getType());
        top.setParent(mp);
        top.setTeamSize(teamSize);
        return getTop(sender, x, top);
    }

    public boolean getTop(CommandSender sender, int x, MatchParams mp) {
        if (x < 1 || x > 100) {
            x = 5;
        }
        if (!Tracker.hasInterface(mp.getName())) {
            return MessageUtil.sendMessage(sender, "&eThere is no tracking for " + mp.getName());
        }

        final String teamSizeStr = (mp.getMinTeamSize() > 1 ? "teamSize=&6" + mp.getMinTeamSize() : "");
        final String arenaString = mp.getType().toPrettyString( mp.getMinTeamSize(), mp.getMaxTeamSize());

        final String headerMsg = "&4Top {x} Gladiators in &6" + arenaString + "&e " + teamSizeStr;
        final String bodyMsg = "&e#{rank}&4 {name} - {wins}:{losses}&6[{ranking}]";

        TrackerInterface sc = Tracker.getInterface( mp );
        sc.printTopX(sender, StatType.RANKING, x, mp.getMinTeamSize(), headerMsg, bodyMsg);
        return true;
    }

    @MCCommand(cmds = {"auto"}, admin = true, perm = "arena.auto")
    public boolean arenaAuto(CommandSender sender, MatchParams params, String args[]) {
        try {
            EventOpenOptions eoo = EventOpenOptions.parseOptions(args, null, params);

            Arena arena = eoo.getArena(params);
            if (arena == null) {
                return MessageUtil.sendMessage(sender, "[BattleArena] auto args=" + Arrays.toString(args)
                        + " can't be started. Arena  is not there or in use");
            }

            arenaController.createAndAutoMatch(arena, eoo);
            final int max = arena.getParams().getMaxPlayers();
            final String maxPlayers = max == ArenaSize.MAX ? "&6any&2 number of players"
                                                           : max + "&2 players";
            MessageUtil.sendMessage(sender,
                    "&2You have " + args[0] + "ed a &6" + params.getName()
                    + "&2 inside &6" + arena.getName()
                    + " &2TeamSize=&6" + arena.getParams().getTeamSize()
                    + "&2 #Teams=&6" + arena.getParams().getNTeams()
                    + "&2 supporting " + maxPlayers + "&2 at &5" + arena.getName());
        } catch (InvalidOptionException e) {
            MessageUtil.sendMessage(sender, e.getMessage());
        } catch (Exception e) {
            MessageUtil.sendMessage(sender, e.getMessage());
            Log.printStackTrace(e);
        }
        return true;
    }

    @MCCommand(cmds = {"open"}, admin = true, exact = 2, perm = "arena.open")
    public boolean arenaOpen(CommandSender sender, MatchParams mp,
            String arenaName) {
        if (arenaName.equalsIgnoreCase("all")) {
            arenaController.openAll(mp);
            return MessageUtil.sendMessage(sender, "&6Arenas for " + mp.getName() + ChatColor.YELLOW + " are now &2open");
            
        } else if (arenaName.equalsIgnoreCase("lobby")) {
            LobbyContainer lc = RoomController.getLobby(mp.getType());
            if (lc == null) {
                return MessageUtil.sendMessage(sender, "&cYou need to set a lobby for " + mp.getName());
            }
            lc.setContainerState(ContainerState.OPEN);
            return MessageUtil.sendMessage(sender, "&6 Lobby for " + mp.getName() + ChatColor.YELLOW + " is now &2open");
        } else {
            Arena arena = arenaController.getArena(arenaName);
            if (arena == null) {
                return MessageUtil.sendMessage(sender, "&cArena " + arenaName + " could not be found");
            }

            arena.setAllContainerState(ContainerState.OPEN);
            return MessageUtil.sendMessage(sender, "&6" + arena.getName() + ChatColor.YELLOW + " is now &2open");
        }
    }

    @MCCommand(cmds = {"open"}, admin = true, perm = "arena.open")
    public boolean arenaOpenContainer(CommandSender sender, Arena arena,
            ChangeType type) {
        try {
            if (type == ChangeType.LOBBY) {
                LobbyContainer lc = RoomController.getLobby(arena
                        .getArenaType());
                if (lc == null) {
                    return MessageUtil.sendMessage(sender, "&cYou need to set a lobby for " + arena.getArenaType().getName());
                }
                lc.setContainerState(ContainerState.OPEN);
            } else {
                arena.setContainerState(type, ContainerState.OPEN);
            }
        } catch (IllegalStateException e) {
            return MessageUtil.sendMessage(sender, "&c" + e.getMessage());
        }
        return MessageUtil.sendMessage(sender, "&6" + arena.getName() + ChatColor.YELLOW + " is now &2open");
    }

    @MCCommand(cmds = {"close"}, admin = true, exact = 2, perm = "arena.close")
    public boolean arenaClose(CommandSender sender, MatchParams mp,
            String arenaName) {
        if (arenaName.equals("all")) {
            for (Arena arena : arenaController.getArenas(mp)) {
                arena.setAllContainerState(ContainerState.CLOSED);
            }
            return MessageUtil.sendMessage(sender, "&6Arenas for " + mp.getName() + ChatColor.YELLOW + " are now &4closed");
            
        } else if (arenaName.equalsIgnoreCase("lobby")) {
            LobbyContainer lc = RoomController.getLobby(mp.getType());
            if (lc == null) {
                return MessageUtil.sendMessage(sender, "&cYou need to set a lobby for " + mp.getName());
            }
            lc.setContainerState(ContainerState.CLOSED);
            return MessageUtil.sendMessage(sender, "&6 Lobby for " + mp.getName() + ChatColor.YELLOW + " is now &4closed");
            
        } else {
            Arena arena = arenaController.getArena(arenaName);
            if (arena == null) {
                return MessageUtil.sendMessage(sender, "&cArena " + arenaName + " could not be found");
            }
            arena.setAllContainerState(ContainerState.CLOSED);
            return MessageUtil.sendMessage(sender, "&6" + arena.getName() + ChatColor.YELLOW + " is now &4closed");
        }
    }

    @MCCommand(cmds = {"close"}, admin = true, perm = "arena.close")
    public boolean arenaCloseContainer(CommandSender sender, Arena arena,
            ChangeType closeLocation) {
        try {
            arena.setContainerState(closeLocation, ContainerState.CLOSED);
        } catch (IllegalStateException e) {
            return MessageUtil.sendMessage(sender, "&c" + e.getMessage());
        }
        return MessageUtil.sendMessage(sender, "&6" + arena.getName() + ChatColor.YELLOW + " " + closeLocation + " is now &4closed");
    }

    @MCCommand(cmds = {"delete"}, admin = true, perm = "arena.delete")
    public boolean arenaDelete(CommandSender sender, Arena arena) {
        new ArenaDeleteEvent(arena).callEvent();
        arenaController.deleteArena(arena);
        ArenaSerializer.saveArenas(arena.getArenaType().getPlugin());
        return MessageUtil.sendMessage(sender, ChatColor.GREEN + "You have deleted the arena &6" + arena.getName());
    }

    @MCCommand(cmds = {"save"}, admin = true, perm = "arena.save")
    public boolean arenaSave(CommandSender sender) {
        ArenaSerializer.saveAllArenas(true);
        return MessageUtil.sendMessage(sender, "&eArenas saved");
    }

    @MCCommand(cmds = {"reload"}, admin = true, perm = "arena.reload")
    public boolean arenaReload(CommandSender sender, MatchParams mp) {
        Plugin plugin = mp.getType().getPlugin();
        BAEventController baec = BattleArena.getBAEventController();
        if (arenaController.hasRunningMatches() || !arenaController.isQueueEmpty() || baec.hasOpenEvent()) {
            MessageUtil.sendMessage( sender,
                    "&cYou can't reload the config while matches are running or people are waiting in the queue");
            
        return MessageUtil.sendMessage( sender, "&cYou can use &6/arena cancel all&c to cancel all matches and clear queues");
        }

        arenaController.stop();
        BattleArena.getSelf().reloadConfig();

        // / Get rid of any current players
        PlayerController.clearArenaPlayers();
        CompetitionController.reloadCompetition(plugin, mp);
        arenaController.resume();
        return MessageUtil.sendMessage(sender, "&6" + plugin.getName() + "&e configuration reloaded");
    }

    @MCCommand(cmds = {"info"}, exact = 1, usage = "info")
    public boolean arenaInfo(CommandSender sender, MatchParams mp) {
        String info = StateOptions.getInfo(mp, mp.getName());
        return MessageUtil.sendMessage(sender, info);
    }

    @MCCommand(cmds = {"info"}, admin = true, usage = "info <arenaname>", order = 1, helpOrder = 6)
    public boolean info(CommandSender sender, Arena arena) {
        MessageUtil.sendMessage(sender, arena.toDetailedString());
        Match match = arenaController.getMatch(arena);
        if (match != null) {
            List<String> strs = new ArrayList<>();
            for (ArenaTeam t : match.getTeams()) {
                strs.add("&5 -&e" + t.getDisplayName());
            }
            MessageUtil.sendMessage(sender, "Teams: " + StringUtils.join(strs, ", "));
        }
        return true;
    }

    @MCCommand(cmds = {"watch"}, subCmds = {"leave"})
    public boolean watchLeave(ArenaPlayer sender) {
        if (!watchController.hasWatcher(sender)) {
            return MessageUtil.sendMessage(sender, "&cYou aren't watching any arenas");
        }
        watchController.leave(sender);
        return MessageUtil.sendMessage(sender, "&eYou stopped watching");
    }

    @MCCommand(cmds = {"watch"})
    public boolean watch(ArenaPlayer sender, MatchParams mp, ArenaPlayer player) {
        if (player.getCompetition() == null || !(player.getCompetition() instanceof Match)) {
            return MessageUtil.sendMessage(sender, "&cThat player is not in a game");
        }
        return watch(sender, mp, ((Match) player.getCompetition()).getArena());
    }

    @MCCommand(cmds = {"watch"})
    public boolean watch(ArenaPlayer sender, MatchParams mp, Arena arena) {
        if (!Permissions.hasMatchPerm(sender.getPlayer(), mp, "watch")) {
            return MessageUtil.sendMessage(sender, "&cYou don't have permission to watch a &6" + mp.getCommand());
        }
        if (isDisabled(sender.getPlayer(), mp)) {
            return true;
        }
        if (!canJoin(sender)) {
            return true;
        }
        if (arena.getVisitorLocs() == null) {
            return MessageUtil.sendMessage(sender, ChatColor.YELLOW + "That arena doesnt allow visitors!");
        }
        if (watchController.watch(sender, arena)) {
            return MessageUtil.sendMessage(sender, ChatColor.YELLOW + "You are now watching "
                    + arena.getName() + " /watch leave : to leave");
        }
        return MessageUtil.sendMessage(sender, ChatColor.RED + "You can't watch at this time");
    }

    @MCCommand(cmds = {"create"}, admin = true, perm = "arena.create", usage = "create <arena name>")
    public boolean arenaCreate(Player sender, MatchParams mp, String name) {
        if ( Defaults.DEBUG_COMMANDS ) sender.sendMessage( "arenaCreate Method entered" );
        
        if (arenaController.getArena(name) != null) {
            return MessageUtil.sendMessage(sender, "&cThere is already an arena named &6" + name);
        }
        if (ParamController.getMatchParams(name) != null) {
            return MessageUtil.sendMessage(sender, "&cYou can't choose an arena type as an arena name");
        }
        try {
            int i = Integer.valueOf(name);
            return MessageUtil.sendMessage(sender, "&cYou can't choose a number as the arena name! Arena name was &e" + i);
        } catch (Exception e) {
            /* good, it's not an integer*/
        }
        if (ParamController.getMatchParams(name) != null) {
            return MessageUtil.sendMessage(sender, "&cYou can't choose an arena type as an arena name");
        }

        MatchParams ap = new MatchParams(mp.getType());

        Arena arena = ArenaType.createArena(name, ap, false);
        if (arena == null) {
            return MessageUtil.sendMessage(sender, "&cCouldn't create the arena " + name + " of type " + ap.getType());
        }

        arena.setSpawnLoc(0, 0, new FixedLocation(sender.getLocation()));
        arenaController.addArena(arena);
        ArenaControllerInterface aci = new ArenaControllerInterface(arena);
        aci.create();
        new ArenaCreateEvent(arena).callEvent();
        aci.init();

        MessageUtil.sendMessage(sender, "&2You have created the arena &6" + arena);
        MessageUtil.sendMessage(sender, "&2A spawn point has been created where you are standing");
        MessageUtil.sendMessage(sender, "&2You can add/change spawn points using &6/arena alter "
                                        + arena.getName() + " <1,2,...,x : which spawn>");
        ArenaSerializer.saveArenas(arena.getArenaType().getPlugin());
        return BattleArena.getSelf().getArenaEditorExecutor().arenaSelect(sender, arena);
    }

    @MCCommand(cmds = {"select", "sel"}, admin = true, perm = "arena.alter")
    public boolean arenaSelect(CommandSender sender, Arena arena) {
        try {
            ArenaEditorExecutor aee = BattleArena.getSelf().getArenaEditorExecutor();
            return aee.arenaSelect(sender, arena);
        } catch (IllegalStateException e) {
            return MessageUtil.sendMessage(sender, "&c" + e.getMessage());
        }
    }

    @MCCommand(cmds = {"setArenaOption", "alter", "edit"}, admin = true, perm = "arena.alter")
    public boolean arenaSetOption(CommandSender sender, Arena arena, ArenaOptionPair aop) {
        return ArenaEditorExecutor.setArenaOption(sender, arena, aop);
    }

    @MCCommand(cmds = {"setArenaOption", "alter", "edit"}, admin = true, perm = "arena.alter")
    public boolean arenaSetOption(CommandSender sender, Arena arena, ParamAlterOptionPair gop) {
        return ArenaEditorExecutor.setArenaOption(sender, arena, gop);
    }

    @MCCommand(cmds = {"setArenaOption", "alter", "edit"}, admin = true, perm = "arena.alter")
    public boolean arenaSetOption(CommandSender sender, Arena arena, TransitionOptionTuple top) {
        return ArenaEditorExecutor.setArenaOption(sender, arena, top);
    }

    @MCCommand(cmds = {"setOption"}, admin = true, perm = "arena.alter")
    public boolean setGameOption(CommandSender sender, MatchParams params, ParamAlterOptionPair gop) {
        return _setGameOption(sender, params, null, gop.alterParamOption, gop.value);
    }

    @MCCommand(cmds = {"setOption"}, admin = true, perm = "arena.alter")
    public boolean setGameOption(CommandSender sender, MatchParams params, TeamIndex index, ParamAlterOptionPair gop) {
        return _setGameOption(sender, params, index.getInt(), gop.alterParamOption, gop.value);
    }

    private boolean _setGameOption(CommandSender sender, MatchParams params,
                                    Integer teamIndex, AlterParamOption option, Object value) {
        try {
            ParamAlterController.setGameOption(sender, params, teamIndex, option, value);
            if (value != null) 
                MessageUtil.sendMessage(sender, "&2Game options &6" + option + "&2 changed to &6" + value);
            else 
                MessageUtil.sendMessage(sender, "&2Game options &6" + option + "&2 changed");    
        } 
        catch (InvalidOptionException e) {
            MessageUtil.sendMessage(sender, "&cCould not set game option " + option.name());
            MessageUtil.sendMessage(sender, "&c" + e.getMessage());
        }
        return true;
    }

    @MCCommand(cmds = {"setOption"}, admin = true, perm = "arena.alter")
    public boolean setGameStateOption(CommandSender sender, MatchParams params, TransitionOptionTuple top) {
        return _setGameStateOption(sender, params, null, top.state, top.op, top.value);
    }

    @MCCommand(cmds = {"setOption"}, admin = true, perm = "arena.alter")
    public boolean setGameStateOption(CommandSender sender, MatchParams params, TeamIndex index, TransitionOptionTuple top) {
        return _setGameStateOption(sender, params, index.getInt(), top.state, top.op, top.value);
    }

    private boolean _setGameStateOption(CommandSender sender, MatchParams params, Integer teamIndex,
            CompetitionState state, TransitionOption to, Object value) {
        try {
            ParamAlterController.setGameOption(sender, params, teamIndex, state, to, value);
            if (value != null) {
                MessageUtil.sendMessage(sender, "&2Game options &6" + state + "&2 added &6" + to + " " + value);
            } else {
                MessageUtil.sendMessage(sender, "&2Game options &6" + state + "&2 added &6" + to);
            }
            StateGraph tops = params.getArenaStateGraph();
            StateOptions ops = tops.getOptions(state);
            MessageUtil.sendMessage(sender, "&2Options at &6" + state + "&2 now &6" + ops.toString());
        } catch (InvalidOptionException e) {
            MessageUtil.sendMessage(sender, "&cCould not set game option " + state + " " + to);
            MessageUtil.sendMessage(sender, "&c" + e.getMessage());
        }
        return true;
    }

    @MCCommand(cmds = {"showOptions"}, admin = true, perm = "arena.alter")
    public boolean showGameOptions(CommandSender sender, MatchParams params) {
        MessageUtil.sendMessage(sender, "&2Options for &f" + params.getName() + "&2 : " + params.getDisplayName());
        MessageUtil.sendMessage(sender, params.toSummaryString());
        return MessageUtil.sendMessage(sender, params.getOptionsSummaryString());
    }

    @MCCommand(cmds = {"showOptions"}, admin = true, perm = "arena.alter")
    public boolean showGameOptions(CommandSender sender, Arena arena) {
        MessageUtil.sendMessage(sender, "&2Options for arena &f" + arena.getName() + "&2 : " + arena.getDisplayName());
        MessageUtil.sendMessage(sender, arena.getParams().toSummaryString());
        return MessageUtil.sendMessage(sender, arena.getParams().getOptionsSummaryString());
    }

    @MCCommand(cmds = {"deleteOption"}, admin = true, perm = "arena.alter")
    public boolean deleteOption(CommandSender sender, MatchParams params, String[] args) {
        params = ParamController.getMatchParams(params);
        ParamAlterController pac = new ParamAlterController(params);
        pac.deleteOption(sender, args);
        return true;
    }

    @MCCommand(cmds = {"restoreDefaultArenaOptions"}, admin = true, perm = "arena.alter")
    public boolean restoreDefaultOptions(CommandSender sender, Arena arena) {
        try {
            ArenaAlterController.restoreDefaultArenaOptions(arena, true);
        } catch (IllegalStateException e) {
            return MessageUtil.sendMessage(sender, "&c" + e.getMessage());
        }
        return MessageUtil.sendMessage(sender, "&2Arena &6" + arena.getName() + "set back to default game options");
    }

    @MCCommand(cmds = {"restoreDefaultArenaOptions"}, admin = true, perm = "arena.alter")
    public boolean restoreDefaultOptions(CommandSender sender, MatchParams params) {
        try {
            ArenaAlterController.restoreDefaultArenaOptions(params);
        } catch (IllegalStateException e) {
            return MessageUtil.sendMessage(sender, "&c" + e.getMessage());
        }
        return MessageUtil.sendMessage(sender, "&2Game type &6" + params.getType() + " set back to default game options");
    }

    @MCCommand(cmds = {"rescind"}, helpOrder = 13)
    public boolean duelRescind(ArenaPlayer player) {
        if (!duelController.hasChallenger(player)) {
            return MessageUtil.sendMessage(player, "&cYou haven't challenged anyone!");
        }
        Duel d = duelController.rescind(player);
        ArenaTeam t = d.getChallengerTeam();
        t.sendMessage("&4[Duel] &6" + player.getDisplayName()
                + "&2 has cancelled the duel challenge!");
        for (ArenaPlayer ap : d.getChallengedPlayers()) {
            MessageUtil.sendMessage(ap, "&4[Duel] &6" + player.getDisplayName() + "&2 has cancelled the duel challenge!");
        }
        return true;
    }

    @MCCommand(cmds = {"reject"}, helpOrder = 12)
    public boolean duelReject(ArenaPlayer player) {
        if (!duelController.isChallenged(player)) {
            return MessageUtil.sendMessage(player, "&cYou haven't been invited to a duel!");
        }
        Duel d = duelController.reject(player);
        ArenaTeam t = d.getChallengerTeam();
        String timeRem = TimeUtil.convertSecondsToString(Defaults.DUEL_CHALLENGE_INTERVAL);
        
        t.sendMessage("&4[Duel] &cThe duel was cancelled as &6" + player.getDisplayName() + "&c rejected your offer");
        t.sendMessage("&4[Duel] &cYou can challenge them again in " + timeRem);
        for (ArenaPlayer ap : d.getChallengedPlayers()) {
            if (ap == player) {
                continue;
            }
            MessageUtil.sendMessage( ap, "&4[Duel] &cThe duel was cancelled as &6" + player.getDisplayName() + "&c rejected the duel");
        }
        MessageUtil.sendMessage(player, "&4[Duel] &cYou rejected the duel, you can't be challenged again for&6 " + timeRem);
        return true;
    }

    @MCCommand(cmds = {"accept"}, helpOrder = 11)
    public boolean duelAccept(ArenaPlayer player) {
        if (!canJoin(player)) {
            return true;
        }
        Duel d = duelController.getDuel(player);
        if (d == null) {
            return MessageUtil.sendMessage(player, "&cYou haven't been invited to a duel!");
        }
        Double wager = (Double) d.getDuelOptionValue(DuelOption.MONEY);
        if (wager != null) {
            if (MoneyController.balance(player.getName()) < wager) {
                MessageUtil.sendMessage(player, "&4[Duel] &cYou don't have enough money to accept the wager!");
                
                duelController.cancelFormingDuel(d, "&4[Duel]&6" + player.getDisplayName() + " didn't have enough money for the wager");
                return true;
            }
        }
        for (ArenaPlayer ap : d.getAllPlayers()) {
            if (!d.getOptions().matches(ap, d.getMatchParams())) {
                duelController.cancelFormingDuel( d, "&4[Duel]&6" + player.getDisplayName() + " wasn't within " + 
                            d.getMatchParams().getStateGraph().getOptions(MatchState.PREREQS).getWithinDistance() + 
                            "&c blocks of an arena" );
                return true;
            }
        }
        if (duelController.accept(player) == null) {
            return true;
        }
        d.getChallengerTeam().sendMessage( "&4[Duel] &6" + player.getDisplayName() + "&2 has accepted your duel offer!");
        
        for (ArenaPlayer ap : d.getChallengedPlayers()) {
            if (ap == player) {
                continue;
            }
            MessageUtil.sendMessage(ap, "&4[Duel] &6" + player.getDisplayName() + "&2 has accepted the duel offer");
        }
        return MessageUtil.sendMessage(player, "&cYou have accepted the duel!");
    }

    @MCCommand(cmds = {"duel", "d"})
    public boolean duel(ArenaPlayer player, String args[]) {
        MatchParams mp = ParamController.getMatchParamCopy("duel");
        return mp == null || duel(player, mp, args);
    }

    @MCCommand(cmds = {"duel", "d"}, helpOrder = 10)
    public boolean duel(ArenaPlayer player, final MatchParams mp, String args[]) {
        if (!Permissions.hasMatchPerm(player.getPlayer(), mp, "duel")) {
            return MessageUtil.sendMessage( player, "&cYou don't have permission to duel in a &6" + mp.getCommand());
        }
        if (isDisabled(player.getPlayer(), mp)) {
            return true;
        }

        if (duelController.isChallenged(player)) {
            MessageUtil.sendMessage( player, "&4[Duel] &cYou have already been challenged to a duel!");
            return MessageUtil.sendMessage( player, "&4[Duel] &6/" + mp.getCommand() + 
                                            " reject&c to cancel the duel before starting your own");
        }
        // / Can the player add this match/event at this moment?
        if (!canJoin(player)) {
            return true;
        }
        if (EventController.isEventType(mp.getName())) {
            return MessageUtil.sendMessage(player, "&4[Duel] &cYou can't duel someone in an Event type!");
        }
        // / Parse the duel options
        DuelOptions duelOptions;
        try {
            duelOptions = DuelOptions.parseOptions(mp, player,
                    Arrays.copyOfRange(args, 1, args.length));
        } 
        catch (InvalidOptionException e1) {
            return MessageUtil.sendMessage(player, e1.getMessage());
        }
        Double rake = (Double) duelOptions.getOptionValue(DuelOption.RAKE);
        if (rake != null) {
            if (rake < 0) {
                Double value = 0.0;
                duelOptions.setOptionValue(DuelOption.RAKE, value);
            }
        }
        Double wager = (Double) duelOptions.getOptionValue(DuelOption.MONEY);
        if (wager != null) {
            if (wager >= 0) {
                if (MoneyController.balance(player.getName()) < wager) {
                    return MessageUtil.sendMessage(player, "&4[Duel] You can't afford that wager!");
                }
            } 
            else return MessageUtil.sendMessage(player, "&4[Duel] The wager must be positive");
        }
        if (!duelOptions.matches(player, mp)) {
            return MessageUtil.sendMessage(player, "&cYou need to be within &6" 
                    + mp.getStateGraph().getOptions(MatchState.PREREQS).getWithinDistance() + "&c blocks of an arena");
        }
        // / Announce warnings
        for (ArenaPlayer ap : duelOptions.getChallengedPlayers()) {
            if (!canJoin(ap)) {
                return MessageUtil.sendMessage(player, "&4[Duel] &6" + ap.getDisplayName() + "&c is in a match, event, or queue");
            }
            if (!duelOptions.matches(ap, mp)) {
                return MessageUtil.sendMessage(
                        player,
                        "&6"
                        + ap.getDisplayName()
                        + "&c needs to be within "
                        + mp.getStateGraph()
                        .getOptions(MatchState.PREREQS)
                        .getWithinDistance()
                        + "&c blocks of an arena");
            }

            final StateGraph ops = mp.getStateGraph();
            if (ops != null) {
                ArenaTeam t = TeamController.createTeam(mp, ap);
                // / Check ready
                if (!ops.teamReady(t, null)) {
                    MessageUtil.sendMessage(player, "&c" + t.getDisplayName() + "&c doesn't have the prerequisites for this duel");
                    return true;
                }
            }

            if (duelController.isChallenged(ap)) {
                return MessageUtil.sendMessage(player, "&4[Duel] &6" + ap.getDisplayName() + "&c already has been challenged!");
            }
            if (!Permissions.hasMatchPerm(ap.getPlayer(), mp, "duel")) {
                return MessageUtil.sendMessage(player, "&6" + ap.getDisplayName()
                        + "&c doesn't have permission to duel in a &6" + mp.getCommand());
            }

            Long grace = duelController.getLastRejectTime(ap);
            if (grace != null
                    && System.currentTimeMillis() - grace < Defaults.DUEL_CHALLENGE_INTERVAL * 1000) {
                return MessageUtil.sendMessage(
                        player,
                        "&4[Duel] &6"
                        + ap.getDisplayName()
                        + "&c can't be challenged for &6"
                        + TimeUtil
                        .convertMillisToString(Defaults.DUEL_CHALLENGE_INTERVAL
                                * 1000
                                - (System.currentTimeMillis() - grace)));
            }
            if (wager != null) {
                if (MoneyController.balance(ap.getName()) < wager) {
                    return MessageUtil.sendMessage(player, "&4[Duel] &6" + ap.getDisplayName() + "&c can't afford that wager!");
                }
            }
        }

        // / Get our team1
        ArenaTeam t = TeamController.getTeam(player);
        if (t == null) {
            t = TeamFactory.createCompositeTeam(0, mp, player);
        } else {
            t.setIndex(0);
        }
        for (ArenaPlayer ap : t.getPlayers()) {
            if (!duelOptions.matches(ap, mp)) {
                return MessageUtil.sendMessage(
                        player,
                        "&6" + ap.getDisplayName() + "&c needs to be within "
                        + mp.getStateGraph().getOptions(MatchState.PREREQS)
                        .getWithinDistance());
            }

            if (wager != null) {
                if (MoneyController.balance(ap.getName()) < wager) {
                    return MessageUtil.sendMessage(player,
                            "&4[Duel] Your teammate &6" + ap.getDisplayName()
                            + "&c can't afford that wager!");
                }
            }
        }

        mp.setNTeams(new MinMax(2));

        int size = duelOptions.getChallengedPlayers().size();
        mp.setMinTeamSize(Math.min(t.size(), size));
        mp.setMaxTeamSize(Math.max(t.size(), size));
        // / set our default rating
        mp.setRated(Defaults.DUEL_ALLOW_RATED && mp.isRated());
        // / allow specified options to overrule
        if (duelOptions.hasOption(DuelOption.RATED)) {
            mp.setRated(true);
        } else if (duelOptions.hasOption(DuelOption.UNRATED)) {
            mp.setRated(false);
        }

        // / Check to make sure at least one arena can be joined at some time
        Arena arena = arenaController.getArenaByMatchParams(mp);
        if (arena == null) {
            Map<Arena, List<String>> reasons = arenaController.getNotMachingArenaReasons(mp);
            if (!reasons.isEmpty()) {
                for (Arena a : reasons.keySet()) {
                    List<String> rs = reasons.get(a);
                    if (!rs.isEmpty()) {
                        return MessageUtil.sendMessage(player, "&c" + rs.get(0));
                    }
                }
            }
            return MessageUtil.sendSystemMessage(player, "valid_arena_not_built", mp.getName());
        }
        final StateGraph ops = mp.getStateGraph();
        if (ops == null) {
            return MessageUtil.sendMessage(player, "&cThis match type has no valid options, contact an admin to fix ");
        }

        Duel duel = new Duel(mp, t, duelOptions);

        // / Announce to the 2nd team
        String t2 = duelOptions.getChallengedTeamString();
        for (ArenaPlayer ap : duelOptions.getChallengedPlayers()) {
            String other = duelOptions.getOtherChallengedString(ap);
            if (!other.isEmpty()) {
                other = "and " + other + " ";
            }
            MessageUtil.sendMessage( ap, "&4[" + mp.getName() + " Duel] &6" + t.getDisplayName() + "&2 " + 
                    MessageUtil.hasOrHave(t.size()) + " challenged you " + other + "to a &6" + mp.getName() + " &2duel!");
            
            MessageUtil.sendMessage( ap, "&4[Duel] &2Options: &6" + duelOptions.optionsString(mp) );
            MessageUtil.sendMessage( ap, "&4[Duel] &6/" + mp.getCommand() + " accept &2: to accept. &6" + 
                    mp.getCommand() + " reject &e: to reject");
        }

        MessageUtil.sendMessage( player, "&4[Duel] &2You have sent a challenge to &6" + t2);
        MessageUtil.sendMessage( player, "&4[Duel] &2You can rescind by typing &6/" + mp.getCommand() + " rescind");
        duelController.addOutstandingDuel(duel);
        return true;
    }

    @MCCommand(cmds = {"start"}, admin = true, perm = "arena.start")
    public boolean arenaStart(CommandSender sender, MatchParams mp) {
        List<Match> matches = arenaController.getRunningMatches(mp);
        if (matches.isEmpty()) {
            return MessageUtil.sendMessage(sender, "&cThere are no open &6" + mp.getType());
        } else if (matches.size() > 1) {
            return MessageUtil.sendMessage(sender, "&cThere are multiple &6" + mp.getType()
                    + "&c open.  Specify which arena.\n&e/" + mp.getCommand()
                    + " cancel <arena>");
        }
        arenaController.startMatch(matches.get(0));
        return MessageUtil.sendMessage(sender, "&2" + mp.getType() + " has been started");
    }

    @MCCommand(cmds = {"forceStart"}, admin = true, perm = "arena.forcestart")
    public boolean arenaForceStart(CommandSender sender, MatchParams mp) {
        if (arenaController.forceStart(mp, false)) {
            return MessageUtil.sendMessage(sender, "&2" + mp.getType() + " has been started");
        }
        return MessageUtil.sendMessage(sender, "&c" + mp.getType() + " could not be started");
    }

    @MCCommand(cmds = {"choose", "class"})
    public boolean chooseClass(ArenaPlayer sender, String arenaClass) {
        ArenaClass ac = ArenaClassController.getClass(arenaClass);
        if (ac == null) {
            return MessageUtil.sendMessage(sender, "&cThere is no class called &6" + arenaClass);
        }
        if (sender.getCurLocation().getType() == LocationType.HOME) {
            return MessageUtil.sendMessage(sender, "&cYou aren't in a game&6");
        }
        ArenaClassController.changeClass(sender.getPlayer(), sender.getCompetition(), ac);
        return true;
    }

    @MCCommand(cmds = {"list"})
    public boolean arenaList(CommandSender sender, MatchParams mp, String[] args) {
        boolean all = args.length > 1 && (args[1]).equals("all");

        Collection<Arena> arenas = arenaController.getArenas().values();
        HashMap<ArenaType, Collection<Arena>> arenasbytype = new HashMap<>();
        for (Arena arena : arenas) {
            Collection<Arena> as = arenasbytype.get(arena.getArenaType());
            if (as == null) {
                as = new ArrayList<>();
                arenasbytype.put(arena.getArenaType(), as);
            }
            as.add(arena);
        }
        if (arenasbytype.isEmpty()) {
            MessageUtil.sendMessage(sender, "&cThere are no &6" + mp.getName()
                    + "&c arenas");
        }
        for (ArenaType at : arenasbytype.keySet()) {
            if (!all && !at.matches(mp.getType())) {
                continue;
            }
            Collection<Arena> as = arenasbytype.get(at);
            if (!as.isEmpty()) {
                MessageUtil.sendMessage(sender, "&e------ Arenas for &6" + at.toString()
                        + "&e ------");
                for (Arena arena : as) {
                    MessageUtil.sendMessage(sender, arena.toSummaryString());
                }
            }
        }
        if (!all) {
            MessageUtil.sendMessage(sender, "&6/arena list all&e: to see all arenas");
        }
        return MessageUtil.sendMessage(sender, "&6/arena info <arenaname>&e: for details on an arena");
    }

    public boolean canJoin(ArenaTeam t) {
        return canJoin(t, true);
    }

    public boolean canJoin(ArenaTeam t, boolean showMessages) {
        for (ArenaPlayer ap : t.getPlayers()) {
            if (!_canJoin(ap, showMessages, true)) {
                return false;
            }
        }
        return true;
    }

    public boolean canJoin(ArenaPlayer player) {
        return canJoin(player, true);
    }

    public boolean canJoin(ArenaPlayer player, boolean showMessages) {
        return _canJoin(player, showMessages, false);
    }

    private boolean _canJoin(ArenaPlayer player, boolean showMessages, boolean teammate) {
        /// Check for any competition
        if (player.getCompetition() != null) {
            if (showMessages) {
                MessageUtil.sendMessage(player, "&cYou are still in the " + player.getCompetition().getName() + ". &6/arena leave");
            }
            return false;
        }
        /// Inside the queue waiting for a match?
        if (InArenaListener.inQueue(player.getUniqueId())) {
            MessageUtil.sendMessage(player, "&eYou are in the queue.");
            if (showMessages) {
                MessageUtil.sendMessage(player, "&eType &6/arena leave");
            }
            return false;
        }

        /// Inside MobArena?
        if ( MobArenaUtil.isEnabled()
                && MobArenaUtil.insideMobArena( player.getPlayer() ) ) {
            if (showMessages) {
                MessageUtil.sendMessage(player, "&cYou need to finish with MobArena first!");
            }
            return false;
        }

        /// Check for player in combat
        if (CombatTagUtil.isTagged(player.getPlayer())
                || (HeroesController.enabled() && HeroesController.isInCombat(player.getPlayer()))) {
            if (showMessages) {
                MessageUtil.sendMessage(player, "&cYou are in combat!");
            }
            return false;
        }

        /// Inside an Event?
        AbstractComp ae = EventController.insideEvent(player);
        if (ae != null) {
            if (showMessages) {
                MessageUtil.sendMessage(player, "&eYou need to leave the Event first. &6/" + ae.getCommand() + " leave");
            }
            return false;
        }

        // / Inside a match?
        Match am = arenaController.getMatch(player);
        if (am != null) {
            ArenaTeam t = am.getTeam(player);
            if (am.isHandled(player)
                    || (!t.hasLeft(player) && t.hasAliveMember(player))) {
                if (showMessages) {
                    MessageUtil.sendMessage(player, "&eYou are already in a match.");
                }
                return false;
            }
            return true;
        }
        if (!teammate) {
            // / Inside a forming team?
            if (teamController.inFormingTeam(player)) {
                FormingTeam ft = teamController.getFormingTeam(player);
                if (ft.isJoining(player)) {
                    if (showMessages) {
                        MessageUtil.sendMessage(player, "&eYou have been invited to the team. " + ft.getDisplayName());
                    }
                    if (showMessages) {
                        MessageUtil.sendMessage(player, "&eType &6/team add|decline");
                    }
                } else if (!ft.hasAllPlayers()) {
                    if (showMessages) {
                        MessageUtil.sendMessage(player, "&eYour team is not yet formed. &6/team disband&e to leave");
                    }
                    if (showMessages) {
                        MessageUtil.sendMessage(player, "&eYou are still missing " + MessageUtil.joinPlayers(
                                ft.getUnjoinedPlayers(), ", ") + " !!");
                    }
                }
                return false;
            }
            // / Make a team for the new Player
            ArenaTeam t = teamController.getSelfFormedTeam(player);
            if (t != null) {
                for (ArenaPlayer p : t.getPlayers()) {
                    if (p == player) {
                        continue;
                    }
                    if (!_canJoin(p, true, true)) {
                        MessageUtil.sendSystemMessage(player, "teammate_cant_join");
                        MessageUtil.sendMessage(player, "&6/team leave: &cto leave the team");
                        return false;
                    }
                }
            }
        }

        if (duelController.hasChallenger(player)) {
            if (showMessages) {
                MessageUtil.sendMessage(player, "&cYou need to rescind your challenge first! &6/arena rescind");
            }
            return false;
        }

        if ( EssentialsUtil.isEnabled()
                && EssentialsUtil.inJail(player)) {
            if (showMessages) {
                MessageUtil.sendMessage(player, "&cYou are still in jail!");
            }
            return false;
        }
        return true;
    }

    public boolean checkAndRemoveFee(MatchParams pi, ArenaTeam t) {
        boolean takesFee = pi.hasEntranceFee();
        boolean needsItems = pi.hasOptionAt(MatchState.PREREQS, TransitionOption.NEEDITEMS);
        boolean takesItems = pi.hasOptionAt(MatchState.PREREQS, TransitionOption.TAKEITEMS);
        if (takesFee) {
            Double fee = pi.getEntranceFee();
            if (fee != null) {

                boolean hasEnough = true;
                for (ArenaPlayer player : t.getPlayers()) {
                    boolean has = MoneyController.hasEnough(player.getName(), fee);
                    hasEnough &= has;
                    if (!has) {
                        MessageUtil.sendMessage(player, "&eYou need &6" + fee + "&e to compete");
                    }
                }
                if (!hasEnough) {
                    if (t.size() > 1) {
                        t.sendMessage("&eYour team does not have enough money to compete");
                    }
                    return false;
                }
            }
        }
        if (needsItems) {
            List<ItemStack> fee = pi.getStateGraph().getNeedItems(MatchState.PREREQS);
            if (fee != null) {
                boolean hasEnough = true;

                for (ArenaPlayer player : t.getPlayers()) {
                    boolean has = InventoryUtil.hasAllItems(player.getPlayer(), fee);
                    hasEnough &= has;
                    if (!has) {
                        MessageUtil.sendMessage(player, "&eYou don't have all the needed items to compete");
                        for (ItemStack is : fee) {
                            MessageUtil.sendMessage(player, "&c- &e" + InventoryUtil.getItemString(is));
                        }
                    }
                }
                if (!hasEnough) {
                    if (t.size() > 1) {
                        t.sendMessage("&eYour team does not have all the items to compete");
                    }
                    return false;
                }
            }
        }
        if (takesItems) {
            List<ItemStack> fee = pi.getStateGraph().getTakeItems(MatchState.PREREQS);
            if (fee != null) {
                boolean hasEnough = true;

                for (ArenaPlayer player : t.getPlayers()) {
                    boolean has = InventoryUtil.hasAllItems(player.getPlayer(), fee);
                    hasEnough &= has;
                    if (!has) {
                        MessageUtil.sendMessage(player, "&eYou don't have all the needed items to compete");
                        for (ItemStack is : fee) {
                            MessageUtil.sendMessage(player, "&c- &e" + InventoryUtil.getItemString(is));
                        }
                    }
                }
                if (!hasEnough) {
                    if (t.size() > 1) {
                        t.sendMessage("&eYour team does not have all the items to compete");
                    }
                    return false;
                }
            }
        }
        /// Take the requirements

        if (takesFee) {
            Double fee = pi.getEntranceFee();
            if (fee != null) {
                for (ArenaPlayer player : t.getPlayers()) {
                    getOrCreateJoinReqs(player).setMoney(fee);
                    MoneyController.subtract(player.getName(), fee);
                    MessageUtil.sendMessage(player, "&6" + fee + " has been subtracted from your account");
                }
            }
        }
        if (takesItems) {
            List<ItemStack> fee = pi.getStateGraph().getTakeItems(MatchState.PREREQS);
            if (fee != null) {
                for (ArenaPlayer player : t.getPlayers()) {
                    getOrCreateJoinReqs(player).setItems(new PInv(fee));
                    InventoryUtil.removeItems(player.getInventory(), fee);
                }
            }
        }
        return true;
    }

    private PlayerSave getOrCreateJoinReqs(ArenaPlayer player) {
        PlayerSave ps = player.getMetaData().getJoinRequirements();
        if (ps == null) {
            ps = new PlayerSave(player);
            player.getMetaData().setJoinRequirements(ps);
        }
        return ps;
    }

    public static boolean refundFee(MatchParams pi, ArenaTeam t) {
        final StateGraph tops = pi.getStateGraph();
        if (tops.hasEntranceFee()) {
            Double fee = tops.getEntranceFee();
            if (fee == null || fee <= 0) {
                return true;
            }
            for (ArenaPlayer player : t.getPlayers()) {
                MoneyController.add(player.getName(), fee);
                MessageUtil.sendMessage(player,
                        "&eYou have been refunded the entrance fee of &6" + fee);
            }
        }
        return true;
    }

    protected Arena getArena(String name) { return arenaController.getArena(name); }

    public static boolean checkPlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendMessage(sender, "&cYou need to be online for this command!");
            return false;
        }
        return true;
    }

    public void setDisabled(List<String> _disabled) { disabled.addAll(_disabled); }
}
