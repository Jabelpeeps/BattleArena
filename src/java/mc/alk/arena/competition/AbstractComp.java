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
import mc.alk.arena.controllers.PlayerController;
import mc.alk.arena.controllers.RoomController;
import mc.alk.arena.controllers.TeamController;
import mc.alk.arena.controllers.joining.AbstractJoinHandler;
import mc.alk.arena.controllers.joining.TeamJoinFactory;
import mc.alk.arena.controllers.messaging.EventMessageImpl;
import mc.alk.arena.controllers.messaging.EventMessager;
import mc.alk.arena.controllers.messaging.MessageHandler;
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
import mc.alk.arena.objects.messaging.EventMessageHandler;
import mc.alk.arena.objects.options.StateOptions;
import mc.alk.arena.objects.options.TransitionOption;
import mc.alk.arena.objects.pairs.JoinResult;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.util.Countdown;
import mc.alk.util.Countdown.CountdownCallback;
import mc.alk.util.Log;
import mc.alk.util.MessageUtil;
import mc.alk.util.PermissionsUtil;


public abstract class AbstractComp extends Competition implements CountdownCallback, ArenaListener {
    @Getter final String name; 
    @Getter protected EventParams params; 
    EventMessager mc; 
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
        params = _params;
        transitionTo(EventState.CLOSED);
        name = _params.getName();
        teamJoinHandler = TeamJoinFactory.createTeamJoinHandler(_params, this);
        if (mc == null)
            mc = new EventMessager(this);
        mc.setMessageHandler(new EventMessageImpl(this));
    }

    public void openEvent() {
        teams.clear();
        EventOpenEvent event = new EventOpenEvent(this);
        callEvent(event);
        if (event.isCancelled())
            return;

        stopTimer();
        transitionTo(EventState.OPEN);
        mc.sendEventOpenMsg();
    }

    public void autoEvent(){
        openEvent();
        mc.sendCountdownTillEvent(params.getSecondsTillStart());
        timer = new Countdown(BattleArena.getSelf(),(long)params.getSecondsTillStart(),
                (long)params.getAnnouncementInterval(), this);
    }

    public void addAllOnline() {

        for (Player p: Bukkit.getOnlinePlayers()){
            if (PermissionsUtil.isAdmin(p)) { /// skip admins (they are doin' importantz thingz)
                continue;}
            ArenaTeam t = TeamController.createTeam(params, PlayerController.toArenaPlayer(p));
            TeamJoinObject tqo = new TeamJoinObject(t,params,null);
            this.joining(tqo);
        }
    }

    /**
     * Add an arena listener for this competition
     * @param arenaListener ArenaListener
     */
    @Override
    public void addArenaListener(ArenaListener arenaListener){
        methodController.addListener(arenaListener);
    }

    /**
     * Remove an arena listener for this competition
     * @param arenaListener ArenaListener
     */
    @Override
    public boolean removeArenaListener(ArenaListener arenaListener){
        return methodController.removeListener(arenaListener);
    }

    public void startEvent() {
        List<ArenaTeam> improper = teamJoinHandler.removeImproperTeams();
        for (ArenaTeam t: improper){
            t.sendMessage("&cYour team has been excluded to having an improper team size");
        }
        /// TODO rebalance teams
        Set<ArenaPlayer> excludedPlayers = getExcludedPlayers();
        for (ArenaPlayer p : excludedPlayers){
            p.sendMessage(Log.colorChat(params.getPrefix()+
                    "&6 &5There werent enough players to create a &6" + params.getMinTeamSize() +"&5 person team"));
        }
        transitionTo(EventState.RUNNING);

        callEvent(new EventStartEvent(this,teams));
    }

    protected void setEventResult(MatchResult result, boolean announce) {
        if (announce){
            if (result.hasVictor()){
                mc.sendEventVictory(result.getVictors(), result.getLosers());
            } else {
                mc.sendEventDraw(result.getDrawers(), result.getLosers());
            }
        }
        callEvent(new EventResultEvent(this,result));
    }

    public void stopTimer(){
        if (timer != null){
            timer.stop();
            timer = null;
        }
    }

    public void cancelEvent() {
        eventCancelled();
    }

    public void eventCompleted(){
        callEvent(new EventCompletedEvent(this));
        endEvent();
    }

    protected void eventCancelled(){
        stopTimer();
        List<ArenaTeam> newTeams = new ArrayList<>(teams);
        callEvent(new EventCancelEvent(this));
        mc.sendEventCancelled(newTeams);
        endEvent();
    }

    protected void endEvent() {
        if (state == EventState.CLOSED)
            return;
        transitionTo(EventState.CLOSED);
        if (Defaults.DEBUG_TRACE) Log.info("BAEvent::endEvent");
        stopTimer();

        removeAllTeams();
        teams.clear();
        teamJoinHandler = null;
        callEvent(new EventFinishedEvent(this));
        HandlerList.unregisterAll(this);
    }

    public boolean canJoin(){
        return isOpen();
    }

    public boolean canJoin(ArenaTeam t){
        return isOpen();
    }

    @Override
    public abstract boolean canLeave(ArenaPlayer p);

    @Override
    protected void transitionTo(CompetitionState state){
        this.state = (EventState) state;
        times.put(this.state, System.currentTimeMillis());
    }

    @Override
    public Long getTime(CompetitionState state){
        return times.get(state);
    }

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
        AbstractJoinHandler.TeamJoinResult tjr = teamJoinHandler.joiningTeam(tqo);
        switch(tjr.joinStatus){
            case ADDED_TO_EXISTING: /* drop down into added */
            case ADDED:
                for (ArenaPlayer player: tqo.getTeam().getPlayers()){
                    player.addCompetition(this);}
                mc.sendTeamJoined(tqo.getTeam());
                break;
            case ADDED_STILL_NEEDS_PLAYERS:
                mc.sendWaitingForMorePlayers(team, tjr.remaining);
                for (ArenaPlayer player: tqo.getTeam().getPlayers()){
                    player.addCompetition(this);}
                break;
            case CANT_FIT:
                mc.sendCantFitTeam(team);
                break;
        }

        return null;
    }

    public String getCommand(){return params.getCommand();}
    public String getDisplayName() { return getName(); }
    public boolean isRunning() {return state == EventState.RUNNING;}
    public boolean isOpen() {return state == EventState.OPEN;}
    public boolean isClosed() {return state == EventState.CLOSED;}
    public boolean isFinished() {return state== EventState.FINISHED;}

    public int getNTeams() {
        int size = 0;
        for (ArenaTeam t: teams){
            if (t.size() > 0)
                size++;
        }
        return size;
    }

    /**
     * Set a Message handler to override default Event messages
     * @param handler EventMessageHandler
     */
    public void setMessageHandler(EventMessageHandler handler){
        mc.setMessageHandler(handler);
    }

    /**
     * Return the Message Handler for this Event
     * @return EventMessageHandler
     */
    public EventMessageHandler getMessageHandler(){
        return mc.getMessageHandler();
    }

    public abstract String getResultString();

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

    public String getInfo() {
        return StateOptions.getInfo(params, params.getName());
    }

    public boolean canLeaveTeam(ArenaPlayer p) {return canLeave(p);}

    /**
     * Broadcast to all players in the Event
     */
    public void broadcast(String msg){for (ArenaTeam t : teams){t.sendMessage(msg);}}

    public Long getTimeTillStart() {
        if (timer == null)
            return null;
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
                mc.sendEventCancelledDueToLackOfPlayers(getPlayers());
                cancelEvent();
            }
        } else {
            mc.sendCountdownTillEvent(remaining);
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

    public void setSilent(boolean silent) {
        mc.setSilent(silent);
    }

    @Override
    public String toString(){
        return "[" + getName()+":"+id+"]";
    }

    public boolean waitingToJoin(ArenaPlayer p) {
        return teamJoinHandler != null && teamJoinHandler.getExcludedPlayers().contains(p);
    }

    public boolean hasEnoughTeams() {
        return getNTeams() >= params.getMinTeams();
    }

    public boolean hasEnough() {
        return teamJoinHandler != null && teamJoinHandler.hasEnough(Integer.MAX_VALUE);
    }

    @Override
    public void addedToTeam(ArenaTeam team, Collection<ArenaPlayer> players) {/* do nothing */}
    @Override
    public void addedToTeam(ArenaTeam team, ArenaPlayer player) {/* do nothing */}
    @Override
    public void removedFromTeam(ArenaTeam team, Collection<ArenaPlayer> players) {/* do nothing */}
    @Override
    public void removedFromTeam(ArenaTeam team, ArenaPlayer player) {/* do nothing */}
    @Override
    public void onPreJoin(ArenaPlayer player, ArenaPlayerTeleportEvent apte) {/* do nothing */}
    @Override
    public void onPostJoin(ArenaPlayer player, ArenaPlayerTeleportEvent apte) {/* do nothing */}
    @Override
    public void onPreQuit(ArenaPlayer player, ArenaPlayerTeleportEvent apte) {/* do nothing */}
    @Override
    public void onPostQuit(ArenaPlayer player, ArenaPlayerTeleportEvent apte) {
        player.removeCompetition(this);
    }
    @Override
    public void onPreEnter(ArenaPlayer player, ArenaPlayerTeleportEvent apte) {/* do nothing */}
    @Override
    public void onPostEnter(ArenaPlayer player,ArenaPlayerTeleportEvent apte) {/* do nothing */}
    @Override
    public void onPreLeave(ArenaPlayer player, ArenaPlayerTeleportEvent apte) {/* do nothing */}
    @Override
    public void onPostLeave(ArenaPlayer player, ArenaPlayerTeleportEvent apte) {/* do nothing */}

    @EventHandler( priority=EventPriority.MONITOR )
    public void onArenaPlayerLeaveEvent(ArenaPlayerLeaveEvent event){
        if (hasPlayer(event.getPlayer())) {
            event.addMessage(MessageHandler.getSystemMessage("you_left_event", this.getName()));
            leave(event.getPlayer());
        }
    }

    @Override
    public boolean hasOption(TransitionOption option) {
        return getParams().hasOptionAt(state, option);
    }
}
