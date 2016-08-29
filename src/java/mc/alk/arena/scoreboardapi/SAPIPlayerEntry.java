package mc.alk.arena.scoreboardapi;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import mc.alk.arena.util.MessageUtil;

public class SAPIPlayerEntry implements SEntry, Comparable<SEntry>{
	OfflinePlayer idOfflinePlayer;
    private String displayName;
    private String displayNameSuffix;
    private String displayNamePrefix;
    private String combinedDisplayName;
    OfflinePlayer displayOfflinePlayer;


    public SAPIPlayerEntry(OfflinePlayer p){
		this.idOfflinePlayer = p;
		setDisplayName(p.getName());
        if (combinedDisplayName != null && p.getName() != null && p.getName().equals(combinedDisplayName)) {
            displayOfflinePlayer = p;}
    }

	public SAPIPlayerEntry(OfflinePlayer p, String display) {
		this.idOfflinePlayer = p;
		setDisplayName(display);
        if (combinedDisplayName != null && p.getName() != null && p.getName().equals(combinedDisplayName)) {
            displayOfflinePlayer = p;}
	}

	@Override
    public OfflinePlayer getOfflinePlayer() {
        if (displayOfflinePlayer == null) {
            displayOfflinePlayer = Bukkit.getOfflinePlayer(combinedDisplayName);}
        return displayOfflinePlayer ;
	}

    @Override
	public String getId() {
		return idOfflinePlayer.getName();
	}

    @Override
    public String getDisplayName(){
        return combinedDisplayName;
    }

    @Override
    public String getBaseDisplayName() {return displayName;}

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = MessageUtil.colorChat(displayName);
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
    public String getDisplayNameSuffix() {
        return displayNameSuffix;
    }

    @Override
    public void setDisplayNamePrefix(String suffix) {
        displayNamePrefix = MessageUtil.colorChat(suffix);
        if (displayNamePrefix.length() > 8) {
            displayNamePrefix = displayNamePrefix.substring(0, 9);
        }
        _setDisplayName();
    }

    @Override
    public String getDisplayNamePrefix() {
        return displayNamePrefix;
    }

    private void _setDisplayName(){
        if ((displayNamePrefix != null ? displayNamePrefix.length() : 0) +
                displayName.length() +
                (displayNameSuffix != null ? displayNameSuffix.length() : 0) > 15) {
            int size = (displayNamePrefix != null ? displayNamePrefix.length() : 0) +
                    (displayNameSuffix != null ? displayNameSuffix.length() : 0);
            this.combinedDisplayName = (displayNamePrefix != null ? displayNamePrefix : "")+
                    displayName.substring(0, 16 - size) + (displayNameSuffix != null ? displayNameSuffix : "");
        } else {
            this.combinedDisplayName = (displayNamePrefix != null ? displayNamePrefix : "")+
                    displayName + (displayNameSuffix != null ? displayNameSuffix : "");
        }
        displayOfflinePlayer = null;
    }

    @Override
    public int compareTo(SEntry o) {
        return this.getId().compareTo(o.getId());
    }
}
