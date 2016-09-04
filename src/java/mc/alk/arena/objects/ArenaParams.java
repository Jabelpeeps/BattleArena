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

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.BattleArena;
import mc.alk.arena.controllers.ParamController;
import mc.alk.arena.objects.arenas.ArenaType;
import mc.alk.arena.objects.options.JoinOptions;
import mc.alk.arena.objects.options.StateOptions;
import mc.alk.arena.objects.options.TransitionOption;
import mc.alk.arena.util.MinMax;


public class ArenaParams {
    @Getter @Setter ArenaType type;
    @Setter Boolean rated;

    @Setter String name;
    @Setter String arenaDisplayName;
    @Setter String command;

    @Setter Integer timeBetweenRounds;
    @Setter Integer secondsTillMatch;
    @Setter Integer matchTime;
    @Setter Integer secondsToLoot;

    @Setter Integer forceStartTime;

    @Getter StateGraph arenaStateGraph;
    protected StateGraph mergedStateGraph;
    @Setter String tableName;

    @Getter ArenaParams parent;
    @Getter MinMax arenaNTeams;
    @Getter MinMax arenaTeamSize;
    @Setter Boolean closeWaitroomWhileRunning;
    @Setter Boolean cancelIfNotEnoughPlayers;
    @Setter int arenaCooldown;
    @Setter int allowedTeamSizeDifference;
    @Setter int nLives;
    @Setter Boolean removePlayersOnLeave;

    @Getter @Setter private Map<Integer, MatchParams> arenaTeamParams;

    public ArenaParams() {}

    public ArenaParams(ArenaType at) {
        type = at;
    }

    public ArenaParams(ArenaParams ap) {
        this( ap.getType() );
        copy( ap );
    }

    public void copy(ArenaParams ap){
        if (this == ap)
            return;
        type = ap.type;
        rated = ap.rated;
        command = ap.command;
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
        arenaDisplayName = ap.arenaDisplayName;
        mergedStateGraph = null;
        
        if (ap.arenaStateGraph != null)
            arenaStateGraph = new StateGraph(ap.arenaStateGraph);
        
        if (ap.arenaNTeams != null)
            arenaNTeams = new MinMax(ap.arenaNTeams);
        
        if (ap.arenaTeamSize != null)
            arenaTeamSize = new MinMax(ap.arenaTeamSize);
        
        arenaTeamParams = ap.arenaTeamParams;
        parent = ap.parent;
        removePlayersOnLeave = ap.removePlayersOnLeave;
    }

    public void flatten() {
        if ( parent == null ) return;
        
        if ( type == null) type = parent.getType();
        if ( rated == null) rated = parent.isRated();
        if ( command == null) command = parent.getCommand();
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
        
        if ( cancelIfNotEnoughPlayers == null) 
            cancelIfNotEnoughPlayers = parent.isCancelIfNotEnoughPlayers();
        
        if ( arenaCooldown == 0 ) arenaCooldown = parent.getArenaCooldown();
        if ( allowedTeamSizeDifference == 0 ) allowedTeamSizeDifference = parent.getAllowedTeamSizeDifference();
        
        if ( arenaDisplayName == null)  arenaDisplayName = parent.getDisplayName();
        
        arenaStateGraph = mergeChildWithParent( this, parent );
        
        if ( arenaNTeams == null && parent.getNTeams() != null) arenaNTeams = new MinMax(parent.getNTeams());
        if ( arenaTeamSize == null && parent.getTeamSize() != null) arenaTeamSize = new MinMax(parent.getTeamSize());

        if ( arenaTeamParams != null && parent.getTeamParams() != null) {
            HashMap<Integer, MatchParams> tp = new HashMap<>( arenaTeamParams);
            tp.putAll(parent.getTeamParams());
            parent = null;
            arenaTeamParams = null;
            for (Entry<Integer, MatchParams> e : tp.entrySet()) {
                MatchParams ap = ParamController.copyParams(e.getValue());
                ap.setParent(this);
                ap.flatten();
                tp.put(e.getKey(), ap);
            }
            arenaTeamParams = tp; 
        } 
        else if (parent.getTeamParams() != null) {
            HashMap<Integer, MatchParams> tp = new HashMap<>(parent.getTeamParams());
            parent = null;
            arenaTeamParams = null;
            for (Entry<Integer, MatchParams> e : tp.entrySet()) {
                MatchParams ap = ParamController.copyParams(e.getValue());
                ap.setParent(this);
                ap.flatten();
                tp.put(e.getKey(), ap);
            }
            arenaTeamParams = tp;
        } 
        else if ( arenaTeamParams != null) {
            HashMap<Integer, MatchParams> tp = new HashMap<>( arenaTeamParams);
            parent = null;
            arenaTeamParams = null;
            for (Entry<Integer, MatchParams> e : tp.entrySet()) {
                MatchParams ap = ParamController.copyParams(e.getValue());
                ap.setParent(this);
                ap.flatten();
                tp.put(e.getKey(), ap);
            }
            arenaTeamParams = tp;
        }
        mergedStateGraph = arenaStateGraph;
        parent = null;
    }

