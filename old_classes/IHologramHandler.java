//package mc.alk.util.handlers;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import org.bukkit.Location;
//
//import mc.alk.arena.objects.Hologram;
//
//public interface IHologramHandler {
//
//    public boolean destroyHologram(Hologram hologram);
//
//    public List<Integer> showLine(Location location, String text);
//
//    public static final IHologramHandler NULL_HANDLER = new IHologramHandler() {
//
//        @Override
//        public boolean destroyHologram(Hologram hologram) {
//            return true;
//        }
//
//        @Override
//        public List<Integer> showLine(Location location, String text) {
//            return new ArrayList<Integer>();
//        }
//    };
//}
