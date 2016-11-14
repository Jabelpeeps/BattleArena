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
import mc.alk.arena.util.Util;


public class ArenaParams {
    @Setter Boolean rated;
    @Setter Boolean closeWaitroomWhileRunning;
    @Setter Boolean cancelIfNotEnoughPlayers;
    @Setter Boolean removePlayersOnLeave;

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

    @Getter @Setter ArenaType type;
    @Getter ArenaParams parent;
    @Getter ArenaSize arenaSize;
    @Setter int arenaCooldown;
    @Setter int allowedTeamSizeDifference;
    @Setter int nLives;

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
        if ( this == ap ) return;
        
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
        arenaTeamParams = ap.arenaTeamParams;
        parent = ap.parent;
        removePlayersOnLeave = ap.removePlayersOnLeave;
        
        if ( ap.arenaStateGraph != null )
            arenaStateGraph = new StateGraph( ap.arenaStateGraph );
        
        if ( ap.arenaSize != null )
            arenaSize = new ArenaSize( ap.arenaSize );      
    }

    public void flatten() {
        if ( parent == null ) return;
        
        if ( type == null ) type = parent.getType();
        if ( rated == null ) rated = parent.isRated();
        if ( command == null ) command = parent.getCommand();
        if ( name == null ) name = parent.getName();
        if ( timeBetweenRounds == null ) timeBetweenRounds = parent.getTimeBetweenRounds();
        if ( secondsTillMatch == null ) secondsTillMatch = parent.getSecondsTillMatch();
        if ( matchTime == null ) matchTime = parent.getMatchTime();
        if ( forceStartTime == null ) forceStartTime = parent.getForceStartTime();
        if ( secondsToLoot == null ) secondsToLoot = parent.getSecondsToLoot();
        if ( tableName == null ) tableName = parent.getDBTableName();
        if ( nLives == 0 ) nLives = parent.getNLives();
        if ( removePlayersOnLeave == null ) removePlayersOnLeave = parent.getRemovePlayersOnLeave();
        
        if ( closeWaitroomWhileRunning == null )
             closeWaitroomWhileRunning = parent.isWaitroomClosedWhenRunning();
        
        if ( cancelIfNotEnoughPlayers == null ) 
            cancelIfNotEnoughPlayers = parent.isCancelIfNotEnoughPlayers();
        
        if ( arenaCooldown == 0 ) arenaCooldown = parent.getArenaCooldown();
        if ( allowedTeamSizeDifference == 0 ) allowedTeamSizeDifference = parent.getAllowedTeamSizeDifference();
        
        if ( arenaDisplayName == null )  arenaDisplayName = parent.getDisplayName();
        
        arenaStateGraph = mergeChildWithParent( this, parent );
        
        if ( arenaSize == null && parent.getSize() != null ) arenaSize = new ArenaSize( parent.getSize() );

        if ( arenaTeamParams != null && parent.getTeamParams() != null ) {
            HashMap<Integer, MatchParams> tp = new HashMap<>( arenaTeamParams );
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
        else if ( parent.getTeamParams() != null ) {
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
        else if ( arenaTeamParams != null ) {
            HashMap<Integer, MatchParams> tp = new HashMap<>( arenaTeamParams );
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

    private StateGraph mergeChildWithParent( ArenaParams cap, ArenaParams pap ) {
        StateGraph mt = cap.arenaStateGraph == null ? new StateGraph() 
                                                    : new StateGraph( cap.arenaStateGraph );
        if ( pap != null ) {
            StateGraph.mergeChildWithParent( mt, mergeChildWithParent( pap, pap.parent ) );
        }
        return mt;
    }

    public void setStateGraph( StateGraph _stateGraph ) {
        arenaStateGraph = _stateGraph;
        clearMerged();
    }

    public String getPlayerRange() {
        return Util.rangeString( getMinPlayers(), getMaxPlayers() );
    }

    public boolean intersect( ArenaParams params ) {
        return  !getType().matches( params.getType() )
                || (    getSize() != null 
                        && params.getSize() != null 
                        && !getSize().matches( params.getSize() ) );
    }

    public boolean matches( JoinOptions jo ) {
        return matches( jo.getMatchParams() );
    }

    public boolean matches( ArenaParams ap ) {
        if ( type != null && ap.type != null && !type.matches(ap.type) ) return false;
        
        return arenaSize.matches( ap.getSize() ); 
    }

    public Collection<String> getInvalidMatchReasons(ArenaParams ap) {
        List<String> reasons = new ArrayList<>();
        if ( type == null ) reasons.add( "ArenaType is null" );
        
        if ( ap.type == null ) reasons.add( "Passed params have an arenaType of null" );
        else reasons.addAll( type.getInvalidMatchReasons( ap.getType() ) );
         
        if ( getSize() != null && ap.getSize() != null && !getSize().matchesNTeams( ap.getSize() ) ) {
            reasons.add( "Arena accepts numTeams=" + getSize().getNumTeamsString() +
                            ". You requested " + ap.getSize().getNumTeamsString() );
        }
        if ( getSize() != null && ap.getSize() != null && !getSize().matchesTeamSize( ap.getSize() ) ) {
            reasons.add( "Arena accepts teamSize=" + getSize().getTeamSizeString() + 
                            ". You requested " + ap.getSize().getTeamSizeString() );
        }
        return reasons;
    }

    public boolean valid() {
        return  type != null 
                && ( arenaSize == null || arenaSize.valid() );
    }

    public Collection<String> getInvalidReasons() {
        List<String> reasons = new ArrayList<>();
        if ( type == null ) reasons.add( "ArenaType is null" );
        
        if ( arenaSize != null && !arenaSize.valid() ) 
            reasons.addAll( arenaSize.getInvalidReasons() );
        return reasons;
    }

    public String getCommand() {
        return command != null ? command : ( parent != null ? parent.getCommand() : null );
    }
    public Boolean isRated(){
        return rated != null ? rated : ( parent != null ? parent.isRated() : null );
    }
    public Integer getSecondsToLoot() {
        return secondsToLoot != null ? secondsToLoot : ( parent != null ? parent.getSecondsToLoot() : null );
    }
    public Integer getSecondsTillMatch() {
        return secondsTillMatch != null ? secondsTillMatch : ( parent != null ? parent.getSecondsTillMatch() : null );
    }
    public Integer getTimeBetweenRounds() {
        return timeBetweenRounds != null ? timeBetweenRounds : ( parent != null ? parent.getTimeBetweenRounds() : null );
    }
    public String getDBTableName() {
        return tableName != null ? tableName : ( parent != null ? parent.getDBTableName() : null );
    }
    public String getName() {
        return name != null ? name : ( parent != null ? parent.getName() : null );
    }
    public ArenaSize getSize() {
        return arenaSize != null ? arenaSize : ( parent != null ? parent.getSize() : null );
    }
    public String toPrettyString() {
        return  getDisplayName() + ":" + type + ( arenaSize == null ? "" : arenaSize.toString() ) ;
    }
    private ChatColor getColor( Object o ) {
        return o == null ? ChatColor.GOLD : ChatColor.WHITE;
    }
    public String toSummaryString() {
        return  "&2&f" + name + "&2:&f" + type + ( arenaSize == null ? "" : arenaSize.toString() ) +
                "&5, timeUntilMatch=" + getColor( secondsTillMatch ) + getSecondsTillMatch() +
                "&5, matchTime=" + getColor( matchTime ) + getMatchTime() +
                "&5, secondsToLoot=" + getColor( secondsToLoot ) + getSecondsToLoot() + "\n" +
                "&crated=" + getColor( rated ) + isRated() + "&c, nLives=" + getColor( nLives ) + getNLives() + "&e";
    }
    public String getOptionsSummaryString() {
        return getStateGraph().getOptionString( arenaStateGraph );
    }
    @Override
    public String toString() {
        return  name + ":" + type 
                    + ( arenaSize == null ? "" : arenaSize.toString() )
                    + ( arenaStateGraph == null ? "" : arenaStateGraph.getOptionString());
    }
    public boolean isDuelOnly() {
        return getStateGraph().hasOptionAt( MatchState.DEFAULTS, TransitionOption.DUELONLY );
    }
    public boolean isAlwaysOpen(){
        return getStateGraph().hasOptionAt( MatchState.DEFAULTS, TransitionOption.ALWAYSOPEN );
    }
    public void setParent( ArenaParams newParent ) {
        parent = newParent;
        clearMerged();
    }
    protected void clearMerged() {
        mergedStateGraph = null;
        if ( arenaTeamParams != null ) {
            for ( MatchParams mp : arenaTeamParams.values() ) {
                mp.clearMerged();
            }
        }
    }
    public int getMinTeamSize() {
        return arenaSize != null ? arenaSize.minTeamSize : ( parent != null ? parent.getMinTeamSize() : 0 );
    }
    public int getMaxTeamSize() {
        return arenaSize != null ? arenaSize.maxTeamSize : ( parent != null ? parent.getMaxTeamSize() : 0 );
    }
    public int getMinTeams() {
        return arenaSize != null ? arenaSize.minTeams : ( parent != null ? parent.getMinTeams() : 0 );
    }
    public int getMaxTeams() {
        return arenaSize != null ? arenaSize.maxTeams : ( parent != null ? parent.getMaxTeams() : 0 );
    }
    public int getMaxPlayers() {
        int nt = getMaxTeams();
        int ts = getMaxTeamSize();       
        return  nt == ArenaSize.MAX || ts == ArenaSize.MAX ? ArenaSize.MAX : nt * ts;
    }
    public int getMinPlayers() {
        int nt = getMinTeams();
        int ts = getMinTeamSize();
        return nt == ArenaSize.MAX || ts == ArenaSize.MAX ? ArenaSize.MAX : nt * ts;
    }
    public void setNTeams(MinMax mm) {
        checkArenaSize();
        if ( mm == null )
            arenaSize.resetNTeams();
        else
            arenaSize.setNTeams( mm );
    }
    public void setTeamSize( MinMax mm ) {
        checkArenaSize();
        if ( mm == null )
            arenaSize.resetTeamSizes();
        else
            arenaSize.setTeamSizes( mm );
    }
    public void setTeamSize( int i ) {
        checkArenaSize();
        arenaSize.setTeamSize( i );
    }
    private void checkArenaSize() {
        if ( arenaSize == null ) arenaSize = new ArenaSize();
    }
    public void setMinTeamSize( int n ) {
        checkArenaSize();
        arenaSize.minTeamSize = n;
    }
    public void setMaxTeamSize( int n ) {
        checkArenaSize();
        arenaSize.maxTeamSize = n;
    }
    public void setMinTeams( int n ) {
        checkArenaSize();
        arenaSize.minTeams = n;
    }
    public void setMaxTeams( int n ) {
        checkArenaSize();
        arenaSize.maxTeams = n;
    }
    public boolean matchesTeamSize(int i) {
        return arenaSize != null ? arenaSize.matchesTeamSize( i ) : ( parent != null && parent.matchesTeamSize(i) );
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
        return getStateGraph().hasOption(option);
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
        return getStateGraph().hasAnyOption( TransitionOption.TELEPORTMAINWAITROOM, TransitionOption.TELEPORTWAITROOM );
    }
    public boolean needsSpectate() {
        return getStateGraph().hasOption( TransitionOption.TELEPORTSPECTATE );
    }
    public boolean needsLobby() {
        return getStateGraph().hasAnyOption( TransitionOption.TELEPORTMAINLOBBY, TransitionOption.TELEPORTLOBBY );
    }
    public Boolean isWaitroomClosedWhenRunning(){
        return closeWaitroomWhileRunning != null ? closeWaitroomWhileRunning 
                                                 : ( parent != null ? parent.isWaitroomClosedWhenRunning() : null);
    }
    public Boolean isCancelIfNotEnoughPlayers(){
        return cancelIfNotEnoughPlayers != null ? cancelIfNotEnoughPlayers 
                                                : ( parent != null ? parent.isCancelIfNotEnoughPlayers() : null );
    }
    public String getDisplayName() {
        return arenaDisplayName != null ? arenaDisplayName : ( parent != null ? parent.getDisplayName() : getName());
    }
    public int getQueueCount() {
        return BattleArena.getBAController().getArenaMatchQueue().getQueueCount( this );
    }
    public int getArenaCooldown() {
        return arenaCooldown != 0 ? arenaCooldown : ( parent != null ? parent.getArenaCooldown() : 0 );
    }
    public int getAllowedTeamSizeDifference() {
        return allowedTeamSizeDifference != 0 ? allowedTeamSizeDifference 
                                              : ( parent != null ? parent.getAllowedTeamSizeDifference() : 0 );
    }
    public Integer getForceStartTime() {
        return forceStartTime != null ? forceStartTime : ( parent != null ? parent.getForceStartTime() : null );
    }
    public Integer getMatchTime() {
        return ( matchTime == null && parent != null ) ? parent.getMatchTime() : matchTime;
    }
    public int getNLives() {
        return nLives != 0 ? nLives : parent != null ? parent.getNLives() : 0;
    }
    public Boolean getRemovePlayersOnLeave() {
        return removePlayersOnLeave == null && parent != null ? parent.getRemovePlayersOnLeave() : removePlayersOnLeave;
    }
    public Map<Integer, MatchParams> getTeamParams() {
        return arenaTeamParams == null && parent != null ? parent.getTeamParams() : arenaTeamParams;
    }

    public MatchParams getTeamParams(int index) {
        if ( arenaTeamParams != null ) {
            MatchParams mp = arenaTeamParams.get( index );
            if ( mp != null ) return mp;
        }
        if ( parent != null ) {
            return parent.getTeamParams( index );
        }
        return null;
    }

    public StateGraph getStateGraph() {
        if ( mergedStateGraph != null ) return mergedStateGraph;
        
        if ( arenaStateGraph == null && parent != null )
            /// this is a bit hard to keep synced, but worth it for the speed improvements
            mergedStateGraph = parent.getStateGraph();
        else 
            mergedStateGraph = mergeChildWithParent( this, parent );
        
        return mergedStateGraph;
    }

    public StateOptions getStateOptions( CompetitionState state ) {
        return getStateGraph().getOptions( state );
    }
}
