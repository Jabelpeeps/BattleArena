package mc.alk.arena.objects.regions;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mc.alk.arena.plugins.WorldGuardController;

@NoArgsConstructor @AllArgsConstructor
public class WorldGuardRegion implements ArenaRegion {

    @Getter @Setter protected String regionWorld;
    @Getter @Setter protected String regionName;

    @Override
    public Object yamlToObject(Map<String, Object> map, String value) {
        if ( value == null ) return null;

        String[] split = value.split(",");
        regionWorld = split[0];
        regionName = split[1];
        return new WorldGuardRegion( regionWorld, regionName );
    }

    @Override
    public Object objectToYaml() {
        return regionWorld + "," + regionName;
    }
    @Override
    public boolean valid() {
        return regionName != null && regionWorld != null
                && WorldGuardController.hasWorldGuard()
                && WorldGuardController.hasRegion(regionWorld, regionName);
    }
    @Override
    public String getWorldName() {
        return regionWorld;
    }
}
