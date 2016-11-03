package mc.alk.arena.competition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.Permissions;
import mc.alk.arena.controllers.PlayerController;
import mc.alk.arena.controllers.RoomController;
import mc.alk.arena.controllers.TeamController;
import mc.alk.arena.controllers.joining.AbstractJoinHandler;
import mc.alk.arena.controllers.joining.TeamJoinFactory;
import mc.alk.arena.events.events.EventCancelEvent;
import mc.alk.arena.events.events.EventCompletedEvent;
import mc.alk.arena.events.events.EventFinishedEvent;
import mc.alk.arena.events.events.EventOpenEvent;
import mc.alk.arena.events.events.EventResultEvent;
import mc.alk.arena.events.events.EventStartEvent;
import mc.alk.arena.events.events.TeamJoinedEvent;
import mc.alk.arena.events.players.ArenaPlayerLeaveEvent;
import mc.alk.arena.events.players.ArenaPlayerTeleportEvent;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.CompetitionState;
import mc.alk.arena.objects.EventParams;
import mc.alk.arena.objects.EventState;
import mc.alk.arena.objects.MatchResult;
import mc.alk.arena.objects.arenas.ArenaListener;
import mc.alk.arena.objects.exceptions.NeverWouldJoinException;
import mc.alk.arena.objects.joining.TeamJoinObject;
import mc.alk.arena.objects.messaging.EventMessenger;
import mc.alk.arena.objects.messaging.MessageHandler;
import mc.alk.arena.objects.options.StateOptions;
import mc.alk.arena.objects.pairs.JoinResult;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.util.Countdown;
import mc.alk.arena.util.Countdown.CountdownCallback;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.MessageUtil;


public abstract class AbstractComp extends Competition implements CountdownCallback, ArenaListener {
    @Getter final String name; 
    @Getter @Setter EventMessenger messenger; 
    Countdown timer; 
    @Setter protected AbstractJoinHandler teamJoinHandler; 
    @Getter protected EventState state; 

    /// When did each transition occur
    final Map<EventState, Long> times = new EnumMap<>(EventState.class);

    /**
     * Create our event from the specified paramaters
     * @param _params EventParams
     */
    public AbstractComp(EventParams _params) throws NeverWouldJoinException {
        super();
        params = _params;
        transitionTo(EventState.CLOSED);
        name = _params.getName();
        teamJoinHandler = TeamJoinFactory.createTeamJoinHandler(_params, this);
        messenger = new EventMessenger(this);
    }
    
    public abstract String getResultString();
    
    public void openEvent() {
        teams.clear();
        EventOpenEvent event = new EventOpenEvent(this);
        callEvent(event);
        if (event.isCancelled())
            return;

        stopTimer();
        transitionTo(EventState.OPEN);
        messenger.sendEventOpenMsg();
    }

    public void autoEvent() {
        openEvent();
        EventParams eParams = (EventParams) params;
        messenger.sendCountdownTillEvent(eParams.getSecondsTillStart());
        timer = new Countdown(BattleArena.getSelf(),(long)eParams.getSecondsTillStart(),
                (long)eParams.getAnnouncementInterval(), this);
    }

    public void addAllOnline() {

        for (Player p: Bukkit.getOnlinePlayers()){
            if (Permissions.isAdmin(p)) continue;

            ArenaTeam t = TeamController.createTeam(params, PlayerController.toArenaPlayer(p));
            TeamJoinObject tqo = new TeamJoinObject(t,params,null);
            joining(tqo);
        }
    }

    public void startEvent() {
        List<ArenaTeam> improper = teamJoinHandler.removeImproperTeams();
        for (ArenaTeam t: improper){
            t.sendMessage("&cYour team has been excluded to having an improper team size");
        }
        /// TODO rebalance teams
        Set<ArenaPlayer> excludedPlayers = getExcludedPlayers();
        for (ArenaPlayer p : excludedPlayers){
            p.sendMessage( MessageUtil.colorChat( params.getPrefix() +
                    "&6 &5There werent enough players to create a &6" + params.getMinTeamSize() + "&5 person team"));
        }
        transitionTo(EventState.RUNNING);

        callEvent(new EventStartEvent(this,teams));
    }

