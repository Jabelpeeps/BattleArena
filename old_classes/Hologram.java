//package mc.alk.util.objects;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//
//import org.bukkit.Location;
//
//public class Hologram {
//
//    public enum VerticalTextSpacing {
//
//        COMPACT(0.30), SPACED(0.40);
//        private final double spacing;
//
//        VerticalTextSpacing(double spacing) {
//            this.spacing = spacing;
//        }
//
//        public double spacing() {
//            return spacing;
//        }
//    }
//
//    private double distanceBetweenLines;
//    private ArrayList<String> lines;
//    private ArrayList<Integer> ids;
//    private Location location;
//    private boolean showing;
//
//    public Hologram(VerticalTextSpacing type, Location location,
//            String... lines) {
//        this(type.spacing(), location, lines);
//    }
//
//    public Hologram(double distanceBetweenLines, Location location,
//            String... lines) {
//        this.lines = new ArrayList<>();
//        this.ids = new ArrayList<>();
//        this.distanceBetweenLines = distanceBetweenLines;
//        this.lines.addAll(Arrays.asList(lines));
//        this.location = location;
//        this.showing = false;
//    }
//
//    public double getDistanceBetweenLines() {
//        return distanceBetweenLines;
//    }
//
//    public ArrayList<String> getLines() {
//        return lines;
//    }
//
//    public ArrayList<Integer> getIds() {
//        return ids;
//    }
//
//    public Location getLocation() {
//        return location;
//    }
//
//    public void setLines(String... lines) {
//        this.lines.clear();
//        this.lines.addAll(Arrays.asList(lines));
//    }
//
//    public void setLocation(Location location) {
//        this.location = location;
//    }
//
//    public void setShowing(boolean option) {
//        this.showing = option;
//    }
//
//    public boolean isShowing() {
//        return showing;
//    }
//}
