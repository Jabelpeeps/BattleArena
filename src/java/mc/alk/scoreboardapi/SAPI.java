package mc.alk.scoreboardapi;

/**
 * @author alkarin
 */
public class SAPI {

    /** How many entries can be shown on the objectives (bukkit max is 15) */
    public static final int MAX_ENTRIES = 15;

    /** Maximum id size (Bukkit only supports up to 16 for scoreboards) */
    public static final int MAX_NAMESIZE = 16;

    /** Maximum size for objective display names*/
    public static final int MAX_OBJECTIVE_DISPLAYNAME_SIZE = 32;

    /** Maximum criteria size for objectives */
    public static final int MAX_OBJECTIVE_CRITERIA_SIZE = 32;

    public static boolean hasBukkitScoreboard() {
        return SAPIFactory.hasBukkitScoreboard();
    }

}
