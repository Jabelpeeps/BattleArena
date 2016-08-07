//package mc.alk.util;
//
//import mc.alk.arena.objects.Hologram;
//import mc.alk.util.factory.HologramHandlerFactory;
//import mc.alk.util.handlers.IHologramHandler;
//
//import org.bukkit.Location;
//
//public class HologramUtil {
//
//    private static IHologramHandler handler = HologramHandlerFactory.getNewInstance();
//
//    public static void sendHologram(Hologram hologram) {
//        double yOffset = (hologram.getLines().size() / 2) * hologram.getDistanceBetweenLines();
//        Location first = hologram
//                .getLocation()
//                .clone()
//                .add(0, yOffset, 0);
//        for (int i = 0; i < hologram.getLines().size(); i++) {
//            hologram.getIds()
//                    .addAll(handler.showLine(first.clone(), hologram.getLines()
//                                    .get(i)));
//            first.subtract(0, hologram.getDistanceBetweenLines(), 0);
//        }
//        hologram.setShowing(true);
//    }
//
//    public static void changeHologram(Hologram hologram) {
//        destroyHologram(hologram);
//        sendHologram(hologram);
//    }
//
//    public static boolean destroyHologram(Hologram hologram) {
//        if (!hologram.isShowing()) {
//            return false;
//        }
//        return handler.destroyHologram(hologram);
//    }
//}