    protected void setEventResult(MatchResult result, boolean announce) {
        if ( announce ) {
            if ( result.hasVictor() )
                messenger.sendEventVictory( result.getVictors(), result.getLosers() );
            else
                messenger.sendEventDraw( result.getDrawers(), result.getLosers() );
        }
        callEvent( new EventResultEvent( this, result ) );
    }

    public void stopTimer(){
        if (timer != null){
            timer.stop();
            timer = null;
        }
    }

    public void eventCompleted(){
        callEvent(new EventCompletedEvent(this));
        endEvent();
    }
    
    public void cancelEvent() {
        stopTimer();
        List<ArenaTeam> newTeams = new ArrayList<>(teams);
        callEvent(new EventCancelEvent(this));
        messenger.sendEventCancelled(newTeams);
        endEvent();
    }

    protected void endEvent() {
        if (state == EventState.CLOSED) return;
        
        transitionTo(EventState.CLOSED);
        
        if (Defaults.DEBUG_EVENTS) Log.info("BAEvent::endEvent");
        stopTimer();

        removeAllTeams();
        teams.clear();
        teamJoinHandler = null;
        callEvent(new EventFinishedEvent(this));
        HandlerList.unregisterAll(this);
    }

    @Override
    protected void transitionTo(CompetitionState _state){
        state = (EventState) _state;
        times.put( state, System.currentTimeMillis() );
    }

    @Override
    public Long getTime( CompetitionState _state ) { return times.get( _state ); }

    /**
     * Called when a player leaves minecraft.. we cant stop them so deal with it
     */
    @Override
    public boolean leave(ArenaPlayer p) {
        ArenaTeam t = getTeam(p);
        p.removeCompetition(this);
        
        if (params.needsLobby()){
            RoomController.leaveLobby(params, p);
        }
        if (t==null) /// they arent in this Event
            return false;
        t.playerLeft(p);
        return true;
    }

    public void removeAllTeams(){

        for (ArenaTeam t: teams){
            for (ArenaPlayer p: t.getPlayers()){
                p.removeCompetition(this);
            }
        }
        teams.clear();
    }

