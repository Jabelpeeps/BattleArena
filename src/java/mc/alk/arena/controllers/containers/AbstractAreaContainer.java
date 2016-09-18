package mc.alk.arena.controllers.containers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.competition.TransitionController;
import mc.alk.arena.events.players.ArenaPlayerLeaveEvent;
import mc.alk.arena.events.players.ArenaPlayerLeaveLobbyEvent;
import mc.alk.arena.listeners.PlayerHolder;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.CompetitionState;
import mc.alk.arena.objects.ContainerState;
import mc.alk.arena.objects.MatchState;
import mc.alk.arena.objects.events.ArenaEventHandler;
import mc.alk.arena.objects.messaging.MessageHandler;
import mc.alk.arena.objects.options.StateOptions;
import mc.alk.arena.objects.spawns.SpawnLocation;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.objects.teams.TeamHandler;
import mc.alk.arena.util.TeamUtil;

public abstract class AbstractAreaContainer extends PlayerHolder implements TeamHandler {
    
    public static final PlayerHolder HOMECONTAINER = new AbstractAreaContainer( "home" ) {
        @Override
        public PlayerHolder.LocationType getLocationType() { return PlayerHolder.LocationType.HOME; }
        @Override
        public ArenaTeam getTeam(ArenaPlayer player) { return null; }
    };

    @Getter @Setter protected String name;
    @Setter protected String displayName;
    @Getter @Setter ContainerState containerState = ContainerState.OPEN;

    boolean disabledAllCommands;
    Set<String> disabledCommands;
    Set<String> enabledCommands;

    final protected Set<UUID> players = new HashSet<>();
    /** Spawn points */
    @Getter final protected List<List<SpawnLocation>> spawns = new ArrayList<>();
    protected List<SpawnLocation> allSpawns  = new ArrayList<>();
    /** Main Spawn is different than the normal spawns.  It is specified by Defaults.MAIN_SPAWN */
    @Getter SpawnLocation mainSpawn;
    /** Our teams */
    final protected List<ArenaTeam> teams = Collections.synchronizedList(new ArrayList<ArenaTeam>());
    /** our values for the team index, only used if the Team.getIndex is null*/
    final Map<ArenaTeam,Integer> teamIndexes = new ConcurrentHashMap<>();
    final static Random r = new Random();

    public AbstractAreaContainer( String _name ) {
        name = _name;
        methodController.addAllEvents( this );
        Bukkit.getPluginManager().registerEvents( this, BattleArena.getSelf() );    
    }

    public void playerLeaving( ArenaPlayer player ) { methodController.updateEvents( MatchState.ONLEAVE, player ); }
    protected void playerJoining( ArenaPlayer player ) { methodController.updateEvents( MatchState.ONENTER, player ); }
    protected void teamLeaving( ArenaTeam team ) {
        if ( teams.remove( team ) )
            methodController.updateEvents( MatchState.ONLEAVE, team.getPlayers() );
    }

    public boolean teamJoining( ArenaTeam team ) {
        teams.add( team );
        teamIndexes.put( team, teams.size() );
        for ( ArenaPlayer ap : team.getPlayers() ) {
            doTransition( MatchState.ONJOIN, ap, team, true );
        }
        return true;
    }

    /**
     * Tekkit Servers don't get the @EventHandler methods (reason unknown) so have this method be
     * redundant.  Possibly can now simplify to just the @ArenaEventHandler
     * @param event ArenaPlayerLeaveEvent
     */
    @ArenaEventHandler
    public void onArenaPlayerLeaveEvent( ArenaPlayerLeaveEvent event ) { _onArenaPlayerLeaveEvent( event ); }

    @EventHandler
    public void _onArenaPlayerLeaveEvent( ArenaPlayerLeaveEvent event ) {
        if ( players.remove( event.getPlayer().getUniqueId() ) ) {
            updateBukkitEvents( MatchState.ONLEAVE, event.getPlayer() );
            callEvent( new ArenaPlayerLeaveLobbyEvent( event.getPlayer(), event.getTeam() ) );
            event.addMessage( MessageHandler.getSystemMessage( "you_left_competition", params.getName() ) );
            event.getPlayer().reset();
        }
    }

