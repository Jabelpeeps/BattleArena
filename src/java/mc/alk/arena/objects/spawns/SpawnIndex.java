package mc.alk.arena.objects.spawns;

import lombok.AllArgsConstructor;

/**
 * @author alkarin
 */
@AllArgsConstructor
public class SpawnIndex {
    public final int teamIndex;
    public final int spawnIndex;

    public SpawnIndex( int index ) {
        this( index, 0 );
    }
}