    private StateGraph mergeChildWithParent(ArenaParams cap, ArenaParams pap) {
        StateGraph mt = cap.arenaStateGraph == null ? new StateGraph() 
                                               : new StateGraph(cap.arenaStateGraph);
        
        if (pap != null) {
            StateGraph.mergeChildWithParent(mt, mergeChildWithParent(pap, pap.parent));
        }
        return mt;
    }

    public void setStateGraph(StateGraph _stateGraph) {
        arenaStateGraph = _stateGraph;
        clearMerged();
    }

    public String getPlayerRange() {
        return ArenaSize.rangeString(getMinPlayers(),getMaxPlayers());
    }

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
        if (type != null && ap.type != null && !type.matches(ap.type)) {
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
        if (type == null) reasons.add("ArenaType is null");
        if (ap.type == null) reasons.add("Passed params have an arenaType of null");
        else reasons.addAll(type.getInvalidMatchReasons(ap.getType()));
        if (getNTeams() != null && ap.getNTeams() != null && !getNTeams().intersect(ap.getNTeams())){
            reasons.add("Arena accepts numTeams="+getNTeams()+". you requested "+ap.getNTeams());
        }
        if (getTeamSize() != null && ap.getTeamSize() != null && !getTeamSize().intersect(ap.getTeamSize())){
            reasons.add("Arena accepts teamSize="+ getTeamSize()+". you requested "+ap.getTeamSize());
        }
        return reasons;
    }

    public boolean valid() {
        return (type != null &&
                (arenaNTeams == null || arenaNTeams.valid()) && (arenaTeamSize == null || arenaTeamSize.valid()));
    }

    public Collection<String> getInvalidReasons() {
        List<String> reasons = new ArrayList<>();
        if (type == null) reasons.add("ArenaType is null");
        if (arenaNTeams != null && !arenaNTeams.valid()){
            reasons.add("Min Teams is greater than Max Teams " + arenaNTeams.min + ":" + arenaNTeams.max);}
        if (arenaTeamSize != null && !arenaTeamSize.valid()){
            reasons.add("Min Team Size is greater than Max Team Size " + arenaTeamSize.min + ":" + arenaTeamSize.max);}
        return reasons;
    }