    @Override
    public boolean removedTeam(ArenaTeam team){
        if (teams.remove(team)){
            for (ArenaPlayer p: team.getPlayers()){
                p.removeCompetition(this);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean addedTeam(ArenaTeam team){
        if (teams.contains(team)) 
            return true;
        callEvent(new TeamJoinedEvent(this,team));
        return teams.add(team);
    }

    /**
     * Called when a team wants to add
     * @param tqo TeamJoinObject that is joining
     * @return where the team ended up
     */
    public JoinResult.JoinStatus joining(TeamJoinObject tqo){
        JoinResult.JoinStatus js;
        ArenaTeam team = tqo.getTeam();
        if (teamJoinHandler == null) {
            js = JoinResult.JoinStatus.NOTOPEN;
            return js;
        }
        AbstractJoinHandler.TeamJoinResult tjr = teamJoinHandler.joiningTeam( tqo );
        switch( tjr.joinStatus ) {
            case ADDED:
                for ( ArenaPlayer player : tqo.getTeam().getPlayers() ) {
                    player.addCompetition( this );
                }
                messenger.sendTeamJoinedEvent( tqo.getTeam() );
                break;
                
            case STILL_NEEDS_PLAYERS:
                messenger.sendWaitingForMorePlayers( team, tjr.remaining );
                for (ArenaPlayer player : tqo.getTeam().getPlayers() ) {
                    player.addCompetition( this );
                }
                break;
                
            case CANT_FIT:
                messenger.sendCantFitTeam(team);
                break;
        }
        return null;
    }

    public String getCommand() { return params.getCommand(); }
    public String getDisplayName() { return getName(); }
    public boolean isRunning() { return state == EventState.RUNNING; }
    public boolean isOpen() { return state == EventState.OPEN; }
    public boolean isClosed() { return state == EventState.CLOSED; }
    public boolean isFinished() { return state == EventState.FINISHED; }

    public int getNTeams() {
        int size = 0;
        for (ArenaTeam t: teams){
            if (t.size() > 0)
                size++;
        }
        return size;
    }

    protected Set<ArenaPlayer> getExcludedPlayers() {
        return teamJoinHandler == null ? null :  teamJoinHandler.getExcludedPlayers();
    }

    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        if (params != null){
            boolean rated = params.isRated();
            sb.append(rated ? "&4Rated" : "&aUnrated").append("&e ").append(name).append(". ");
            sb.append("&e(&6").append(state).append("&e)");
            sb.append("&eTeam size=").append(params.getTeamSize());
            sb.append("&e Teams=&6 ").append(teams.size());
        }
        if (state == EventState.OPEN && teamJoinHandler != null){
            sb.append("\n&eJoiningTeams: ").append(MessageUtil.joinPlayers(teamJoinHandler.getExcludedPlayers(), ", "));
        }
        return sb.toString();
    }

    public String getInfo() { return StateOptions.getInfo( params, params.getName() ); }

    /**
     * Broadcast to all players in the Event
     */
    public void broadcast( String msg ) {
        for ( ArenaTeam t : teams ) {
            t.sendMessage( msg );
        }
    }

    public Long getTimeTillStart() {
        if ( timer == null ) return null;
        return timer.getTimeRemaining();
    }

    @Override
    public boolean intervalTick(int remaining){
        if (!isOpen())
            return false;
        if (remaining == 0){
            if (this.hasEnough() ){
                startEvent();
            } else {
                messenger.sendEventCancelledDueToLackOfPlayers(getPlayers());
                cancelEvent();
            }
        } else {
            messenger.sendCountdownTillEvent(remaining);
        }
        return true;
    }

    /**
     * Get all players in the Event
     * if Event is open will return those players still waiting for a team as well
     * @return players
     */
    @Override
    public Set<ArenaPlayer> getPlayers() {
        Set<ArenaPlayer> players = new HashSet<>();
        for (ArenaTeam t: getTeams()){
            players.addAll(t.getPlayers());}
        if (isOpen() && teamJoinHandler != null){
            players.addAll(teamJoinHandler.getExcludedPlayers());
        }
        return players;
    }

    @Override
    public String toString() { return "[" + getName() + ":" + id + "]"; }

    public boolean waitingToJoin(ArenaPlayer p) {
        return teamJoinHandler != null && teamJoinHandler.getExcludedPlayers().contains(p);
    }

    public boolean hasEnoughTeams() { return getNTeams() >= params.getMinTeams(); }
    public void setSilent( boolean silent ) { messenger.setSilent( silent ); }
    public boolean hasEnough() { return teamJoinHandler != null && teamJoinHandler.hasEnough(Integer.MAX_VALUE); }

    @Override
    public void addedToTeam(ArenaTeam team, Collection<ArenaPlayer> players) { }
    @Override
    public void addedToTeam(ArenaTeam team, ArenaPlayer player) { }
    @Override
    public void removedFromTeam(ArenaTeam team, Collection<ArenaPlayer> players) { }
    @Override
    public void removedFromTeam(ArenaTeam team, ArenaPlayer player) { }
    @Override
    public void onPostQuit( ArenaPlayer player, ArenaPlayerTeleportEvent apte ) {
        player.removeCompetition( this );
    }

    @EventHandler( priority = EventPriority.MONITOR )
    public void onArenaPlayerLeaveEvent( ArenaPlayerLeaveEvent event ) {
        if ( hasPlayer( event.getPlayer() ) ) {
            event.addMessage( MessageHandler.getSystemMessage( "you_left_event", getName() ) );
            leave( event.getPlayer() );
        }
    }
}
