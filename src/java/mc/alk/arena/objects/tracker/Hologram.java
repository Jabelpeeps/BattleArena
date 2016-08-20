package mc.alk.arena.objects.tracker;

import java.util.ArrayList;
import java.util.Arrays;

import org.bukkit.Location;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public class Hologram {

    @AllArgsConstructor
    public enum VerticalTextSpacing {
        COMPACT(0.30), SPACED(0.40);        
        @Getter private final double spacing;
    }

    @Getter private double distanceBetweenLines;
    @Getter private ArrayList<String> lines = new ArrayList<>();
    @Getter private ArrayList<Integer> ids = new ArrayList<>();
    @Getter @Setter private Location location;
    @Getter @Setter private boolean showing;

    public Hologram( VerticalTextSpacing type, Location loc, String... _lines ) {
        this( type.getSpacing(), loc, _lines );
    }

    public Hologram( double distance, Location loc, String... _lines ) {
        distanceBetweenLines = distance;
        lines.addAll(Arrays.asList(_lines));
        location = loc;
        showing = false;
    }

    public void setLines(String... _lines) {
        lines.clear();
        lines.addAll(Arrays.asList(_lines));
    }
}
