package mc.alk.util;

import mc.alk.util.factory.SignHandlerFactory;
import mc.alk.util.handlers.ISignHandler;

import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

public class SignUtil {

    static ISignHandler handler = SignHandlerFactory.getNewInstance();

    public static void sendLines(Player player, Sign sign, String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].length() > 15) {
                lines[i] = lines[i].substring(0, 15);
            }
        }
        handler.sendLines(player, sign, lines);
    }
}
