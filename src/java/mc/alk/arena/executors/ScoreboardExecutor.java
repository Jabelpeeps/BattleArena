package mc.alk.arena.executors;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import mc.alk.arena.competition.Match;
import mc.alk.arena.controllers.BattleArenaController;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.objects.scoreboard.ArenaObjective;
import mc.alk.arena.objects.scoreboard.ArenaScoreboard;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.scoreboardapi.SAPIDisplaySlot;
import mc.alk.util.MessageUtil;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

/**
 * Command executor used to modify the Scoreboard of an arena that is in
 * progress. <br/><br/>
 *
 * <pre>
 * Use-case scenario: Can be used with VariableTriggers plugin and scripts to create custom arenas.
 *
 * Command syntax examples:
 * /sb create {arena} {sbLabel} {amountToWin}
 * /sb inc {ArenaPlayer}
 * /sb add {ArenaPlayer} {amount}
 * /sb minus {ArenaPlayer} {amount}
 * /sb dec {ArenaPlayer}
 * /sb undo
 * /sb debug
 * </pre>
 *
 * @author Nikolai
 */
public class ScoreboardExecutor extends CustomCommandExecutor {

    Plugin plugin;
    protected final BattleArenaController bac;
    boolean MESSAGES = false;
    /**
     * key = matchID
     */
    Map<Integer, ArenaObjective> objectives = new HashMap<Integer, ArenaObjective>(); // key = matchID
    /**
     * key = matchID, value is the amount of points needed to win the arena.
     */
    Map<Integer, Integer> amount2win = new HashMap<Integer, Integer>(); // key = matchID
    /**
     * Stack of inverse Commands.
     */
    LinkedList<String> stack = new LinkedList<String>();
    String lastOperation = "";

    public ScoreboardExecutor(Plugin reference, BattleArenaController controller, boolean messages) {
        this.plugin = reference;
        this.bac = controller;
        this.MESSAGES = messages;
    }

    /**
     *
     * @param sender This command can be run as a player or as the console.
     * @param arena Create the scoreboard for which arena ?
     * @param label Scoreboard Label
     * @param amountToWin How many points does a team need to win ?
     * @return
     */
    @MCCommand(cmds = {"create"}, op = true)
    public boolean create(CommandSender sender, Arena arena, String label, Integer amountToWin) {
        Match match = this.bac.getMatch(arena);
        int matchID = match.getID();
        if (objectives.containsKey(matchID)) {
            printMessage(sender, "You may only have one custom objective per match.");
            String name = objectives.get(matchID).getID();
            String msg = "Objective " + name + " will be over written";
            printMessage(sender, msg);
        }
        ArenaObjective objective = new ArenaObjective(label, label, label, SAPIDisplaySlot.SIDEBAR, 60);
        ArenaScoreboard scoreboard = match.getScoreboard();
        objective.setScoreBoard(scoreboard);
        scoreboard.addObjective(objective);

        objectives.put(matchID, objective);
        amount2win.put(matchID, amountToWin);
        printMessage(sender, "Objective " + label + " created!");
        printMessage(sender, "Teams must reach " + amountToWin + " points to win");
        
        String inverseCmd = "sb unregister " + arena.getName();
        saveInverseCommand(inverseCmd);
        return true;
    }
    
    /**
     * This command is only partially implemented.
     * @param sender
     * @param arena
     * @return 
     */
    @MCCommand(cmds = {"remove", "delete", "unregister"}, op = true)
    public boolean remove(CommandSender sender, Arena arena) {
        Match match = this.bac.getMatch(arena);
        int matchID = match.getID();
        ArenaObjective objective = objectives.get(matchID);
        if (objective == null) {
            printMessage(sender, "This command will only unregister a custom Objective created by the /arenaScoreboard cmd");
            printMessage(sender, "You have to create an objective before you can remove it");
            return true;
        }
        // objective.unregister();
        objectives.remove(matchID);
        amount2win.remove(matchID);
        printMessage(sender, "The amount needed to win has been removed from this arena");
        return true;
    }

