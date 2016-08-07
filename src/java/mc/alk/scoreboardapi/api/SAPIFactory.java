package mc.alk.scoreboardapi.api;

import mc.alk.scoreboardapi.scoreboard.SAPIDisplaySlot;
import mc.alk.scoreboardapi.scoreboard.SAPIObjective;
import mc.alk.scoreboardapi.scoreboard.SAPIScoreboard;
import mc.alk.scoreboardapi.scoreboard.bukkit.BObjective;
import mc.alk.scoreboardapi.scoreboard.bukkit.BScoreboard;
import org.bukkit.plugin.Plugin;

/**
 * @author alkarin
 */
public class SAPIFactory {

    private static boolean hasBukkitScoreboard = false;

    static{
        try {
            Class.forName("org.bukkit.scoreboard.Scoreboard");
            hasBukkitScoreboard = true;
        } catch (ClassNotFoundException e) {
            hasBukkitScoreboard = false;
        }
    }

    /**
     * Create a new Objective
     * @param id id of the Objective
     * @param displayName how to display the Objective
     * @param criteria What is the criteria
     * @param slot Which display slot to use [SIDEBAR, PLAYER_LIST, BELOW_NAME]
     * @param priority lower means this Objective will be shown in the same slot over another with higher priority
     * @return The new Objective
     */
    public static SAPIObjective createObjective(String id, String displayName, String criteria,
                                                SAPIDisplaySlot slot, int priority) {
        SAPIObjective o = hasBukkitScoreboard ? new BObjective(id,displayName,criteria,priority) :
                new SAPIObjective(id,displayName,criteria,priority);
        o.setDisplaySlot(slot);
        return o;
    }

    /**
     * Create a new SAPIObjective
     * @param id id of the Objective
     * @param displayName how to display the Objective
     * @param criteria What is the criteria
     * @param slot Which display slot to use [SIDEBAR, PLAYER_LIST, BELOW_NAME]
     * @param priority lower means this Objective will be shown in the same slot over another with higher priority
     * @return The new SAPIObjective
     */
    public static SAPIObjective createSAPIObjective(String id, String displayName, String criteria,
                                                    SAPIDisplaySlot slot, int priority) {
        SAPIObjective o =  new SAPIObjective(id,displayName,criteria,priority);
        o.setDisplaySlot(slot);
        return o;
    }

    public static SScoreboard createScoreboard(Plugin plugin, String name) {
        return hasBukkitScoreboard ? new BScoreboard(plugin, name) :  new SAPIScoreboard(plugin, name);
    }

    public static SScoreboard createSAPIScoreboard(Plugin plugin, String name) {
        return new SAPIScoreboard(plugin, name);
    }

    public static boolean hasBukkitScoreboard() {
        return hasBukkitScoreboard;
    }

    public static void transferOldScoreboards(SScoreboard oldScoreboard, SScoreboard newScoreboard) {
        if (!hasBukkitScoreboard) {
            return;
        }
        if (!(oldScoreboard instanceof BScoreboard) || !(newScoreboard instanceof BScoreboard)) {
            return;
        }
        ((BScoreboard)newScoreboard).transferOldScoreboards((BScoreboard)oldScoreboard);
    }

}
