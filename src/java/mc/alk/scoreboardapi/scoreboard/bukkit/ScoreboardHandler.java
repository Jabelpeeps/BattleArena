//package mc.alk.scoreboardapi.scoreboard.bukkit;
//
//import java.lang.reflect.Field;
//
//import org.bukkit.Bukkit;
//import org.bukkit.OfflinePlayer;
//import org.bukkit.craftbukkit.v1_7_R2.entity.CraftPlayer;
//import org.bukkit.entity.Player;
//import org.bukkit.scoreboard.Objective;
//
//import mc.alk.scoreboardapi.api.STeam;
//import net.minecraft.server.v1_7_R2.PacketPlayOutScoreboardObjective;
//import net.minecraft.server.v1_7_R2.ScoreboardObjective;
//
///**
// * @author alkarin
// */
//public class ScoreboardHandler {
//
//    public void setDisplayName(Objective o, STeam team, String display) {
//        if (display == null || o == null)
//            return;
//        Class<?> c = o.getClass();
//        try {
//            Field f = c.getDeclaredField("objective");
//            f.setAccessible(true);
//            Object ob = f.get(o);
//            PacketPlayOutScoreboardObjective pposo = new PacketPlayOutScoreboardObjective((ScoreboardObjective) ob, 2);
//            f = pposo.getClass().getDeclaredField("b");
//            f.setAccessible(true);
//            f.set(pposo, display);
//            for (OfflinePlayer op : team.getPlayers()) {
//                if (op.isOnline()) {
//                    Player p = Bukkit.getPlayerExact(op.getName());
//                    if (p == null || !p.getScoreboard().equals(o.getScoreboard()))
//                        continue;
//                    ((CraftPlayer) p).getHandle().playerConnection.sendPacket(pposo);
//                }
//            }
//        } catch (Throwable e) {
//            e.printStackTrace();
//        }
//
//    }
//}
