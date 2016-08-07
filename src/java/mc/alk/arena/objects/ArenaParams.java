package mc.alk.arena.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import mc.alk.arena.BattleArena;
import mc.alk.arena.controllers.ParamController;
import mc.alk.arena.objects.arenas.ArenaType;
import mc.alk.arena.objects.options.JoinOptions;
import mc.alk.arena.objects.options.StateOptions;
import mc.alk.arena.objects.options.TransitionOption;
import mc.alk.arena.util.MinMax;


public class ArenaParams {
    ArenaType arenaType;
    Boolean rated;

    String name;
    String displayName;
    String cmd;

    Integer timeBetweenRounds;
    Integer secondsTillMatch;
    Integer matchTime;
    Integer secondsToLoot;

    Integer forceStartTime;

    StateGraph stateGraph;
    StateGraph mergedStateGraph;
    String tableName;

    ArenaParams parent;
    MinMax nTeams;
    MinMax teamSize;
    Boolean closeWaitroomWhileRunning;
    Boolean cancelIfNotEnoughPlayers;
    int arenaCooldown;
    int allowedTeamSizeDifference;
    int nLives;
    Boolean removePlayersOnLeave;

    private Map<Integer, MatchParams> teamParams;

    public ArenaParams() {}

    public ArenaParams(ArenaType at) {
        arenaType = at;
    }

    public ArenaParams(ArenaParams ap) {
        this( ap.getType() );
        copy( ap );
    }

    public void copy(ArenaParams ap){
        if (this == ap)
            return;
        arenaType = ap.arenaType;
        rated = ap.rated;
        cmd = ap.cmd;
        name = ap.name;
        timeBetweenRounds = ap.timeBetweenRounds;
        secondsTillMatch = ap.secondsTillMatch;
        secondsToLoot = ap.secondsToLoot;
        tableName = ap.tableName;
        closeWaitroomWhileRunning = ap.closeWaitroomWhileRunning;
        cancelIfNotEnoughPlayers= ap.cancelIfNotEnoughPlayers;
        arenaCooldown = ap.arenaCooldown;
        allowedTeamSizeDifference = ap.allowedTeamSizeDifference;
        matchTime = ap.matchTime;
        forceStartTime = ap.forceStartTime;
        nLives = ap.nLives;
        displayName = ap.displayName;
        mergedStateGraph = null;
        if (ap.stateGraph != null)
            stateGraph = new StateGraph(ap.stateGraph);
        if (ap.nTeams != null)
            nTeams = new MinMax(ap.nTeams);
        if (ap.teamSize != null)
            teamSize = new MinMax(ap.teamSize);
        teamParams = ap.teamParams;
        parent = ap.parent;
        removePlayersOnLeave = ap.removePlayersOnLeave;
    }

    public void flatten() {
        if ( parent == null ) return;
        
        if ( arenaType == null) arenaType = parent.getType();
        if ( rated == null) rated = parent.isRated();
        if ( cmd == null) cmd = parent.getCommand();
        if ( name == null) name = parent.getName();
        if ( timeBetweenRounds == null) timeBetweenRounds = parent.getTimeBetweenRounds();
        if ( secondsTillMatch == null) secondsTillMatch = parent.getSecondsTillMatch();
        if ( matchTime == null) matchTime = parent.getMatchTime();
        if ( forceStartTime == null) forceStartTime = parent.getForceStartTime();
        if ( secondsToLoot == null) secondsToLoot = parent.getSecondsToLoot();
        if ( tableName == null) tableName = parent.getDBTableName();
        if ( nLives == 0 ) nLives = parent.getNLives();
        if ( removePlayersOnLeave == null) removePlayersOnLeave = parent.getRemovePlayersOnLeave();
        if ( closeWaitroomWhileRunning == null)
             closeWaitroomWhileRunning = parent.isWaitroomClosedWhenRunning();
        if ( cancelIfNotEnoughPlayers == null) cancelIfNotEnoughPlayers = parent.isCancelIfNotEnoughPlayers();
        
        if ( arenaCooldown == 0 ) arenaCooldown = parent.getArenaCooldown();
        if ( allowedTeamSizeDifference == 0 ) allowedTeamSizeDifference = parent.getAllowedTeamSizeDifference();
        
        if ( displayName == null)  displayName = parent.getDisplayName();
        stateGraph = mergeChildWithParent( this, parent );
        if ( nTeams == null && parent.getNTeams() != null) nTeams = new MinMax(parent.getNTeams());
        if ( teamSize == null && parent.getTeamSize() != null) teamSize = new MinMax(parent.getTeamSize());

        if ( teamParams != null && parent.getTeamParams() != null) {
            HashMap<Integer, MatchParams> tp = new HashMap<>( teamParams);
            tp.putAll(parent.getTeamParams());
            parent = null;
            teamParams = null;
            for (Entry<Integer, MatchParams> e : tp.entrySet()) {
                MatchParams ap = ParamController.copyParams(e.getValue());
                ap.setParent(this);
                ap.flatten();
                tp.put(e.getKey(), ap);
            }
            teamParams = tp;
            
        } else if (parent.getTeamParams() != null) {
            HashMap<Integer, MatchParams> tp = new HashMap<>(parent.getTeamParams());
            parent = null;
            teamParams = null;
            for (Entry<Integer, MatchParams> e : tp.entrySet()) {
                MatchParams ap = ParamController.copyParams(e.getValue());
                ap.setParent(this);
                ap.flatten();
                tp.put(e.getKey(), ap);
            }
            teamParams = tp;
            
        } else if ( teamParams != null) {
            HashMap<Integer, MatchParams> tp = new HashMap<>( teamParams);
            parent = null;
            teamParams = null;
            for (Entry<Integer, MatchParams> e : tp.entrySet()) {
                MatchParams ap = ParamController.copyParams(e.getValue());
                ap.setParent(this);
                ap.flatten();
                tp.put(e.getKey(), ap);
            }
            teamParams = tp;
        }
        mergedStateGraph = stateGraph;
        parent = null;
    }

