package mc.alk.scoreboardapi;

/**
 * @author alkarin
 */
public class SAPIScore {
    SEntry entry;
    int score;
    public SAPIScore(){

    }
    public SAPIScore(SEntry entry, int score) {
        this.entry = entry;
        this.score = score;
    }

    public SEntry getEntry() {
        return entry;
    }

    public void setEntry(SEntry entry) {
        this.entry = entry;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }


}
