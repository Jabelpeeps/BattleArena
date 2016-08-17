package mc.alk.arena.scoreboardapi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author alkarin
 */
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class SAPIScore {
    SEntry entry;
    int score;
}