    protected void doTransition( MatchState state, ArenaPlayer player, ArenaTeam team, boolean onlyInMatch ) {
        if ( player != null )
            TransitionController.transition( this, state, player, team, onlyInMatch );
        else
            TransitionController.transition( this, state, team, onlyInMatch );
    }

    @Override
    public boolean canLeave(ArenaPlayer p) { return false; }
    @Override
    public boolean leave(ArenaPlayer p) { return players.remove( p.getUniqueId() ); }
    @Override
    public boolean isHandled(ArenaPlayer p) { return players.contains( p.getUniqueId() ); }
    @Override
    public CompetitionState getState() { return MatchState.INLOBBY; }

    @Override
    public boolean checkReady( ArenaPlayer player, ArenaTeam team, StateOptions mo ) {
        return params.getStateGraph().playerReady( player, null );
    }

    @Override
    public SpawnLocation getSpawn( int index, boolean random ) {
        if ( index == Defaults.MAIN_SPAWN )
            return mainSpawn != null ? mainSpawn 
                                     : ( spawns.size() == 1 ? spawns.get(0).get(0) 
                                                            : null );
        if ( random ) {
            if ( allSpawns == null ) buildAllSpawns();
            
            return allSpawns == null ? null 
                                     : allSpawns.get( r.nextInt( allSpawns.size() ) );
        }
        List<SpawnLocation> l = index >= spawns.size() ? spawns.get( index % spawns.size() ) 
                                                       : spawns.get( index );
        return l.get( r.nextInt( l.size() ) );
    }

    public SpawnLocation getSpawn( int teamIndex, int spawnIndex ) {
        List<SpawnLocation> l = teamIndex >= spawns.size() ? null 
                                                           : spawns.get( teamIndex );
        return l == null || spawnIndex >= l.size() ? null 
                                                   : l.get( spawnIndex );
    }

    private void buildAllSpawns() {
        if ( spawns.isEmpty() ) return;
        
        for ( List<SpawnLocation> spawn : spawns ) {
            allSpawns.addAll( spawn );
        }
    }

    /**
     * Set the spawn location for the team with the given index
     * @param teamIndex index of which team to add this spawn to
     * @param spawnIndex which spawn to set
     * @param loc SpawnLocation
     */
    public void setSpawnLoc( int teamIndex, int spawnIndex, SpawnLocation loc ) throws IllegalStateException {
        if ( teamIndex == Defaults.MAIN_SPAWN ) {
            mainSpawn = loc;
        } 
        else if ( spawns.size() > teamIndex ) {
            List<SpawnLocation> list = spawns.get(teamIndex);
            
            if ( list.size() > spawnIndex )
                list.set( spawnIndex, loc );
            else if ( list.size() == spawnIndex )
                list.add( loc );
            else 
                throw new IllegalStateException( "You must set team spawn " + ( list.size() + 1 ) + " first" );
        } 
        else if ( spawns.size() == teamIndex ) {
            ArrayList<SpawnLocation> list = new ArrayList<>();
            
            spawns.add( list );
            
            if ( spawnIndex > 0 ) {
                throw new IllegalStateException( "You must set spawn #1 for the " + 
                                                    TeamUtil.getTeamName( teamIndex ) + " team first" ); 
            }
            list.add( loc );
        } 
        else 
            throw new IllegalStateException( "You must set spawn " + ( spawns.size() + 1 ) + " first" );
    }
    
    public boolean validIndex(int index){ return spawns.size() < index; }
    public String getDisplayName() { return displayName == null ? name : displayName; }
    public boolean isOpen() { return containerState.isOpen(); }
    public boolean isClosed() { return containerState.isClosed(); }
    public String getContainerMessage() { return containerState.getMsg(); }
}
