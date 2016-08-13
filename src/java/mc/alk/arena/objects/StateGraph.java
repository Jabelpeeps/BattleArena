package mc.alk.arena.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import mc.alk.arena.objects.exceptions.InvalidOptionException;
import mc.alk.arena.objects.options.StateOptions;
import mc.alk.arena.objects.options.TransitionOption;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.util.InventoryUtil;

public class StateGraph {
	final Map<CompetitionState,StateOptions> ops = new HashMap<>();
	final EnumSet<TransitionOption> allops = EnumSet.noneOf( TransitionOption.class );;

	public StateGraph() {}
	public StateGraph(StateGraph o) {
		for (CompetitionState ms : o.ops.keySet()){
			ops.put(ms, new StateOptions(o.ops.get(ms)));
		}
	}

	public Map<CompetitionState, StateOptions> getAllOptions(){
		return ops;
	}

	public void addStateOptions(CompetitionState ms, StateOptions tops) {
		ops.put(ms, tops);
		allops.clear();
	}

	public void addStateOption(CompetitionState state, TransitionOption option) throws InvalidOptionException {
		StateOptions tops = ops.get(state);
		
		if (tops == null){
			tops = new StateOptions();
			ops.put(state, tops);
		}
		tops.addOption(option);
        allops.clear();
	}

	public void addStateOption(CompetitionState state, TransitionOption option, Object value) throws InvalidOptionException {
		StateOptions tops = ops.get(state);
		
		if (tops == null){
			tops = new StateOptions();
			ops.put(state, tops);
		}
		tops.addOption(option,value);
        allops.clear();
	}

	public boolean removeStateOption(CompetitionState state, TransitionOption option) {
		StateOptions tops = ops.get(state);
		return tops != null && tops.removeOption(option) != null;
	}

	public void removeStateOptions(CompetitionState ms) {
		ops.remove(ms);
        allops.clear();
	}

	private void calculateAllOptions(){
		
        allops.clear();

        for (StateOptions top: ops.values())
			allops.addAll(top.getOptions().keySet());
	}

	public boolean hasAnyOption(TransitionOption wgnoenter) {
        if ( allops.isEmpty() ) calculateAllOptions();
        
		return allops.contains(wgnoenter);
	}

	public boolean hasAnyOption(TransitionOption... options) {
        if ( allops.isEmpty() ) calculateAllOptions();
        
		for (TransitionOption op: options){
			if (allops.contains(op))
				return true;
		}
		return false;
	}

	public CompetitionState getCompetitionState(TransitionOption option) {
		for (CompetitionState state: ops.keySet()){
		    
			StateOptions tops = ops.get(state);
			
			if (tops.hasOption(option))
				return state;
		}
		return null;
	}

	public boolean hasAllOptions(TransitionOption... options) {
		Set<TransitionOption> opts = EnumSet.copyOf( Arrays.asList( options ) );
		
        if ( allops.isEmpty() ) calculateAllOptions();
        
        return allops.containsAll(opts);
	}

	public boolean hasInArenaOrOptionAt(CompetitionState state, TransitionOption option) {
		StateOptions tops = ops.get(state);
		return tops == null ? hasOptionAt(MatchState.INARENA,option) : tops.hasOption(option);
	}

    /**
     * Return whether the given state is found with the given option
     * @param state CompetitionState
     * @param withindistance StateOption
     * @return true or false
     */
	public boolean hasOptionAt(CompetitionState state, TransitionOption withindistance) {
		StateOptions tops = ops.get(state);
		return tops != null && tops.hasOption(withindistance);
	}

    public boolean needsClearInventory() {
		return ops.containsKey(MatchState.PREREQS) && ops.get(MatchState.PREREQS).hasOption(TransitionOption.CLEARINVENTORY);
	}
	public String getRequiredString(String header) {
		return ops.containsKey(MatchState.PREREQS) ? ops.get(MatchState.PREREQS).getNotReadyMsg(header) : null;
	}
	public String getRequiredString(ArenaPlayer p, World w, String header) {
		return ops.containsKey(MatchState.PREREQS) ? ops.get(MatchState.PREREQS).getNotReadyMsg(p,w,header) : null;
	}
	public String getGiveString(CompetitionState ms) {
		return ops.containsKey(ms) ? ops.get(ms).getPrizeMsg(null) : null;
	}
	public StateOptions getOptions(CompetitionState ms) {
		return ops.get(ms);
	}
	public Double getEntranceFee() {
		return ops.containsKey(MatchState.PREREQS) ? ops.get(MatchState.PREREQS).getMoney() : null;
	}
    public boolean hasEntranceFee() {
		return ops.containsKey(MatchState.PREREQS) && ops.get(MatchState.PREREQS).hasMoney();
    }
	public boolean playerReady(ArenaPlayer p, World w) {
		return !ops.containsKey(MatchState.PREREQS) || ops.get(MatchState.PREREQS).playerReady(p, w);
	}
    public List<ItemStack> getNeedItems(CompetitionState state){
        return ops.containsKey(state) ? ops.get(state).getNeedItems() : null;
    }
    public List<ItemStack> getTakeItems(CompetitionState state){
        return ops.containsKey(state) ? ops.get(state).getTakeItems() : null;
    }
    public List<ItemStack> getGiveItems(CompetitionState state){
        return ops.containsKey(state) ? ops.get(state).getGiveItems() : null;
    }
    