    private StateGraph mergeChildWithParent(ArenaParams cap, ArenaParams pap) {
        StateGraph mt = cap.stateGraph == null ? new StateGraph() 
                                               : new StateGraph(cap.stateGraph);
        
        if (pap != null) {
            StateGraph.mergeChildWithParent(mt, mergeChildWithParent(pap, pap.parent));
        }
        return mt;
    }

    public StateGraph getThisStateGraph(){
        return stateGraph;
    }

    public void setStateGraph(StateGraph _stateGraph) {
        stateGraph = _stateGraph;
        clearMerged();
    }

    public MinMax getThisTeamSize() {
        return teamSize;
    }

    public MinMax getThisNTeams() {
        return nTeams;
    }

    public String getPlayerRange() {
        return ArenaSize.rangeString(getMinPlayers(),getMaxPlayers());
    }

    public ArenaType getType() {return arenaType;}

    public void setType(ArenaType type) { arenaType = type; }

    public boolean intersect(ArenaParams params) {
        if (    !getType().matches( params.getType() )
                || (    getNTeams() != null 
                        && params.getNTeams() != null 
                        && !getNTeams().intersect( params.getNTeams() ) ) )
                return false;
        
        return getTeamSize() != null && params.getTeamSize() != null 
                && !getTeamSize().intersect( params.getTeamSize() );
    }

    public boolean matches(final JoinOptions jo){
        return matches(jo.getMatchParams());
    }

    public boolean matches(final ArenaParams ap) {
        if (arenaType != null && ap.arenaType != null && !arenaType.matches(ap.arenaType)) {
            return false;}
        MinMax nt = getNTeams();
        MinMax nt2 = ap.getNTeams();
        if (nt != null && nt2 != null && !nt.intersect(nt2)){
            return false;}
        MinMax ts = getTeamSize();
        MinMax ts2 = ap.getTeamSize();
        return !(ts != null && ts2 != null && !ts.intersect(ts2));
    }

    public Collection<String> getInvalidMatchReasons(ArenaParams ap) {
        List<String> reasons = new ArrayList<>();
        if (arenaType == null) reasons.add("ArenaType is null");
        if (ap.arenaType == null) reasons.add("Passed params have an arenaType of null");
        else reasons.addAll(arenaType.getInvalidMatchReasons(ap.getType()));
        if (getNTeams() != null && ap.getNTeams() != null && !getNTeams().intersect(ap.getNTeams())){
            reasons.add("Arena accepts numTeams="+getNTeams()+". you requested "+ap.getNTeams());
        }
        if (getTeamSize() != null && ap.getTeamSize() != null && !getTeamSize().intersect(ap.getTeamSize())){
            reasons.add("Arena accepts teamSize="+ getTeamSize()+". you requested "+ap.getTeamSize());
        }
        return reasons;
    }

