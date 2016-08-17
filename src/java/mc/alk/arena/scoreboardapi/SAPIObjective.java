package mc.alk.arena.scoreboardapi;

import java.util.List;
import java.util.TreeMap;

import org.bukkit.OfflinePlayer;

import lombok.Getter;
import lombok.Setter;

public class SAPIObjective implements SObjective{
    @Getter protected final String id;
    @Getter protected String criteria;
    protected String combinedDisplayName;
    protected String displayName;
    @Getter protected String displayNameSuffix;
    @Getter protected String displayNamePrefix;
    @Getter protected SAPIDisplaySlot displaySlot;

    @Getter @Setter protected SScoreboard scoreboard;

    /// Used for Team support
    @Getter @Setter protected boolean displayPlayers;
    @Getter @Setter protected boolean displayTeams;

    // 1-1000 scale, not strictly enforced
    // the lower priorities will not be preempted when set
    @Getter int priority;

    protected TreeMap<SEntry,SAPIScore> entries = new TreeMap<>();

    public SAPIObjective(String id, String displayName, String criteria) {
        this(id, displayName,criteria,50);
    }

    public SAPIObjective(String id, String displayName, String criteria, int priority) {
        this.id = id;
        this.criteria = colorChat(criteria);
        this.priority = priority;
        setDisplayName(displayName);
        displayPlayers = true;
        displayTeams = true;
        displaySlot = SAPIDisplaySlot.NONE;
    }

    @Override
    public String getBaseDisplayName() {
        return displayName;
    }

    @Override
    public String getDisplayName(){
        return combinedDisplayName;
    }

    @Override
    public void setDisplayName(String displayName){
        this.displayName = colorChat(displayName);
        _setDisplayName();
    }

    @Override
    public void setDisplayNameSuffix(String suffix) {
        this.displayNameSuffix = colorChat(suffix);
        _setDisplayName();
    }

    @Override
    public void setDisplayNamePrefix(String prefix) {
        this.displayNamePrefix = colorChat(prefix);
        _setDisplayName();
    }

    @Override
    public void setDisplaySlot(SAPIDisplaySlot slot) {
        this.displaySlot = slot;
        if (scoreboard != null){
            scoreboard.setDisplaySlot(slot, this,true);
        }
    }

    @Override
    public boolean setPoints(SEntry e, int points) {
        boolean has = entries.containsKey(e);
        setPoints(getOrCreateSAPIScore(e), points);
        return has;
    }

    protected boolean setPoints(SAPIScore o, int points) {
        boolean change = o.getScore() != points;
        if (change){
            o.setScore(points);
            return true;
        }
        return false;
    }

    @Override
    public boolean setTeamPoints(STeam team, int points) {
        if (displayTeams){
            setPoints(team,points);
        }
        if (displayPlayers){
            for (OfflinePlayer p: team.getPlayers()){
                SEntry e = scoreboard.getOrCreateEntry(p);
                setPoints(e,points);
            }
        }
        return true;
    }

    @Override
    public boolean setPoints(String id, int points) {
        if (scoreboard == null)
            return false;
        SEntry l = scoreboard.getEntry(id);
        if (l == null)
            return false;
        setPoints(l,points);
        return true;
    }

    @Override
    public int getPoints(String id) {
        SEntry l = scoreboard.getEntry(id);
        if (l == null)
            return -1;
        return getPoints(l);
    }

    @Override
    public int getPoints(SEntry e) {
        return entries.containsKey(e) ? entries.get(e).getScore() : -1;
    }

    public static String colorChat(String msg) { return msg.replace('&', (char) 167); }

    @Override
    public SEntry addEntry(OfflinePlayer p, int points) {
        return addEntry(p.getName(),points);
    }

    @Override
    public SEntry addEntry(String id, int points) {
        SEntry e = scoreboard.getEntry(id);
        if (e == null){
            if (getScoreboard() != null)
                e = scoreboard.createEntry(id, id);
            else
                throw new IllegalStateException("You cannot add an entry that hasnt already been created " + id);
        }
        addEntry(e,points);
        return e;
    }

    protected final SAPIScore getOrCreateSAPIScore(SEntry e){
        return getOrCreateSAPIScore(e,0);
    }

    protected final SAPIScore getOrCreateSAPIScore(SEntry e, int points){
        if (entries.containsKey(e))
            return entries.get(e);
        SAPIScore o = new SAPIScore(e, points);
        entries.put(e, o);
        setPoints(o, points);
        return o;
    }

    @Override
    public boolean addEntry(SEntry entry, int points) {
        if (entry instanceof STeam) {
            return addTeam((STeam) entry, points);}
        boolean has = entries.containsKey(entry);
        if (!has) {
            getOrCreateSAPIScore(entry, points);
        } else {
            setPoints(getOrCreateSAPIScore(entry), points);
        }
        return has;
    }

    @Override
    public SEntry removeEntry(OfflinePlayer player) {
        SEntry e = scoreboard.getEntry(player);
        if (e == null)
            return null;
        return removeEntry(e);
    }

    @Override
    public SEntry removeEntry(String id) {
        SEntry e = scoreboard.getEntry(id);
        if (e == null)
            return null;
        return removeEntry(e);
    }

    @Override
    public SEntry removeEntry(SEntry entry) {
        return entries.remove(entry)!=null ? entry : null;
    }

    @Override
    public STeam addTeam(String id, int points) {
        STeam t = scoreboard.getTeam(id);
        if (t != null){
            addTeam(t,points);
        }
        return t;
    }

    @Override
    public boolean addTeam(STeam entry, int points) {
        boolean has = entries.containsKey(entry);
        SAPIScore sc = getOrCreateSAPIScore(entry);
        setPoints(sc, points);
        if (isDisplayTeams()){
            setPoints(sc, points);}
        for (OfflinePlayer e: entry.getPlayers()) {
            sc = getOrCreateSAPIScore(scoreboard.getOrCreateEntry(e));
            if (isDisplayPlayers()){
                setPoints(sc, points);}
        }
        return has;
    }

    @Override
    public boolean contains(SEntry e) {
        return entries.containsKey(e);
    }

    @Override
    public void initPoints(List<SEntry> entries, List<Integer> points) {
        //todo
    }

    protected void _setDisplayName() {
        this.combinedDisplayName = SAPIUtil.createLimitedString(displayNamePrefix, displayName, displayNameSuffix,
                SAPI.MAX_OBJECTIVE_DISPLAYNAME_SIZE);
    }
}
