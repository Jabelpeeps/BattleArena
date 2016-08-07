package mc.alk.util.handlers;

import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

public interface ISignHandler {

    void sendLines(Player player, Sign sign, String[] lines);

    public static final ISignHandler NULL_HANDLER = new ISignHandler() {

        @Override
        public void sendLines(Player player, Sign sign, String[] lines) {
            // do nothing
        }
    };

}