    public boolean valid() {
        return (arenaType != null &&
                (nTeams == null || nTeams.valid()) && (teamSize == null || teamSize.valid()));
    }

    public Collection<String> getInvalidReasons() {
        List<String> reasons = new ArrayList<>();
        if (arenaType == null) reasons.add("ArenaType is null");
        if (nTeams != null && !nTeams.valid()){
            reasons.add("Min Teams is greater than Max Teams " + nTeams.min + ":" + nTeams.max);}
        if (teamSize != null && !teamSize.valid()){
            reasons.add("Min Team Size is greater than Max Team Size " + teamSize.min + ":" + teamSize.max);}
        return reasons;
    }

    public String getCommand() {
        return cmd != null ? cmd 
                           : (parent != null ? parent.getCommand() 
                                             : null);

    }

    public void setRated(boolean _rated) {
        rated = _rated;
    }

    public Boolean isRated(){
        return rated != null ? rated : (parent != null ? parent.isRated() : null);
    }

    public void setSecondsToLoot(Integer i) {
        secondsToLoot=i;
    }

    public Integer getSecondsToLoot() {
        return secondsToLoot != null ? secondsToLoot 
                                     : ( parent != null ? parent.getSecondsToLoot() 
                                                        : null );
    }

    public void setSecondsTillMatch(Integer i) {
        secondsTillMatch=i;
    }

    public Integer getSecondsTillMatch() {
        return secondsTillMatch != null ? secondsTillMatch 
                                        : (parent != null ? parent.getSecondsTillMatch() 
                                                          : null);
    }

    public void setTimeBetweenRounds(Integer i) {
        timeBetweenRounds = i;
    }

