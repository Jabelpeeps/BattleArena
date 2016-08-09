package mc.alk.util;

import org.bukkit.entity.Player;

/**
 * Never use player.getTotalExperience() because it can return an incorrect
 * value because Bukkit does not update experience after spending it on
 * enchants.
 *
 * @author alkarin version: 1.4
 *
 * Levels based off of 1.3 exp formulas verified by myself. Formulas used. lvl
 * <= 15 : 17*lvl; 15 < lvl < 31 : 17*l + 3*(0.5*l2*(l2+1)), and 17 + 3*l2,
 * where l2 = (l - 16) lvl > 30 : 17*l + 3*(0.5*l2*(l2+1))+4*(0.5*l3*(l3+1)) and
 * 17+inc, 17 + 3*l2 +4*l3, where l2 = (l-16) and l3=(l-31)
 *
 * The forms you see in the functions are simplifications of the above
 */
public class ExpUtil {

    /**
     * Never use player.getTotalExperience(), use this method instead.
     * player.getTotalExperience() shows XP that has been spent on enchants.
     */
    public static int getTotalExperience(final Player player) {
        int exp = Math.round(getExpAtLevel(player.getLevel()) * player.getExp());
        int currentLevel = player.getLevel();

        while (currentLevel > 0) {
            currentLevel--;
            exp += getExpAtLevel(currentLevel);
        }
        if (exp < 0) {
            exp = Integer.MAX_VALUE;
        }
        return exp;
    }

    /**
     * New XP math for Minecraft 1.8 Credits go to andrewkm for the 1.8 code
     */
    public static int getExpAtLevel(final int level) {
        if (level <= 15) {
            return (2 * level) + 7;
        }
        if ((level >= 16) && (level <= 30)) {
            return (5 * level) - 38;
        }
        return (9 * level) - 158;
    }

    /**
     * Set the total amount of experience for a player
     *
     * @param player
     * @param exp
     */
    public static void clearExperience(Player player) {
        setTotalExperience(player, 0);
    }

    /**
     * Set the total amount of experience for a player
     *
     * @param player
     * @param exp
     */
    public static void setTotalExperience(Player player, int exp) {
        player.setTotalExperience(0);
        player.setLevel(0);
        player.setExp(0);
        if (exp > 0) {
            player.giveExp(exp);
        }
    }

    /**
     * Set the level of a player
     *
     * @param player
     * @param level
     */
    public static void setLevel(Player player, int level) {
        player.setTotalExperience(0);
        player.setLevel(0);
        player.setExp(0);
        player.setLevel(level);
    }

    /**
     * Give experience to a player
     *
     * @param player
     * @param exp
     */
    public static void giveExperience(Player player, int exp) {
        final int currentExp = getTotalExperience(player);
        player.setTotalExperience(0);
        player.setLevel(0);
        player.setExp(0);
        final int newexp = currentExp + exp;
        if (newexp > 0) {
            player.giveExp(newexp);
        }
    }
}
