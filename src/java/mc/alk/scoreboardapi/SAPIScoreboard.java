package mc.alk.scoreboardapi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SAPIScoreboard implements SScoreboard{
	protected Map<String, SObjective> objectives = new HashMap<>();
	protected HashMap<SAPIDisplaySlot,SObjective> slots = new HashMap<>();
	protected final String name;
	protected Handler handler = new Handler();
    protected final Plugin plugin;
	public SAPIScoreboard(Plugin plugin, String name){
		this.name = name;
        this.plugin = plugin;
    }

	@Override
    public void clear(){
		objectives.clear();
	}

	@Override
    public SObjective registerNewObjective(SObjective obj) {
		objectives.put(obj.getId().toUpperCase(), obj);
		if (obj.getScoreboard() == null || !obj.getScoreboard().equals(this)){
			obj.setScoreboard(this);
		}
		if (obj.getDisplaySlot() != null)
			setDisplaySlot(obj.getDisplaySlot(),obj,false,true);
		return obj;
	}

	@Override
    public SObjective registerNewObjective(String id, String displayName, String criteria,
			SAPIDisplaySlot slot) {
		SAPIObjective o =  new SAPIObjective(id, displayName,criteria);
		o.setDisplayName(displayName);
		o.setDisplaySlot(slot);
		registerNewObjective(o);
		return o;
	}

	@Override
    public void setDisplaySlot(SAPIDisplaySlot slot, SObjective objective) {
		setDisplaySlot(slot,objective, false);
	}

	@Override
    public void setDisplaySlot(SAPIDisplaySlot slot, SObjective objective, boolean fromObjective) {
		setDisplaySlot(slot, objective,fromObjective, true);
	}

	private void _setDisplaySlot(SAPIDisplaySlot slot, SObjective objective, boolean fromObjective) {
		slots.put(slot, objective);
		if (!fromObjective)
			objective.setDisplaySlot(slot);
	}

	boolean setDisplaySlot(final SAPIDisplaySlot slot, final SObjective objective,
			boolean fromObjective, boolean swap) {
		if (!slots.containsKey(slot)){
			_setDisplaySlot(slot,objective,fromObjective);
			return true;
		}
        int opriority = slots.get(slot).getPriority();
        /// Check to see if we need to move
        /// if our new objective priority <= oldpriority
        if (objective.getPriority() <= opriority){
        	SAPIDisplaySlot swapSlot = slot.swap();
        	SObjective movingObjective = slots.get(slot);
        	if (!slots.containsKey(swapSlot) || opriority <= slots.get(swapSlot).getPriority()) {
        		_setDisplaySlot(swapSlot,movingObjective,fromObjective);
        	}
        	_setDisplaySlot(slot,objective,fromObjective);
        	return true;
        }
		return false;
	}

	@Override
    public SObjective getObjective(SAPIDisplaySlot slot) {
		return slots.get(slot);
	}

	@Override
    public SObjective getObjective(String id) {
		return objectives.get(id.toUpperCase());
	}

	@Override
    public String getPrintString() {
		StringBuilder sb = new StringBuilder();
		for (Entry<SAPIDisplaySlot,SObjective> entry : slots.entrySet()){
			sb.append("&5").append(entry.getKey()).append(" : ").append(entry.getValue()).append("\n");
		}
		return sb.toString();
	}

	@Override
    public SEntry createEntry(OfflinePlayer p) {
		return createEntry(p,p.getName());
	}

	@Override
    public SEntry createEntry(OfflinePlayer p, String displayName) {
		return handler.getOrCreateEntry(p, displayName);
	}

	@Override
    public SEntry createEntry(String id, String displayName) {
		return handler.getOrCreateEntry(id, displayName);
	}

	@Override
    public STeam createTeamEntry(String id, String displayName) {
		SAPITeam st = new SAPITeam(this,id,displayName);
		handler.registerEntry(st);
		return st;
	}

	@Override
    public SEntry removeEntry(OfflinePlayer p) {
		SEntry sb = handler.getEntry(p);
		if (sb != null){
			return removeEntry(sb);}
		return null;
	}

	@Override
    public SEntry removeEntry(SEntry e) {
		e = handler.removeEntry(e);
        if (e != null){
            for (SObjective o : this.getObjectives()) {
                o.removeEntry(e);
            }
        }
        return e;
    }

	@Override
    public String getName() {
		return name;
	}

	@Override
    public SEntry getEntry(String id) {
		return handler.getEntry(id);
	}

	@Override
    public SEntry getEntry(OfflinePlayer player) {
		return handler.getEntry(player);
	}

	@Override
    public STeam getTeam(String id) {
		return handler.getTeamEntry(id);
	}

	@Override
	public SEntry getOrCreateEntry(OfflinePlayer p) {
		return handler.getOrCreateEntry(p);
	}

	@Override
	public Collection<SEntry> getEntries() {
		return handler.getEntries();
	}

    @Override
    public void removeScoreboard(Player player) {
        /* nothing to do */
    }

    @Override
    public void setScoreboard(Player p) {
        /* nothing to do */
    }


    @Override
	public List<SObjective> getObjectives() {
		return new ArrayList<>(objectives.values());
	}


    @Override
    public boolean setEntryDisplayName(String id, String name) {
        SEntry e = handler.getEntry(id);
        if (e == null)
            return false;
        setEntryDisplayName(e, name);
        return true;
    }

    @Override
    public void setEntryDisplayName(SEntry entry, String name){
        entry.setDisplayName(name);
    }

    @Override
    public boolean setEntryNamePrefix(String id, String name) {
        SEntry e = handler.getEntry(id);
        if (e == null)
            return false;
        setEntryNamePrefix(e, name);
        return true;
    }

    @Override
    public void setEntryNamePrefix(SEntry entry, String name) {
        entry.setDisplayNamePrefix(name);
    }

    @Override
    public boolean setEntryNameSuffix(String id, String name) {
        SEntry e = handler.getEntry(id);
        if (e == null)
            return false;
        setEntryNameSuffix(e, name);
        return true;
    }

    @Override
    public void setEntryNameSuffix(SEntry entry, String name) {
        entry.setDisplayNameSuffix(name);
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public boolean hasThisScoreboard(Player player) {
        return handler.contains(player);
    }

}
