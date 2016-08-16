package mc.alk.arena.objects.signs;

import org.bukkit.Location;

import mc.alk.arena.BattleArena;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchParams;

/**
 * @author alkarin
 */
class ArenaLeaveSign extends ArenaCommandSign{

    ArenaLeaveSign(Location location, MatchParams mp, String[] op1, String[] op2) {
        super(location, mp, op1, op2);
    }

    @Override
    public void performAction(ArenaPlayer player) {
        BattleArena.getBAExecutor().leave(player, matchParams);
    }

    @Override
    public String getCommand() {
        return "Leave";
    }
}