    public String getCommand() {
        return command != null ? command 
                           : (parent != null ? parent.getCommand() 
                                             : null);
    }
    public Boolean isRated(){
        return rated != null ? rated : (parent != null ? parent.isRated() : null);
    }
    public Integer getSecondsToLoot() {
        return secondsToLoot != null ? secondsToLoot 
                                     : ( parent != null ? parent.getSecondsToLoot() 
                                                        : null );
    }
    public Integer getSecondsTillMatch() {
        return secondsTillMatch != null ? secondsTillMatch 
                                        : (parent != null ? parent.getSecondsTillMatch() 
                                                          : null);
    }
    public Integer getTimeBetweenRounds() {
        return timeBetweenRounds != null ? timeBetweenRounds 
                                         : (parent != null ? parent.getTimeBetweenRounds() 
                                                           : null);
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

    public String toPrettyString() {
        return  getDisplayName() + ":" + type + ",numTeams=" + getNTeams() + ",teamSize=" + getTeamSize();
    }

    private ChatColor getColor(Object o) {
        return o == null ? ChatColor.GOLD : ChatColor.WHITE;
    }

    public String toSummaryString() {
        return  "&2&f" + name + "&2:&f" + type +
                "&2,numTeams=" + getColor(arenaNTeams) + getNTeams() +
                "&2,teamSize=" + getColor(arenaTeamSize) + getTeamSize() + "\n" +
                "&5forceStartTime=" + getColor(forceStartTime) + getForceStartTime() +
                "&5, timeUntilMatch=" + getColor(secondsTillMatch) + getSecondsTillMatch() +
                "&5, matchTime=" + getColor(matchTime)+getMatchTime() +
                "&5, secondsToLoot=" + getColor(secondsToLoot) + getSecondsToLoot() + "\n" +
                "&crated=" + getColor(rated) + isRated() + "&c, nLives=" + getColor(nLives) + getNLives() + "&e";
    }

    public String getOptionsSummaryString() {
        return getStateGraph().getOptionString(arenaStateGraph);
    }

    @Override
    public String toString(){
        return  name + ":" + type + ",numTeams=" + getNTeams() + 
                ",teamSize=" + getTeamSize() + " options=\n" 
                                            + ( arenaStateGraph == null ? "" 
                                                                        : arenaStateGraph.getOptionString());
    }

    public boolean isDuelOnly() {
        return getStateGraph().hasOptionAt(MatchState.DEFAULTS, TransitionOption.DUELONLY);
    }

    public boolean isAlwaysOpen(){
        return getStateGraph().hasOptionAt(MatchState.DEFAULTS, TransitionOption.ALWAYSOPEN);
    }

    public void setParent(ArenaParams parent) {
        this.parent = parent;
        clearMerged();
    }

    protected void clearMerged() {
        mergedStateGraph = null;
        if ( arenaTeamParams != null ) {
            for ( MatchParams mp: arenaTeamParams.values() ){
                mp.clearMerged();
            }
        }
    }

    public int getMinTeamSize() {
        return arenaTeamSize != null ? arenaTeamSize.min : (parent != null ? parent.getMinTeamSize() : 0);
    }

    public int getMaxTeamSize() {
        return arenaTeamSize != null ? arenaTeamSize.max : (parent != null ? parent.getMaxTeamSize() : 0);
    }

    public int getMinTeams() {
        return arenaNTeams != null ? arenaNTeams.min : (parent != null ? parent.getMinTeams() : 0);
    }

    public int getMaxTeams() {
        return arenaNTeams != null ? arenaNTeams.max : (parent != null ? parent.getMaxTeams() : 0);
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
            arenaNTeams = null;
        } else {
            if (arenaNTeams == null){
                arenaNTeams = new MinMax(mm);
            } else {
                arenaNTeams.min = mm.min;
                arenaNTeams.max = mm.max;
            }
        }
    }

    /**
     * @return MinMax representing the number of teams
     */
    public MinMax getNTeams(){
        return arenaNTeams != null ? arenaNTeams : (parent != null ? parent.getNTeams() : null);
    }

    /**
     * @return MinMax representing the team sizes
     */
    public MinMax getTeamSize(){
        return arenaTeamSize != null ? arenaTeamSize : (parent != null ? parent.getTeamSize() : null);
    }
    public void setTeamSize(int size) {
        setTeamSize(new MinMax(size));
    }
    public void setTeamSize(MinMax mm) {
        if (mm == null){
            arenaTeamSize = null;
        } else {
            if (arenaTeamSize == null){
                arenaTeamSize = new MinMax(mm);
            } else {
                arenaTeamSize.min = mm.min;
                arenaTeamSize.max = mm.max;
            }
        }
    }

