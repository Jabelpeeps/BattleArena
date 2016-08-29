package mc.alk.arena.objects.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import lombok.Getter;
import mc.alk.arena.util.MessageUtil;

public class SAPIEntry implements SEntry, Comparable<SAPIEntry>{
    @Getter private final String id;
    private String displayName;
    @Getter private String displayNameSuffix;
    @Getter private String displayNamePrefix;
    private String combinedDisplayName;
    protected OfflinePlayer offlinePlayer;

    public SAPIEntry(String _id, String _displayName){
		id = _id;
        setDisplayName(_displayName);
	}

	@Override
    public OfflinePlayer getOfflinePlayer() {
        if (offlinePlayer == null) 
            offlinePlayer = Bukkit.getPlayer( displayName );
        return offlinePlayer;
	}

	public OfflinePlayer getPlayerListName(){
        if (offlinePlayer == null) 
            offlinePlayer = Bukkit.getPlayer( displayName );
		return offlinePlayer;
	}

    @Override
    public String getDisplayName() { return combinedDisplayName; }
    @Override
    public String getBaseDisplayName() { return displayName; }

    @Override
    public void setDisplayName(String _displayName) {
        displayName = MessageUtil.colorChat(_displayName);
        _setDisplayName();
    }

    @Override
    public void setDisplayNameSuffix(String suffix) {
        displayNameSuffix = MessageUtil.colorChat(suffix);
        if (displayNameSuffix.length() > 8) {
            displayNameSuffix = displayNameSuffix.substring(0, 9);
        }
        _setDisplayName();
    }

    @Override
    public void setDisplayNamePrefix(String suffix) {
        displayNamePrefix = MessageUtil.colorChat(suffix);
        if (displayNamePrefix.length() > 8) {
            displayNamePrefix = displayNamePrefix.substring(0, 9);
        }
        _setDisplayName();
    }

    private void _setDisplayName() {
        combinedDisplayName = SAPIUtil.createLimitedString(displayNamePrefix, displayName, displayNameSuffix, SAPI.MAX_NAMESIZE);
        offlinePlayer = null;
    }

    @Override
    public String toString() { return "[SAPIEntry " + getId() + " : " + getDisplayName() + "]"; }
    @Override
    public int compareTo(SAPIEntry o) { return id.compareTo(o.id); }
}