	public boolean teamReady(ArenaTeam t, World w) {
		StateOptions to = ops.get(MatchState.PREREQS);
		
		if ( to == null ) return true;
		
		for ( ArenaPlayer p : t.getPlayers() ){
			if (!to.playerReady(p,w))
				return false;
		}
		return true;
	}
	
	public List<MatchState> getMatchStateRange(TransitionOption startOption, TransitionOption endOption) {
		boolean foundOption = false;
		List<MatchState> list = new ArrayList<>();
		
		for ( MatchState ms : MatchState.values() ) {
			StateOptions to = ops.get(ms);
			if ( to == null ) continue;
			
			if ( to.hasOption( startOption ) ) foundOption = true;
			
			if ( to.hasOption( endOption ) ) return list;
			
			if ( foundOption ) list.add( ms );
		}
		return list;
	}
   
	final Comparator<CompetitionState> compComp = (o1, o2) -> {
	    return o1.globalOrdinal() - o2.globalOrdinal();
	};
	
	public String getOptionString() {
        return getOptionString(null);
    }
    
    public String getOptionString(StateGraph subset) {
        
        if (subset == null) {
            subset = new StateGraph();
        }
        StringBuilder sb = new StringBuilder();
        List<CompetitionState> states = new ArrayList<>(ops.keySet());
        List<CompetitionState> states2 = new ArrayList<>(subset.ops.keySet());
        Collections.sort(states, compComp);
        Collections.sort(states2, compComp);

        for (CompetitionState ms : states){
            
            StateOptions to = ops.get(ms);
            StateOptions to2 = subset.ops.get(ms);
            sb.append(ms).append(" -- ");
            sb.append(to.getOptionString(to2)).append("\n");
            
            Map<Integer, ArenaClass> classes = ( to2 != null && to2.getClasses() != null ) ? to2.getClasses() 
                                                                                           : to.getClasses();
            if (classes != null){
                sb.append("             classes - ");
                
                for (ArenaClass ac : classes.values()){
                    sb.append(" ").append(ac.getDisplayName());}
                sb.append("\n");
            }
            
            List<ItemStack> items = ( to2 != null && to2.getGiveItems() != null ) ? to2.getGiveItems() 
                                                                                  : to.getGiveItems();
            if (items != null){
                sb.append("             items - ");
                for (ItemStack item: items){
                    sb.append(" ").append(InventoryUtil.getItemString(item));}
                sb.append("\n");
            }
            
            items = ( to2 != null && to2.getNeedItems() != null ) ? to2.getNeedItems() 
                                                                  : to.getNeedItems();
            if (items != null){
                sb.append("             needitems - ");
                for (ItemStack item: items){
                    sb.append(" ").append(InventoryUtil.getItemString(item));}
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public Double getDoubleOption(CompetitionState state, TransitionOption option) {
		StateOptions tops = getOptions(state);
		return tops == null ? null : tops.getDouble(option);
	}

	public static StateGraph mergeChildWithParent(StateGraph cmt, StateGraph pmt) {
        if (cmt == null && pmt == null)
            return null;
        
        if (cmt == null)
			cmt = new StateGraph();
        
		if (pmt == null)
			return cmt;
		
		for (Entry<CompetitionState, StateOptions> entry: pmt.ops.entrySet()){
            if (cmt.ops.containsKey(entry.getKey())){
                cmt.ops.get(entry.getKey()).addOptions(entry.getValue());
            } else {
                cmt.ops.put(entry.getKey(), new StateOptions(entry.getValue()));
            }
		}
		cmt.calculateAllOptions();
		return cmt;
	}

    public void deleteOptions(CompetitionState state) {
        ops.remove(state);
        calculateAllOptions();
    }
}