    public void setMinTeamSize(int n) {
        if (arenaTeamSize == null){ arenaTeamSize = new MinMax(n);}
        else arenaTeamSize.min = n;
    }
    public void setMaxTeamSize(int n) {
        if (arenaTeamSize == null){ arenaTeamSize = new MinMax(n);}
        else arenaTeamSize.max = n;
    }
    public void setMinTeams(int n) {
        if (arenaNTeams == null){ arenaNTeams = new MinMax(n);}
        else arenaNTeams.min = n;
    }
    public void setMaxTeams(int n) {
        if (arenaNTeams == null){ arenaNTeams = new MinMax(n);}
        else arenaNTeams.max = n;
    }

    public boolean matchesTeamSize(int i) {
        return arenaTeamSize != null ? arenaTeamSize.contains(i) : (parent != null && parent.matchesTeamSize(i));
    }

    public boolean hasOptionAt(CompetitionState state, TransitionOption op) {
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
        return closeWaitroomWhileRunning != null ? closeWaitroomWhileRunning 
                                                 : (parent != null ? parent.isWaitroomClosedWhenRunning() 
                                                                   : null);
    }
    public Boolean isCancelIfNotEnoughPlayers(){
        return cancelIfNotEnoughPlayers != null ? cancelIfNotEnoughPlayers 
                                                : (parent != null ? parent.isCancelIfNotEnoughPlayers() 
                                                                  : null);
    }
    public String getDisplayName() {
        return arenaDisplayName != null ? arenaDisplayName 
                                   : (parent != null ? parent.getDisplayName() 
                                                     : this.getName());
    }

    public int getQueueCount() {
        return BattleArena.getBAController().getArenaMatchQueue().getQueueCount(this);
    }
    public int getArenaCooldown() {
        return arenaCooldown != 0 ? arenaCooldown 
                                  : (parent != null ? parent.getArenaCooldown()
                                                    : 0 );
    }
    public int getAllowedTeamSizeDifference() {
        return allowedTeamSizeDifference != 0 ? allowedTeamSizeDifference 
                                              : (parent != null ? parent.getAllowedTeamSizeDifference()
                                                                : 0 );
    }
    public Integer getForceStartTime() {
        return forceStartTime != null ? forceStartTime : (parent!= null ? parent.getForceStartTime() 
                                                                        : null);
    }
    public Integer getMatchTime() {
        return matchTime == null && parent!=null ? parent.getMatchTime() 
                                                 : matchTime;
    }
    public int getNLives() {
        return nLives != 0 ? nLives 
                           : parent != null ? parent.getNLives() 
                                            : 0;
    }
    public Boolean getRemovePlayersOnLeave() {
        return removePlayersOnLeave == null && parent != null ? parent.getRemovePlayersOnLeave() : removePlayersOnLeave;
    }
    public Map<Integer, MatchParams> getTeamParams() {
        return arenaTeamParams == null && parent != null ? parent.getTeamParams() : arenaTeamParams;
    }

    public MatchParams getTeamParams(int index) {
        if (arenaTeamParams != null) {
            MatchParams mp = arenaTeamParams.get(index);
            if (mp != null) 
                return mp;
        }
        if (parent != null) {
            return parent.getTeamParams(index);
        }
        return null;
    }

    public StateGraph getStateGraph(){
        if (mergedStateGraph != null)
            return mergedStateGraph;
        
        if (arenaStateGraph == null && parent!=null) {
            /// this is a bit hard to keep synced, but worth it for the speed improvements
            mergedStateGraph = parent.getStateGraph();
        } 
        else {
            mergedStateGraph = mergeChildWithParent(this, parent);
        }
        return mergedStateGraph;
    }

    public StateOptions getStateOptions(CompetitionState state) {
        return getStateGraph().getOptions(state);
    }
}