    /**
     * Add X amount.
     */
    @MCCommand(cmds = {"addpoints", "add"}, op = true)
    public boolean add(CommandSender sender, ArenaPlayer ap, Integer x) {
        Match match = bac.getMatch(ap);
        int matchID = match.getID();
        ArenaTeam team = ap.getTeam();
        ArenaObjective objective = objectives.get(matchID);
        objective.addPoints(team, x);

        int max = amount2win.get(matchID);
        int points = objective.getPoints(team);

        printMessage(sender, "" + x + " points added to " + team.getDisplayName());
        printMessage(sender, "" + team.getDisplayName() + " has " + points + " points");
        if (points >= max) {
            printMessage(sender, "" + team.getDisplayName() + " was declared the winner!");
            match.setVictor(team);
            stack.clear();
            return true;
        }
        String inverseCmd = "sb minus " + ap.getName() + " " + x.toString();
        saveInverseCommand(inverseCmd);
        return true;
    }

    /**
     * Increment by one.
     */
    @MCCommand(cmds = {"addpoint", "increment", "increase", "inc"}, op = true)
    public boolean increment(CommandSender sender, ArenaPlayer ap) {
        return add(sender, ap, 1);
    }

    /**
     * Decrement by one.
     */
    @MCCommand(cmds = {"removepoint", "decrement", "decrease", "dec"}, op = true)
    public boolean decrement(CommandSender sender, ArenaPlayer ap) {
        return minus(sender, ap, 1);
    }

    /**
     * Minus X amount.
     */
    @MCCommand(cmds = {"minus", "subtract", "removepoints"}, op = true)
    public boolean minus(CommandSender sender, ArenaPlayer ap, Integer x) {
        int neg = x * -1;
        return add(sender, ap, neg);
    }
    
    /**
     * Cmd to toggle between verbose and quiet modes. <br/>
     */
    @MCCommand(cmds={"toggle"}, op=true)
    public boolean toggle(CommandSender sender) {
        if (MESSAGES) {
            MESSAGES = false;
        } else {
            MESSAGES = true;
        }
        return true;
    }
    /**
     * In verbose mode, messages from this executor will be displayed. <br/>
     */
    @MCCommand(cmds={"verbose", "v"}, op=true)
    public boolean verbose(CommandSender sender) {
        MESSAGES = true;
        printMessage(sender, "The ArenaScoreboard command is now in verbose mode.");
        printMessage(sender, "All messages from this command will now be displayed.");
        return true;
    }
    
    /**
     * In quiet mode, messages from this executor will NOT be displayed. <br/>
     * 
     * This was a requested feature because apparently, this cmd was spamming the console.
     */
    @MCCommand(cmds={"quiet", "q"}, op=true)
    public boolean quiet(CommandSender sender) {
        MESSAGES = false;
        sender.sendMessage("The ArenaScoreboard command is now in quiet mode.");
        sender.sendMessage("All messages from this command will now be hidden");
        return true;
    }
    
    private boolean printMessage(CommandSender sender, String msg) {
        if (MESSAGES) {
            MessageUtil.sendMessage(sender, msg);
        }
        return true;
    }

    /**
     * Undo the last '/sb' command by the sender. <br/><br/>
     *
     * I'm not sure there's an actual use-case-scenario for undo... 
     * I just always wanted to write an undo command :P
     */
    @MCCommand(cmds = {"undo"}, op = true)
    public boolean undo(CommandSender sender) {
        String cmd = getLastCommand();
        if (cmd.equals("")) {
            printMessage(sender, "There is nothing to undo");
        } else {
            Bukkit.dispatchCommand(sender, cmd);
            printMessage(sender, "Undo successful: You may undo " + stack.size() + " more times.");
        }
        return true;
    }

    /**
     * Do not push() something on the stack if the last operation was undo.
     */
    private void saveInverseCommand(String inverseCmd) {
        if (stack.size() >= 10) {
            stack.removeLast();
        }
        if (!lastOperation.equals("undo")) {
            stack.push(inverseCmd);
        } else {
            lastOperation = "";
        }
    }

    /**
     * The only reason to pop() something off the stack is to do an undo
     * operation.
     */
    private String getLastCommand() {
        lastOperation = "undo";
        String lastCmd = "";
        if (!stack.isEmpty()) {
            lastCmd = stack.pop();
        }
        return lastCmd;
    }

}