    public Integer getTimeBetweenRounds() {
        return timeBetweenRounds != null ? timeBetweenRounds 
                                         : (parent != null ? parent.getTimeBetweenRounds() 
                                                           : null);
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    public String getDBTableName(){
        return tableName != null ? tableName 
                                 : (parent != null ? parent.getDBTableName() 
                                                   : null);
    }

    public String getName() {
        return name != null ? name 
                            : (parent != null ? parent.getName() 
                                              : null);
    }
    public void setName(String name) {
        this.name = name;
    }

    public String toPrettyString() {
        return  getDisplayName()+":"+arenaType+",numTeams="+getNTeams()+",teamSize="+ getTeamSize();
    }

    private ChatColor getColor(Object o) {
        return o == null ? ChatColor.GOLD : ChatColor.WHITE;
    }

    public String toSummaryString() {
        return  "&2&f" + name + "&2:&f" + arenaType +
                "&2,numTeams=" + getColor(nTeams) + getNTeams() +
                "&2,teamSize=" + getColor(teamSize) + getTeamSize() + "\n" +
                "&5forceStartTime=" + getColor(forceStartTime) + getForceStartTime() +
                "&5, timeUntilMatch=" + getColor(secondsTillMatch) + getSecondsTillMatch() +
                "&5, matchTime=" + getColor(matchTime)+getMatchTime() +
                "&5, secondsToLoot=" + getColor(secondsToLoot) + getSecondsToLoot() + "\n" +
                "&crated=" + getColor(rated) + isRated() + "&c, nLives=" + getColor(nLives) + getNLives() + "&e";
    }

    public String getOptionsSummaryString() {
        return getStateGraph().getOptionString(stateGraph);
    }

    @Override
    public String toString(){
        return  name + ":" + arenaType + ",numTeams=" + getNTeams() + 
                ",teamSize=" + getTeamSize() + " options=\n" 
                                            + (getThisStateGraph() == null ? "" 
                                                                          : getThisStateGraph().getOptionString());
    }

    public boolean isDuelOnly() {
        return getStateGraph().hasOptionAt(MatchState.DEFAULTS, TransitionOption.DUELONLY);
    }

    public boolean isAlwaysOpen(){
        return getStateGraph().hasOptionAt(MatchState.DEFAULTS, TransitionOption.ALWAYSOPEN);
    }

    public void setParent(ArenaParams parent) {
        this.parent=parent;
        clearMerged();
    }

    protected void clearMerged() {
        this.mergedStateGraph = null;
        if (teamParams!=null) {
            for (MatchParams mp: teamParams.values()){
                mp.clearMerged();
            }
        }
    }

    public ArenaParams getParent() {
        return parent;
    }

    public int getMinTeamSize() {
        return teamSize != null ? teamSize.min : (parent != null ? parent.getMinTeamSize() : 0);
    }

    public int getMaxTeamSize() {
        return teamSize != null ? teamSize.max : (parent != null ? parent.getMaxTeamSize() : 0);
    }

    public int getMinTeams() {
        return nTeams != null ? nTeams.min : (parent != null ? parent.getMinTeams() : 0);
    }

    public int getMaxTeams() {
        return nTeams != null ? nTeams.max : (parent != null ? parent.getMaxTeams() : 0);
    }

    public int getMaxPlayers() {
        MinMax nt = getNTeams();
        MinMax ts = getTeamSize();
        if (nt==null || ts == null)
            return 0;
        return nt.max == ArenaSize.MAX || ts.max == ArenaSize.MAX ? ArenaSize.MAX 
                                                                  : nt.max * ts.max;
    }

    public int getMinPlayers() {
        MinMax nt = getNTeams();
        MinMax ts = getTeamSize();
        if (nt==null || ts == null)
            return 0;
        return nt.min == ArenaSize.MAX || ts.min == ArenaSize.MAX ? ArenaSize.MAX 
                                                                  : nt.min * ts.min;
    }

    public void setNTeams(int size) {
        setNTeams(new MinMax(size));
    }
    public void setNTeams(MinMax mm) {
        if (mm == null){
            nTeams = null;
        } else {
            if (nTeams == null){
                nTeams = new MinMax(mm);
            } else {
                nTeams.min = mm.min;
                nTeams.max = mm.max;
            }
        }
    }

    /**
     * @return MinMax representing the number of teams
     */
    public MinMax getNTeams(){
        return nTeams != null ? nTeams : (parent != null ? parent.getNTeams() : null);
    }

    /**
     * @return MinMax representing the team sizes
     */
    public MinMax getTeamSize(){
        return teamSize != null ? teamSize : (parent != null ? parent.getTeamSize() : null);
    }
    public void setTeamSize(int size) {
        setTeamSize(new MinMax(size));
    }
    public void setTeamSize(MinMax mm) {
        if (mm == null){
            teamSize = null;
        } else {
            if (teamSize == null){
                teamSize = new MinMax(mm);
            } else {
                teamSize.min = mm.min;
                teamSize.max = mm.max;
            }
        }
    }

    public void setMinTeamSize(int n) {
        if (teamSize == null){ teamSize = new MinMax(n);}
        else teamSize.min = n;
    }
    public void setMaxTeamSize(int n) {
        if (teamSize == null){ teamSize = new MinMax(n);}
        else teamSize.max = n;
    }
    public void setMinTeams(int n) {
        if (nTeams == null){ nTeams = new MinMax(n);}
        else nTeams.min = n;
    }
    public void setMaxTeams(int n) {
        if (nTeams == null){ nTeams = new MinMax(n);}
        else nTeams.max = n;
    }

    public boolean matchesTeamSize(int i) {
        return teamSize != null ? teamSize.contains(i) : (parent != null && parent.matchesTeamSize(i));
    }

    public boolean hasOptionAt(CompetitionState state, TransitionOption op) {
        return getStateGraph().hasOptionAt(state, op);
    }

    public boolean hasOptionAt(CompetitionState state, StateOption op) {
        return getStateGraph().hasOptionAt(state, op);
    }

    public boolean hasEntranceFee() {
        return hasOptionAt(MatchState.PREREQS,TransitionOption.MONEY);
    }

    public Double getEntranceFee(){
        return getDoubleOption(MatchState.PREREQS, TransitionOption.MONEY);
    }

    public Double getDoubleOption(CompetitionState state, TransitionOption option){
        return getStateGraph().getDoubleOption(state, option);
    }

    public boolean hasAnyOption(TransitionOption option) {
        return getStateGraph().hasAnyOption(option);
    }

    public List<ItemStack> getWinnerItems() {
        return getGiveItems(MatchState.WINNERS);
    }

    public List<ItemStack> getLoserItems() {
        return getGiveItems(MatchState.LOSERS);
    }

    public List<ItemStack> getGiveItems(CompetitionState state) {
        StateOptions tops = getStateOptions(state);
        return (tops != null && tops.hasOption(TransitionOption.GIVEITEMS)) ? tops.getGiveItems() : null;
    }

    public List<PotionEffect> getEffects(CompetitionState state) {
        StateOptions tops = getStateOptions(state);
        return (tops != null && tops.hasOption(TransitionOption.ENCHANTS)) ? tops.getEffects() : null;
    }

    public boolean needsWaitroom() {
        return getStateGraph().hasAnyOption(TransitionOption.TELEPORTMAINWAITROOM, TransitionOption.TELEPORTWAITROOM);
    }

    public boolean needsSpectate() {
        return getStateGraph().hasAnyOption(TransitionOption.TELEPORTSPECTATE);
    }

    public boolean needsLobby() {
        return getStateGraph().hasAnyOption(TransitionOption.TELEPORTMAINLOBBY, TransitionOption.TELEPORTLOBBY);
    }

    public Boolean isWaitroomClosedWhenRunning(){
        return closeWaitroomWhileRunning != null ? closeWaitroomWhileRunning :
                (parent != null ? parent.isWaitroomClosedWhenRunning() : null);
    }

    public void setWaitroomClosedWhileRunning(Boolean value) {
        this.closeWaitroomWhileRunning = value;
    }

    public Boolean isCancelIfNotEnoughPlayers(){
        return cancelIfNotEnoughPlayers != null ? cancelIfNotEnoughPlayers :
                (parent != null ? parent.isCancelIfNotEnoughPlayers() : null);
    }

    public void setCancelIfNotEnoughPlayers(Boolean value) {
        this.cancelIfNotEnoughPlayers = value;
    }

    public String getDisplayName() {
        return displayName != null ? displayName :
                (parent != null ? parent.getDisplayName() : this.getName());
    }

    public String getThisDisplayName() {
        return displayName;
    }

    public int getQueueCount() {
        return BattleArena.getBAController().getArenaMatchQueue().getQueueCount(this);
    }

    public void setArenaCooldown(int cooldown) {
        arenaCooldown = cooldown;
    }

    public int getArenaCooldown() {
        return arenaCooldown != 0 ? arenaCooldown 
                                  : (parent != null ? parent.getArenaCooldown()
                                                    : 0 );
    }

    public void setAllowedTeamSizeDifference(int difference) {
        allowedTeamSizeDifference = difference;
    }

    public int getAllowedTeamSizeDifference() {
        return allowedTeamSizeDifference != 0 ? allowedTeamSizeDifference 
                                              : (parent != null ? parent.getAllowedTeamSizeDifference()
                                                                : 0 );
    }

    public Integer getForceStartTime() {
        return forceStartTime != null ? forceStartTime : (parent!= null ? parent.getForceStartTime() : null);
    }
    public void setForceStartTime(Integer _forceStartTime) {
        forceStartTime = _forceStartTime;
    }

    public Integer getMatchTime() {
        return matchTime == null && parent!=null ? parent.getMatchTime() 
                                                 : matchTime;
    }

    public void setMatchTime(Integer _matchTime) {
        matchTime = _matchTime;
    }
    public void setNLives(Integer nlives){
        nLives = nlives;
    }

    public int getNLives() {
        return nLives != 0 ? nLives 
                           : parent != null ? parent.getNLives() 
                                            : 0;
    }

    public Boolean getRemovePlayersOnLeave() {
        return removePlayersOnLeave == null && parent != null ? parent.getRemovePlayersOnLeave() : removePlayersOnLeave;
    }

    public void setRemovePlayersOnLeave(Boolean _removePlayersOnLeave) {
        removePlayersOnLeave = _removePlayersOnLeave;
    }

    public Map<Integer, MatchParams> getTeamParams() {
        return teamParams == null && parent != null ? parent.getTeamParams() : teamParams;
    }

    public MatchParams getTeamParams(int index) {
        if (teamParams!=null) {
            MatchParams mp = teamParams.get(index);
            if (mp != null) {
                return mp;}
        }
        if (parent != null) {
            return parent.getTeamParams(index);
        }
        return null;
    }

    public Map<Integer, MatchParams> getThisTeamParams() {
        return teamParams;
    }

    public void setTeamParams(Map<Integer, MatchParams> teamParams) {
        this.teamParams = teamParams;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public StateGraph getStateOptions() {
        return getStateGraph();
    }

    public StateGraph getStateGraph(){
        if (mergedStateGraph != null)
            return mergedStateGraph;
        if (stateGraph == null && parent!=null) {
            /// this is a bit hard to keep synced, but worth it for the speed improvements
            mergedStateGraph = parent.getStateGraph();
        } else {
            mergedStateGraph = mergeChildWithParent(this, this.parent);
        }
        return mergedStateGraph;
    }

    public StateOptions getStateOptions(CompetitionState state) {
        return getStateGraph().getOptions(state);
    }
}
