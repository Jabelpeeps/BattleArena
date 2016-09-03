package mc.alk.arena.competition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.controllers.ArenaAlterController.ChangeType;
import mc.alk.arena.controllers.ArenaController;
import mc.alk.arena.controllers.ListenerAdder;
import mc.alk.arena.controllers.ParamController;
import mc.alk.arena.controllers.PlayerStoreController;
import mc.alk.arena.controllers.RewardController;
import mc.alk.arena.controllers.Scheduler;
import mc.alk.arena.controllers.containers.GameManager;
import mc.alk.arena.controllers.joining.AbstractJoinHandler;
import mc.alk.arena.controllers.tracker.TrackerInterface;
import mc.alk.arena.events.matches.MatchCancelledEvent;
import mc.alk.arena.events.matches.MatchCompletedEvent;
import mc.alk.arena.events.matches.MatchFindCurrentLeaderEvent;
import mc.alk.arena.events.matches.MatchFinishedEvent;
import mc.alk.arena.events.matches.MatchOpenEvent;
import mc.alk.arena.events.matches.MatchPrestartEvent;
import mc.alk.arena.events.matches.MatchResultEvent;
import mc.alk.arena.events.matches.MatchStartEvent;
import mc.alk.arena.events.matches.MatchTimerIntervalEvent;
import mc.alk.arena.events.players.ArenaPlayerDeathEvent;
import mc.alk.arena.events.players.ArenaPlayerLeaveEvent;
import mc.alk.arena.events.players.ArenaPlayerTeleportEvent;
import mc.alk.arena.events.prizes.ArenaDrawersPrizeEvent;
import mc.alk.arena.events.prizes.ArenaLosersPrizeEvent;
import mc.alk.arena.events.prizes.ArenaPrizeEvent;
import mc.alk.arena.events.prizes.ArenaWinnersPrizeEvent;
import mc.alk.arena.events.teams.TeamDeathEvent;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.ArenaSize;
import mc.alk.arena.objects.CompetitionSize;
import mc.alk.arena.objects.CompetitionState;
import mc.alk.arena.objects.ContainerState;
import mc.alk.arena.objects.LocationType;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.MatchResult;
import mc.alk.arena.objects.MatchState;
import mc.alk.arena.objects.StateGraph;
import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.objects.arenas.ArenaListener;
import mc.alk.arena.objects.events.ArenaEventHandler;
import mc.alk.arena.objects.messaging.Channels;
import mc.alk.arena.objects.messaging.MatchMessager;
import mc.alk.arena.objects.messaging.MessageHandler;
import mc.alk.arena.objects.modules.ArenaModule;
import mc.alk.arena.objects.options.StateOptions;
import mc.alk.arena.objects.options.TransitionOption;
import mc.alk.arena.objects.scoreboard.ArenaObjective;
import mc.alk.arena.objects.scoreboard.ArenaScoreboard;
import mc.alk.arena.objects.scoreboard.SAPIDisplaySlot;
import mc.alk.arena.objects.scoreboard.SEntry;
import mc.alk.arena.objects.spawns.SpawnLocation;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.objects.tracker.WLTRecord.WLT;
import mc.alk.arena.objects.victoryconditions.NLives;
import mc.alk.arena.objects.victoryconditions.NoTeamsLeft;
import mc.alk.arena.objects.victoryconditions.OneTeamLeft;
import mc.alk.arena.objects.victoryconditions.TeamTimeLimit;
import mc.alk.arena.objects.victoryconditions.TimeLimit;
import mc.alk.arena.objects.victoryconditions.VictoryCondition;
import mc.alk.arena.objects.victoryconditions.VictoryType;
import mc.alk.arena.objects.victoryconditions.interfaces.DefinesNumLivesPerPlayer;
import mc.alk.arena.objects.victoryconditions.interfaces.DefinesNumTeams;
import mc.alk.arena.objects.victoryconditions.interfaces.DefinesTimeLimit;
import mc.alk.arena.objects.victoryconditions.interfaces.ScoreTracker;
import mc.alk.arena.plugins.BTInterface;
import mc.alk.arena.plugins.HeroesController;
import mc.alk.arena.plugins.WorldGuardController;
import mc.alk.arena.tracker.Tracker;
import mc.alk.arena.util.Countdown;
import mc.alk.arena.util.InventoryUtil;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.MessageUtil;
import mc.alk.arena.util.TeamUtil;

public abstract class Match extends Competition implements Runnable, ArenaController {

    public enum PlayerState{OUTOFMATCH,INMATCH}

    @Getter final MatchParams params; 
    @Getter final Arena arena; 

    @Getter MatchState state = MatchState.NONE;

    /// When did each transition occur
    final Map<MatchState, Long> times = Collections.synchronizedMap(new EnumMap<MatchState,Long>(MatchState.class));
    @Getter final List<VictoryCondition> victoryConditions = new ArrayList<>();
    @Getter @Setter MatchResult matchResult; 

    final StateGraph tops; 
    final PlayerStoreController psc = new PlayerStoreController(); 

    Set<MatchState> waitRoomStates = null; 
    final CompetitionState tinState; /// which matchstat teleports players in (first one is chosen)
    Long joinCutoffTime = null; /// at what point do we cut people off from joining
    Integer currentTimer = null; /// Our current timer (for switching between states)

    Countdown startCountdown = null; 
    Countdown matchCountdown = null; 

    private final Collection<ArenaPlayer> inGamePlayers = new HashSet<>();

    /** who is still inside arena area. (does not include waitrooms, etc). Used for event handling */
    final Set<UUID> inMatch = new HashSet<>();
    List<UUID> inMatchList; /// threadsafe list for iterating over inMatch and is synced with inMatch

    final Set<ArenaTeam> nonEndingTeams = new HashSet<>();
    final Map<ArenaTeam,Integer> individualTeamTimers = new HashMap<>();
    final Map<ArenaTeam,TeamTimeLimit> individualTeamTimeLimits = new HashMap<>();
    final Set<ArenaTeam> deadTeams = new HashSet<>();

    final AtomicBoolean addedVictoryConditions = new AtomicBoolean(false);
    final GameManager gameManager;
    @Getter double prizePoolMoney = 0;

    /// These get used enough or are useful enough that i'm making variables even though they can be found in match options
    final boolean needsClearInventory, clearsInventory, clearsInventoryOnDeath;
    final boolean keepsInventory;
    boolean respawns;
    final boolean spawnsRandom;
    final boolean woolTeams, armorTeams;
    final boolean alwaysTeamNames;
    final boolean respawnsWithClass;
    final boolean cancelExpLoss;
    final boolean individualWins;
    @Getter final boolean alwaysOpen;

    int neededTeams; 
    int nLivesPerPlayer = 1; /// This will change as victory conditions are added
    @Getter final ArenaScoreboard scoreboard;
    @Getter MatchMessager matchMessager; 
    AbstractJoinHandler joinHandler;
    final ArenaObjective defaultObjective;
    ArenaPreviousState oldArenaState;

    public Match(Arena _arena, MatchParams matchParams, Collection<ArenaListener> listeners) {
        
        transitionTo(MatchState.ONCREATE);

        if (Defaults.DEBUG) 
            System.out.println("ArenaMatch::" + matchParams );
        
        params = ParamController.copyParams(matchParams);
        params.setName(this.getName());
        params.flatten();
        
        tops = params.getStateGraph();
        gameManager = GameManager.getGameManager( params );
        
        arena = _arena;
        
        addArenaListener( arena );
        
        if ( listeners != null )
            addArenaListeners( listeners );
        
        scoreboard = new ArenaScoreboard( params.getName(), params );

        matchMessager = new MatchMessager(this);
        arena.setMatch(this);

        Collection<ArenaModule> modules = params.getModules();
        if (modules != null){
            for (ArenaModule am: modules){
                if (am.isEnabled())
                    addArenaListener(am);
            }
        }
        /// placed anywhere options
        if ( arena.hasRegion() )
            WorldGuardController.setFlag( arena.getWorldGuardRegion(), 
                                          "entry", 
                                          !tops.hasAnyOption(TransitionOption.WGNOENTER) );

        woolTeams = tops.hasAnyOption(TransitionOption.WOOLTEAMS) && params.getMaxTeamSize() > 1 
                    || tops.hasAnyOption(TransitionOption.ALWAYSWOOLTEAMS);
        
        armorTeams = tops.hasAnyOption(TransitionOption.ARMORTEAMS);
        tinState = tops.getCompetitionState(TransitionOption.TELEPORTIN);
        
        spawnsRandom = tinState != null && tops.hasOptionAt(tinState, TransitionOption.RANDOMSPAWN);
        
        alwaysTeamNames = tops.hasAnyOption(TransitionOption.ALWAYSTEAMNAMES);
        cancelExpLoss = tops.hasAnyOption(TransitionOption.NOEXPERIENCELOSS);
        matchResult = new MatchResult();
        
        needsClearInventory = tops.hasOptionAt(MatchState.PREREQS, TransitionOption.CLEARINVENTORY);
        clearsInventory = tops.hasOptionAt(MatchState.ONCOMPLETE, TransitionOption.CLEARINVENTORY);
        keepsInventory = tops.hasOptionAt(MatchState.ONDEATH, TransitionOption.KEEPINVENTORY);
        clearsInventoryOnDeath = tops.hasOptionAt(MatchState.ONDEATH, TransitionOption.CLEARINVENTORY);
        respawns = tops.hasOptionAt(MatchState.ONDEATH, TransitionOption.RESPAWN) 
                    || tops.hasOptionAt(MatchState.ONDEATH, TransitionOption.RANDOMSPAWN);

        respawnsWithClass = tops.hasOptionAt(MatchState.ONSPAWN, TransitionOption.RESPAWNWITHCLASS);
        alwaysOpen = params.isAlwaysOpen();
        neededTeams = alwaysOpen ? 0 : params.getMinTeams();

        individualWins = tops.hasOptionAt( MatchState.DEFAULTS, TransitionOption.INDIVIDUALWINS) || alwaysOpen;

        if ( tops.hasAnyOption( TransitionOption.TELEPORTWAITROOM, TransitionOption.TELEPORTLOBBY,
                                TransitionOption.TELEPORTMAINWAITROOM, TransitionOption.TELEPORTMAINLOBBY,
                                TransitionOption.TELEPORTCOURTYARD ) ) {
            waitRoomStates = new HashSet<>(
                    tops.getMatchStateRange( TransitionOption.TELEPORTMAINWAITROOM, TransitionOption.TELEPORTIN ) );
            
            if ( waitRoomStates.isEmpty() ) 
                waitRoomStates = null;
        }

        ListenerAdder.addListeners( this, tops );
        methodController.addAllEvents(this);
        
        if ( !Defaults.TESTSERVER )
            Bukkit.getPluginManager().registerEvents( this, BattleArena.getSelf() );
  
        defaultObjective = scoreboard.createObjective( "default",
                                                       "Player Kills", 
                                                       "&6Still Alive", 
                                                       SAPIDisplaySlot.SIDEBAR, 
                                                       100 );
        if ( params.getMaxTeamSize() <= 2 ) 
            defaultObjective.setDisplayTeams( false );

        /// Save the arena state if we are changing it
        if ( hasWaitroom() && params.isWaitroomClosedWhenRunning() ) {
            oldArenaState = new ArenaPreviousState( arena );
            arena.setContainerState( ChangeType.WAITROOM,
                                     new ContainerState( ContainerState.AreaContainerState.CLOSED,
                                                         "&cA match is already in progress in arena " 
                                                                     + arena.getName() ) );
        }
        updateBukkitEvents( MatchState.ONCREATE );
    }


    void updateBukkitEvents( MatchState matchState ) {
        methodController.updateEvents( null, matchState, getInMatchList() );
    }

    private void updateBukkitEvents( MatchState matchState, ArenaPlayer player ) {
        methodController.updateEvents( matchState, player );
    }

    private void updateBukkitEvents( MatchState matchState, ArenaListener listener ) {
        methodController.updateEvents( listener, matchState, getInMatchList() );
    }

    /**
     * As this gets calls Arena's and events which can call bukkit events
     * this should be done in a synchronous fashion
     */
    public void open(){
        MatchOpenEvent event = new MatchOpenEvent( this );
        callEvent( event );
        if ( event.isCancelled() ) {
            cancelMatch();
            return;
        }
        arena.publicOnOpen();
        transitionTo( MatchState.ONOPEN );
        updateBukkitEvents( MatchState.ONOPEN );
        transitionTo( MatchState.INOPEN );
        onJoin( teams );
    }

    @Override
    public void run() {
        preStartMatch();
    }

    public boolean isTimedStart() {
        return startCountdown != null;
    }

    public void setTimedStart(int seconds, Integer interval) {
        if ( startCountdown != null ) {
            startCountdown.stop();
            startCountdown = null;
        }
        if ( joinHandler != null ) { 
            joinHandler.useWaitingScoreboard();
            joinHandler.setWaitingScoreboardTime(seconds);
        }
        if ( seconds > 0 ) {
            matchMessager.sendCountdownTillPrestart(seconds);
            startCountdown = new Countdown( BattleArena.getSelf(), 
                                            seconds, 
                                            interval, 
                                            remaining -> {

                                                    if ( state != MatchState.ONOPEN ) return false;
                                                    
                                                    if ( remaining == 0 ) {
                                                        if ( checkEnoughTeams( teams, neededTeams ) )
                                                            preStartMatch();
                                                    } 
                                                    else {
                                                        matchMessager.sendCountdownTillPrestart( remaining );
                                                    }
                                                    return true;
                                                });
        } 
        else preStartMatch();
    }

    public void hookTeamJoinHandler( AbstractJoinHandler teamJoinHandler ) {
        
        teamJoinHandler.setCompetition( this );
        joinHandler = teamJoinHandler;
        teams = joinHandler.getTeams();
        
        for ( int i = 0; i < teams.size(); i++ ) {
            teams.get(i).setIndex(i);
            
            for ( ArenaPlayer ap : teams.get(i).getPlayers() ) {
                joiningOngoing( teams.get(i), ap );
            }
        }
        inGamePlayers.addAll( joinHandler.getPlayers() );
    }
    
    public List<ArenaTeam> getNonEmptyTeams() {
        
        List<ArenaTeam> aTeams = new ArrayList<>();
        for ( ArenaTeam at : teams ) {
            if ( at != null && at.size() > 0 ) 
                aTeams.add( at );
        }
        return aTeams;
    }
    
    private void preStartTeams( List<ArenaTeam> teams, boolean matchPrestarting ) {
        
        StateOptions ts = params.getStateOptions( MatchState.ONPRESTART );
        /// If we will teleport them into the arena for the first time, check to see they are ready first
        if ( ts != null && ts.hasAnyOption( TransitionOption.TELEPORTWAITROOM, 
                                            TransitionOption.TELEPORTMAINWAITROOM, 
                                            TransitionOption.TELEPORTIN) ) {
            for ( ArenaTeam t : teams ) 
                checkReady( t, params.getStateOptions( MatchState.PREREQS ) );	
        }
        TransitionController.transition( this, MatchState.ONPRESTART, teams, true );

        if ( matchPrestarting ) 
            matchMessager.sendOnPreStartMsg( getNonEmptyTeams() );
        else 
            matchMessager.sendOnPreStartMsg( getNonEmptyTeams(), Channels.NullChannel );
    }

    private void preStartMatch() {
        
        if (    state == MatchState.ONCANCEL 
                || state.ordinal() >= MatchState.INPRESTART.ordinal() )
            return; 
        
        if ( Defaults.DEBUG ) Log.info("ArenaMatch::startMatch()");
        
        transitionTo( MatchState.ONPRESTART );
        updateBukkitEvents( MatchState.ONPRESTART );
        transitionTo( MatchState.INPRESTART );
        callEvent( new MatchPrestartEvent( this,teams ) );

        preStartTeams(teams, true);
        arena.publicOnPrestart();

        startCountdown = new Countdown( BattleArena.getSelf(), 
                                        params.getSecondsTillMatch(), 
                                        1, 
                                        remaining -> {
                                            
                                            ArenaObjective obj = scoreboard.getObjective( SAPIDisplaySlot.SIDEBAR );
                                            
                                            if ( obj != null ) {
                                                if (    remaining == 0 
                                                        && params.getMatchTime() == CompetitionSize.MAX ) {
                                                    obj.setDisplayNameSuffix( "" );
                                                }
                                                else
                                                    obj.setDisplayNameSuffix( " &e(" + remaining + ")" );
                                            }
                                            return (currentTimer!=null);                
                                        });

        currentTimer = Scheduler.scheduleSynchronousTask( () -> startMatch(), 
                                                          params.getSecondsTillMatch() * 20L );

        if ( waitRoomStates != null ) 
            joinCutoffTime = System.currentTimeMillis() + ( params.getSecondsTillMatch() 
                                                                - Defaults.JOIN_CUTOFF_TIME ) * 1000; 
    }

    /**
     * If the match is already in the preStart phase, just start it
     */
    public void start() {
        if ( state != MatchState.INPRESTART ) return;
        
        if ( currentTimer != null ) 
            Bukkit.getScheduler().cancelTask(currentTimer);
        
        currentTimer = Scheduler.scheduleSynchronousTask( () -> startMatch(), ( 10 * 20L ) );
    }

    void startMatch(){
        if ( state == MatchState.ONCANCEL ) return; 
        
        transitionTo( MatchState.ONSTART );
        
        if ( !addedVictoryConditions.get() ) {
            addVictoryConditions();
        }
        List<ArenaTeam> competingTeams = new ArrayList<>();
        /// If we will teleport them into the arena for the first time, check to see they are ready first
        StateOptions ts = params.getStateOptions( state );
        if ( ts != null && ts.hasAnyOption( TransitionOption.TELEPORTWAITROOM, 
                                            TransitionOption.TELEPORTMAINWAITROOM, 
                                            TransitionOption.TELEPORTIN) ) {
            for ( ArenaTeam t : teams ) 
                checkReady( t, params.getStateOptions( MatchState.PREREQS ) );
        }
        for ( ArenaTeam t : teams ) {
            if ( !t.isDead() ) 
                competingTeams.add(t);
        }
        int nCompetingTeams = competingTeams.size();

        if ( Defaults.DEBUG ) 
            Log.info( "[BattleArena] competing teams = " + competingTeams + ":" + neededTeams + "   allteams=" + teams );

        if ( nCompetingTeams >= neededTeams ) {
            
            MatchStartEvent event = new MatchStartEvent( this, teams );
            updateBukkitEvents( MatchState.ONSTART );
            callEvent( event );
            TransitionController.transition( this, MatchState.ONSTART, competingTeams, true);
            arena.publicOnStart();
            matchMessager.sendOnStartMsg( getNonEmptyTeams() );
            
            if ( Defaults.USE_SCOREBOARD ) {
                for ( ArenaPlayer ap : inGamePlayers ) {
                    scoreboard.setScoreboard( ap.getPlayer() );
                }
            }
        }
        checkEnoughTeams( competingTeams, neededTeams );
        transitionTo( MatchState.INGAME );
    }

    @Override
    public boolean hasOption(TransitionOption option) {
        return params.getStateGraph().hasInArenaOrOptionAt(state, option);
    }

    private boolean checkEnoughTeams( List<ArenaTeam> competingTeams, int _neededTeams ) {
        final int nCompetingTeams = competingTeams.size();
        
        if ( nCompetingTeams >= _neededTeams ) {
            return true;
        } 
        else if ( nCompetingTeams < _neededTeams && nCompetingTeams == 1 ) {
            
            ArenaTeam victor = competingTeams.get(0);
            victor.sendMessage( "&4WIN!!!&eThe other team was offline or didnt meet the entry requirements." );
            setVictor( victor );
            return false;         
        } 
        else { /// Seriously, no one showed up?? Well, one of them won regardless, but scold them
            if ( competingTeams.isEmpty() )
                cancelMatch();
            else 
                setDraw();
            return false;
        }
    }

    private synchronized void matchWinLossOrDraw(MatchResult result) {
        if ( alwaysOpen )
            nonEndingMatchWinLossOrDraw( result );
       else
            endingMatchWinLossOrDraw( result );
    }

    private synchronized void nonEndingMatchWinLossOrDraw(MatchResult result) {
        
        List<ArenaTeam> remove = new ArrayList<>();
        
        for ( ArenaTeam t : result.getLosers() )
            if ( !nonEndingTeams.add( t ) || t.size() == 0 )
                remove.add( t ); 
        
        result.removeLosers( remove );
        remove.clear();
        
        for ( ArenaTeam t : result.getDrawers() )
            if ( !nonEndingTeams.add( t ) || t.size() == 0 )
                remove.add( t ); 
        
        result.removeDrawers( remove );
        remove.clear();
        
        for ( ArenaTeam t : result.getVictors() )
            if ( !nonEndingTeams.add( t ) || t.size() == 0 )
                remove.add( t ); 
        
        result.removeVictors(remove);

        if ( result.getLosers().isEmpty() && result.getDrawers().isEmpty() && result.getVictors().isEmpty() )
            return;

        MatchResultEvent event = new MatchResultEvent( this,result );
        callEvent( event );
        
        if ( event.isCancelled() ) return;
       
        int timerid = Scheduler.scheduleSynchronousTask( new NonEndingMatchVictory( this, result ), 2L );
        for ( ArenaTeam t : teams )
            individualTeamTimers.put( t, timerid );
    }

    private synchronized void endingMatchWinLossOrDraw(MatchResult result) {
        if (    state == MatchState.ONVICTORY 
                || state == MatchState.ONCOMPLETE 
                || state == MatchState.ONCANCEL )
            return;
        
        matchResult = result;
        MatchResultEvent event = new MatchResultEvent( this,result );
        callEvent(event);
        
        if ( event.isCancelled() ) return;

        transitionTo( MatchState.ONVICTORY );
        transitionTo( MatchState.INVICTORY );
        arena.publicOnVictory( result );
        /// Call the rest after a 2 tick wait to ensure the calling method complete before the
        /// victory conditions start rolling in
        currentTimer = Scheduler.scheduleSynchronousTask( new MatchVictory(this), 2L );
    }

    class NonEndingMatchVictory implements Runnable {
        final Match am;
        final MatchResult result;
        NonEndingMatchVictory( Match _am, MatchResult _result ){ am = _am; result = _result; }

        @Override
        public void run() {
            List<ArenaTeam> aTeams = new ArrayList<>();
            final Set<ArenaTeam> victors = result.getVictors();
            final Set<ArenaTeam> losers = result.getLosers();
            final Set<ArenaTeam> drawers = result.getDrawers();
            aTeams.addAll( victors );
            aTeams.addAll( losers );
            aTeams.addAll( drawers );
            if ( Defaults.DEBUG_TRACE ) 
                Log.trace( am.getId(), "Match::MatchVictory():" + am +
                                       "  victors=" + victors +
                                       "  losers=" + losers + 
                                       "  drawers=" + drawers + " " + matchResult + 
                                       " secondsToLoot=" + params.getSecondsToLoot() );
            if ( params.isRated() ) {
                TrackerInterface sc = Tracker.getInterface( params );
                BTInterface.addRecord( sc, victors, losers, drawers, result.getResult(), params.isTeamRating() );
            }

            if ( result.hasVictor() ) { 
                matchMessager.sendOnVictoryMsg(victors, losers);
            } 
            else { 
                matchMessager.sendOnDrawMessage(drawers,losers);
            }

            TransitionController.transition( am, MatchState.ONVICTORY, aTeams, true);
            
            int timerid = Scheduler.scheduleSynchronousTask( new NonEndingMatchCompleted(am, result, aTeams),
                                                             params.getSecondsToLoot() * 20L );
            for ( ArenaTeam t : aTeams )
                individualTeamTimers.put( t, timerid );
        }
    }

    class MatchVictory implements Runnable {
        final Match am;
        MatchVictory(Match _am){ am = _am; }

        @Override
        public void run() {
            final Set<ArenaTeam> victors = matchResult.getVictors();
            final Set<ArenaTeam> losers = matchResult.getLosers();
            final Set<ArenaTeam> drawers = matchResult.getDrawers();
            
            if ( Defaults.DEBUG_TRACE ) 
                Log.trace( am.getId(), "Match::MatchVictory():" + am + 
                                       "  victors=" + victors +
                                       "  losers=" + losers + 
                                       "  drawers=" + drawers + " " + matchResult + 
                                       " secondsToLoot=" + params.getSecondsToLoot() );

            if ( params.isRated() ) {
                TrackerInterface sc = Tracker.getInterface( params );
                BTInterface.addRecord( sc, victors,losers,drawers,am.getMatchResult().getResult(), params.isTeamRating());
            }
            if ( matchResult.hasVictor() ) { 
                matchMessager.sendOnVictoryMsg(victors, losers);
            } 
            else { 
                matchMessager.sendOnDrawMessage(drawers,losers);
            }
            updateBukkitEvents( MatchState.ONVICTORY );
            TransitionController.transition( am, MatchState.ONVICTORY, teams, true );
            currentTimer = Scheduler.scheduleSynchronousTask(
                                    new MatchCompleted( am ), params.getSecondsToLoot() * 20L );
        }
    }

    void givePrizes( Match am, MatchResult result ) {
        
        if ( result.getLosers() != null && !result.getLosers().isEmpty() ) {
            
            TransitionController.transition( am, MatchState.LOSERS, result.getLosers(), false );
            ArenaPrizeEvent event = new ArenaLosersPrizeEvent( am, result.getLosers() );
            callEvent( event );
            new RewardController( event, psc ).giveRewards();
        }
        if ( result.getDrawers() != null && !result.getDrawers().isEmpty() ) {
            TransitionController.transition( am, MatchState.DRAWERS, result.getDrawers(), false );
            ArenaPrizeEvent event = new ArenaDrawersPrizeEvent( am, result.getDrawers() );
            callEvent( event );
            new RewardController( event,psc ).giveRewards();
        }
        if ( result.getVictors() != null && !result.getVictors().isEmpty() ) {
            TransitionController.transition( am, MatchState.WINNERS, result.getVictors(), false );
            ArenaPrizeEvent event = new ArenaWinnersPrizeEvent( am, result.getVictors() );
            callEvent( event );
            new RewardController( event, psc ).giveRewards();
        }
    }

    class NonEndingMatchCompleted implements Runnable {
        final Match am;
        final MatchResult result;
        final List<ArenaTeam> aTeams;

        NonEndingMatchCompleted( Match _am, MatchResult _result, List<ArenaTeam> _teams ) {
            am = _am;
            result = _result;
            aTeams = _teams;
        }

        @Override
        public void run() {
            final Collection<ArenaTeam> victors = result.getVictors();
            
            if ( Defaults.DEBUG ) Log.info( "Match::NonEndingMatchCompleted(): " + victors );
            /// ONCOMPLETE can teleport people out of the arena,
            /// So the order of events is usually
            /// ONCOMPLETE(half of effects) -> ONLEAVE( and all effects) -> ONCOMPLETE( rest of effects)
            TransitionController.transition( am, MatchState.ONCOMPLETE, aTeams, true);
            
            /// Once again, lets delay this final bit so that transitions have time to finish before
            /// Other splisteners get a chance to handle
            int timerid = Scheduler.scheduleSynchronousTask( () -> { givePrizes(am, result);
                                                                     nonEndingDeconstruct(aTeams); } );
            for ( ArenaTeam t : aTeams )
                individualTeamTimers.put( t, timerid );
        }
    }

    class MatchCompleted implements Runnable {
        final Match am;

        MatchCompleted( Match _am ) { am = _am; }
        @Override
        public void run() {
            transitionTo(MatchState.ONCOMPLETE);
            if ( Defaults.DEBUG ) Log.info( "Match::MatchCompleted(): " + am.getMatchResult() );
            /// ONCOMPLETE can teleport people out of the arena,
            /// So the order of events is usually
            /// ONCOMPLETE(half of effects) -> ONLEAVE( and all effects) -> ONCOMPLETE( rest of effects)
            TransitionController.transition( am, MatchState.ONCOMPLETE, teams, true);
            
            /// Once again, lets delay this final bit so that transitions have time to finish before
            /// Other splisteners get a chance to handle
            currentTimer = Scheduler.scheduleSynchronousTask( 
                    () -> { callEvent( new MatchCompletedEvent(am) );
                            arena.publicOnComplete();
                            /// Losers and winners get handled after the match is complete
                            givePrizes(am, matchResult);
                            updateBukkitEvents(MatchState.ONCOMPLETE);
                            deconstruct(); 
                          } );
        }
    }

    public synchronized void cancelMatch() {
        
        if ( state == MatchState.ONCANCEL ) return;
        
        state = MatchState.ONCANCEL;
        arena.publicOnCancel();
        
        for ( ArenaTeam t : teams ) {
            if ( t == null ) continue;
            
            TransitionController.transition( this, MatchState.ONCANCEL, t, true );
        }
        /// For players that were in the process of joining when cancel happened
        for (Entry<ArenaTeam,Integer> entry : individualTeamTimers.entrySet()){
            Scheduler.cancelTask(entry.getValue());
            if (!teams.contains(entry.getKey()))
                TransitionController.transition( this, MatchState.ONCANCEL, entry.getKey(), true);
        }
        callEvent(new MatchCancelledEvent(this));
        updateBukkitEvents(MatchState.ONCANCEL);
        deconstruct();
    }

    void nonEndingDeconstruct(List<ArenaTeam> teams){
        for (ArenaTeam t: teams){
            TransitionController.transition( this, MatchState.ONFINISH, t, true);
            for (ArenaPlayer p: t.getPlayers()){
                p.removeCompetition(this);
                if (joinHandler != null)
                    joinHandler.leave(p);
            }
            individualTeamTimers.remove(t);
            scoreboard.removeTeam(t);
            TeamTimeLimit ttl = individualTeamTimeLimits.remove(t);
            if (ttl !=null){
                ttl.stopCountdown();
            }
            t.reset();
        }
        nonEndingTeams.removeAll(teams);
    }

    void deconstruct(){
        /// Teleport out happens 1 tick after oncancel/oncomplete, we also must wait 1 tick
        final Match match = this;
        callEvent(new MatchFinishedEvent(match));
        updateBukkitEvents(MatchState.ONFINISH);
        
        for (ArenaTeam t: teams){
            TransitionController.transition( this, MatchState.ONFINISH, t, true);
            for (ArenaPlayer p: t.getPlayers()){
                p.removeCompetition(this);
            }
            scoreboard.removeTeam(t);
        }
        /// For players that were in the process of joining when deconstruct happened
        for (Entry<ArenaTeam,Integer> entry : individualTeamTimers.entrySet()){
            Scheduler.cancelTask(entry.getValue());
            if (!teams.contains(entry.getKey())){
                TransitionController.transition( this, MatchState.ONCANCEL, entry.getKey(), true);
                for (ArenaPlayer p: entry.getKey().getPlayers()){
                    p.removeCompetition(this);
                }
            }
        }
        if (oldArenaState != null){
            oldArenaState.revert(arena);}
        scoreboard.clear();
        arena.publicOnFinish();
        inMatch.clear();
        inMatchList = null;
        teams.clear();
        methodController.deconstruct();
        HandlerList.unregisterAll(this);
    }
    
    @Override
    public boolean addedTeam(ArenaTeam team) {
        
        if (this.isFinished())
            return false;
        if (Defaults.DEBUG_MATCH_TEAMS)Log.info( id + " addedTeam(" + team.getName() + ":" + team.getId()+ ")" );

        team.setArenaObjective(defaultObjective);
        scoreboard.addTeam(team);

        for (ArenaPlayer p: team.getPlayers()){
            if (p == null)
                continue;
            _addedToTeam( team, p );
            if (!this.isHandled(p))
                joiningOngoing(team, p);
        }
        return true;
    }

    /**
     * Add an arena listener for this competition
     * @param arenaListener ArenaListener
     */
    @Override
    public final void addArenaListener(ArenaListener arenaListener){
        methodController.addListener(arenaListener);
        /// update listener to ONCREATE if we are already opened
        if (state.ordinal() >= MatchState.ONCREATE.ordinal()) {
            methodController.updateEventsRange(arenaListener, MatchState.ONCREATE, state, getInMatchList());
        }
        if (!inMatch.isEmpty()){
            updateBukkitEvents( MatchState.ONENTER,arenaListener ); /// register all current inside players with the vc
        }
    }

    @Override
    public boolean removeArenaListener(ArenaListener arenaListener){
        return methodController.removeListener(arenaListener);
    }
    
    private List<UUID> getInMatchList(){
        synchronized (this){
            if (inMatchList==null) {
                inMatchList = new ArrayList<>(inMatch);
            }
            return inMatchList;
        }
    }

    public boolean isInMatch(ArenaPlayer player) {
        return inMatch.contains(player.getUniqueId());
    }

    private boolean addInMatch(ArenaPlayer player) {
        boolean added = false;
        synchronized (this) {
            if(inMatch.add(player.getUniqueId())){
                inMatchList = null;
                added = true;
            }
        }
        if (Defaults.DEBUG_TRACE) Log.trace( id, player.getName() + "   !!!!&2playerEntering  "+added+" t=" + player.getTeam());
        if (added){
            updateBukkitEvents(MatchState.ONENTER,player);
            arena.publicOnEnter(player, player.getTeam());
        }
        return added;
    }

    private boolean removeInMatch(ArenaPlayer player) {
        boolean removed = false;

        synchronized (this) {
            if (inMatch.remove(player.getUniqueId())){
                inMatchList = null;
                removed = true;
            }
        }
        if ( Defaults.DEBUG_TRACE ) 
            Log.trace( id, player.getName() + "   !!!!&4playerLeaving  " + removed + " t=" + player.getTeam() );
        if ( removed ) {
            player.despawnMobs();
            updateBukkitEvents(MatchState.ONLEAVE,player);
            arena.publicOnLeave(player,player.getTeam());
        }
        return removed;
    }

    /** Called during both, addedTeam and addedToTeam */
    private void _addedToTeam(ArenaTeam team, ArenaPlayer player) {
        deadTeams.remove(team);
        team.setAlive(player);
        scoreboard.addedToTeam(team, player);
        defaultObjective.setPoints(player, 0);
    }

    @Override
    public boolean removedTeam(ArenaTeam team){
        if ( Defaults.DEBUG_MATCH_TEAMS ) 
            Log.info( id + " removedTeam(" + team.getName() + ":" + team.getId() + ")" );
        scoreboard.removeTeam(team);
        teams.remove(team);
        HeroesController.removeTeam(team);
        return true;
    }

    /**
     * Add to an already existing team
     * @param team ArenaTeam
     * @param player player
     */
    @Override
    public void addedToTeam(final ArenaTeam team, final ArenaPlayer player) {
        if (isEnding())
            return;
        if (Defaults.DEBUG_MATCH_TEAMS)
            Log.info( id + " addedToTeam(" + team.getName() + ":" + team.getId() + 
                    ", " + player.getName() + ") inside=" + isInMatch( player ) );

        if ( !team.hasSetName() && team.getDisplayName().length() > Defaults.MAX_TEAM_NAME_APPEND ) 
            team.setDisplayName( TeamUtil.getTeamName( team.getIndex() ) );
        
        _addedToTeam(team, player);

        matchMessager.sendAddedToTeam(team,player);
        if (!this.isHandled(player))
            joiningOngoing(team, player);
    }

    void doTransition(Match match, MatchState state, ArenaPlayer player, ArenaTeam team, boolean onlyInMatch){
        if (player != null){
            TransitionController.transition( match, state, player, team, onlyInMatch);
        } else {
            TransitionController.transition( match, state, team, onlyInMatch);
        }
    }

    private void joiningOngoing(final ArenaTeam team, final ArenaPlayer player) {
        final Match match = this;

        if ( !gameManager.hasPlayer( player ) ) {
            doTransition( match, MatchState.ONJOIN, player,team, true );
        }
        if ( state.ordinal() >= MatchState.ONPRESTART.ordinal() ) {
            doTransition( match, MatchState.ONPRESTART, player, team, true );

            if ( state.ordinal() >= MatchState.ONSTART.ordinal() ) {
                int timerid = Scheduler.scheduleSynchronousTask( 
                        () -> doTransition( match, MatchState.ONSTART, player,team, true ),
                        params.getSecondsTillMatch() * 20L );
                if ( !individualTeamTimeLimits.containsKey( team ) ) {
                    TeamTimeLimit ttl = new TeamTimeLimit( this, team );
                    individualTeamTimeLimits.put( team, ttl );
                    ttl.startCountdown();
                }
                individualTeamTimers.put( team, timerid );
            }
        }
    }

    @Override
    public void addedToTeam(ArenaTeam team, Collection<ArenaPlayer> players) {
        for ( ArenaPlayer ap : players )
            addedToTeam( team, ap );
    }

    @Override
    public void removedFromTeam(ArenaTeam team, Collection<ArenaPlayer> players) {
        for ( ArenaPlayer ap : players )
            removedFromTeam( team, ap );
    }

    @Override
    public void removedFromTeam(ArenaTeam team, ArenaPlayer player) {
       
        if ( Defaults.DEBUG_MATCH_TEAMS ) 
            Log.info( id + " removedFromTeam(" + team.getName() + ":" + team.getId() + ")" + player.getName() );
        HeroesController.removedFromTeam(team, player.getPlayer());
        scoreboard.removeFromTeam(team, player);
    }

    protected void onJoin(Collection<ArenaTeam> teams){
        if (teams == null)
            return;
        for (ArenaTeam t: teams)
            addedTeam( t );
    }

    /**
     * TeamHandler override
     * Players can always leave, they just might be killed for doing so
     */
    @Override
    public boolean canLeave(ArenaPlayer p) {
        return !tops.hasOptionAt(state, TransitionOption.NOLEAVE);
    }

    /**
     * TeamHandler override
     * We already handle leaving in other methods.
     */
    @Override
    public boolean leave(ArenaPlayer p) {
        return joinHandler != null && joinHandler.leave(p);
    }

    protected void checkAndHandleIfTeamDead(ArenaTeam team){
        if (Defaults.DEBUG_TRACE) Log.info("   -Team " + team+ " dead=" + team.isDead());
        if (team.isDead() && deadTeams.add(team)){
            if (this.getState().ordinal() < MatchState.ONVICTORY.ordinal()){
                callEvent(new TeamDeathEvent(team));
            }
            if (alwaysOpen) {
                TeamTimeLimit ttl = individualTeamTimeLimits.remove(team);
                if (ttl !=null) {
                    ttl.stopCountdown();
                }
            }
        }
    }

    @Override
    public void onPreJoin(ArenaPlayer player, ArenaPlayerTeleportEvent apte) {
        preFirstJoin(player);
    }

    private void preFirstJoin(ArenaPlayer player){
        if (Defaults.DEBUG_TRACE) Log.info(player.getName() + " -preFirstJoin  t=" + player.getTeam());
        ArenaTeam team = getTeam(player);
        leftPlayers.remove(player.getUniqueId()); /// remove players from the list as they are now joining again
        inGamePlayers.add(player);
        player.addCompetition(this);
        if (params.hasEntranceFee()){
            prizePoolMoney += params.getEntranceFee();}
        if (WorldGuardController.hasWorldGuard() && arena.hasRegion()){
            psc.addMember(player, arena.getWorldGuardRegion());}

        TransitionController.transition( this, MatchState.ONENTERARENA, player, team, false);
    }

    @Override
    public void onPostJoin(ArenaPlayer player, ArenaPlayerTeleportEvent apte) {
        postFirstJoin(player);
    }

    private void postFirstJoin(ArenaPlayer player){
        if (Defaults.DEBUG_TRACE) Log.info(player.getName() + " -preFirstJoin  t=" + player.getTeam());
        ArenaTeam team = getTeam(player);
        arena.publicOnJoin(player,team);
        if (state == MatchState.ONOPEN && joinHandler != null && joinHandler.isFull()){
            Scheduler.scheduleSynchronousTask(BattleArena.getSelf(), this);
        }
        if (nLivesPerPlayer != 1 && nLivesPerPlayer != ArenaSize.MAX) {
            player.getMetaData().setLivesLeft(nLivesPerPlayer);
            SEntry e = scoreboard.getEntry(player.getPlayer().getName());
            if (e!=null)
                scoreboard.setEntryNameSuffix(e, "(" + nLivesPerPlayer + ")");
        }
        if (!params.getUseTrackerPvP()){
            TrackerInterface sc = Tracker.getInterface( params );
            sc.stopTracking(player.getName());
            sc.stopMessages(player.getName());
        }
        if (woolTeams && team !=null && team.getIndex()!=-1){
            TeamUtil.setTeamHead(team.getIndex(), player); // give wool heads
        }

        if (cancelExpLoss){
            psc.cancelExpLoss(player,true);}
        addInMatch(player);
    }

    /**
     * Only do this when players are not in the competition yet
     * @param event ArenaPlayerLeaveEvent
     */
    @EventHandler
    public void onArenaPlayerLeaveEventGlobal(ArenaPlayerLeaveEvent event){
        if (Defaults.DEBUG_TRACE) 
            MessageUtil.sendMessage(event.getPlayer(), " -onArenaPlayerLeaveEventGlobal  t=" + event.getPlayer().getTeam());

        if (    leftPlayers.contains( event.getPlayer().getUniqueId() ) 
                || (    inGamePlayers.contains( event.getPlayer() ) 
                        && event.getPlayer().getCurLocation().getType() == LocationType.ARENA )
                || !isHandled( event.getPlayer() ) )
            return;
        privateQuitting(event);
    }

    @ArenaEventHandler
    public void onArenaPlayerLeaveEvent(ArenaPlayerLeaveEvent event){
        ArenaPlayer player = event.getPlayer();
        if (Defaults.DEBUG_TRACE) MessageUtil.sendMessage( player, " -onArenaPlayerLeaveEvent  t=" + player.getTeam());
        if (!isHandled(player))
            return;
        privateQuitting(event);
    }

    protected void privateQuitting(ArenaPlayerLeaveEvent event) {
        ArenaPlayer ap = event.getPlayer();
        if (params.hasOptionAt(MatchState.DEFAULTS, TransitionOption.DROPITEMS)) {
            InventoryUtil.dropItems(ap.getPlayer());
            InventoryUtil.clearInventory(ap.getPlayer());
        }
        /// The onCancel should teleport them out, and call leaveArena(ap)
        TransitionController.transition( this, MatchState.ONCANCEL, ap, ap.getTeam(), false);

        event.addMessage(MessageHandler.getSystemMessage("you_left_competition", this.params.getName()));
    }

    @Override
    public void onPreQuit(ArenaPlayer player, ArenaPlayerTeleportEvent apte) { }

    @Override
    public void onPostQuit(ArenaPlayer player, ArenaPlayerTeleportEvent apte) {
        if (Defaults.DEBUG_TRACE) Log.trace( id, player.getName() + " -onPostQuit  t=" + player.getTeam());
        ArenaTeam t = player.getTeam();
        TransitionController.transition( this, MatchState.ONLEAVEARENA, player, t, false);
        if (WorldGuardController.hasWorldGuard() && arena.hasRegion())
            psc.removeMember(player, arena.getWorldGuardRegion());
        player.removeCompetition(this);
        player.reset(); /// reset the players
        if (!params.getUseTrackerPvP()){
            TrackerInterface sc = Tracker.getInterface( params );
            sc.resumeTracking(player.getName());
            sc.resumeMessages(player.getName());
        }
        if (t != null){
            if ( woolTeams )
                TeamUtil.removeTeamHead( t.getIndex(), player.getPlayer() );
            t.killMember(player);
            checkAndHandleIfTeamDead(t);
            scoreboard.removeFromTeam(t, player);
        }

        boolean cancelsIfGone = state.ordinal() <= MatchState.ONOPEN.ordinal();
        /// free the team up for more players
        if (alwaysOpen || cancelsIfGone){
            joinHandler.leave(player);
            inGamePlayers.remove(player);
        }
        if (cancelExpLoss){
            psc.cancelExpLoss(player,false);}
        removeInMatch(player);
        player.setTeam(null);
        if (cancelsIfGone && joinHandler.isEmpty()) {
            cancelMatch();
        }
        if (state == MatchState.ONCANCEL || state.ordinal() <= MatchState.ONOPEN.ordinal()){
            if (params.hasEntranceFee())
                this.prizePoolMoney -= params.getEntranceFee();
            /// refund their join requirements
            if (player.getMetaData().getJoinRequirements()!=null){
                PlayerStoreController _psc = new PlayerStoreController(player.getMetaData().getJoinRequirements());
                _psc.restoreAll(player);
            }
        }
        if (player.getMetaData().getJoinRequirements()!=null){
            player.getMetaData().setJoinRequirements(null);
        }
    }

    @Override
    public void onPreEnter(ArenaPlayer player, ArenaPlayerTeleportEvent apte) {
        if (Defaults.DEBUG_TRACE) Log.trace( id,player.getName() + " -&fonPreEnter  t=" + player.getTeam());
        if (!inGamePlayers.contains(player)){
            preFirstJoin(player);
            player.getMetaData().setJoining(true);
        }
        addInMatch(player);
    }

    @Override
    public void onPostEnter(ArenaPlayer player, ArenaPlayerTeleportEvent apte) {
        if (Defaults.DEBUG_TRACE) Log.trace( id, player.getName() + " -&fonPostEnter  t=" + player.getTeam());
        if (player.getMetaData().isJoining()){
            player.getMetaData().setJoining(false);
            postFirstJoin(player);
        }
    }

    @Override
    public void onPreLeave(ArenaPlayer player, ArenaPlayerTeleportEvent apte) {
        if (Defaults.DEBUG_TRACE) Log.trace( id, player.getName() + " -&8onPreLeave  t=" + player.getTeam());
        removeInMatch(player);
    }

    @Override
    public void onPostLeave(ArenaPlayer player, ArenaPlayerTeleportEvent apte) {
        if (Defaults.DEBUG_TRACE) Log.trace( id, player.getName() + " -&8onPostLeave  t=" + player.getTeam());
    }

    private synchronized void addVictoryConditions(){
        addedVictoryConditions.set(true);
        VictoryCondition vt = VictoryType.createVictoryCondition(this);

        /// Add a time limit unless one is provided by default
        if (!alwaysOpen && !(vt instanceof DefinesTimeLimit) &&
                (params.getMatchTime() != null && params.getMatchTime() > 0 &&
                        params.getMatchTime() != Integer.MAX_VALUE)){
            addVictoryCondition(new TimeLimit(this));
        }
        if (alwaysOpen){
            for (ArenaTeam team : getNonEmptyTeams()){
                TeamTimeLimit ttl = new TeamTimeLimit(this, team);
                individualTeamTimeLimits.put(team, ttl);
                ttl.startCountdown();
            }
        }
        /// set the number of lives
        Integer nLives = params.getNLives();
        if ( nLives > 0 && !(vt instanceof DefinesNumLivesPerPlayer)){
            addVictoryCondition(new NLives(this, nLives));
        }

        /// Add a default number of teams unless the specified victory condition handles it
        Integer nTeams = params.getMinTeams();
        if (!(vt instanceof DefinesNumTeams)){
            if (nTeams <= 0 || alwaysOpen){
                /* do nothing.  They want this event to be open even with no teams*/
            } else if (nTeams == 1){
                addVictoryCondition(new NoTeamsLeft(this));
            } else {
                addVictoryCondition(new OneTeamLeft(this));
            }
        }
        addVictoryCondition(vt);
    }

    /**
     * Add Another victory condition to this match
     * @param victoryCondition Victory condition to add
     */
    public void addVictoryCondition(VictoryCondition victoryCondition){
        if (Defaults.DEBUG_TRACE) Log.trace( id, getArena().getName() + " adding vc=" + victoryCondition);
        victoryConditions.add(victoryCondition);
        addArenaListener(victoryCondition);
        if (!alwaysOpen && victoryCondition instanceof DefinesNumTeams)
            neededTeams = Math.max(neededTeams, ((DefinesNumTeams)victoryCondition).getNeededNumberOfTeams().max);
        
        if (victoryCondition instanceof DefinesNumLivesPerPlayer){
            nLivesPerPlayer = Math.max(nLivesPerPlayer, ((DefinesNumLivesPerPlayer)victoryCondition).getLivesPerPlayer());
            if (nLivesPerPlayer > 1){
                respawns=true;
            }
            for (ArenaPlayer ap : inGamePlayers){
                if (nLivesPerPlayer != 1 && nLivesPerPlayer != ArenaSize.MAX) {
                    ap.getMetaData().setLivesLeft(nLivesPerPlayer);
                    scoreboard.setEntryNameSuffix(ap.getName(),"&4("+nLivesPerPlayer+")");
                }
            }
        }
        /// if it's a time limit. make a timer for the scoreboard
        if (Defaults.USE_SCOREBOARD && !alwaysOpen && victoryCondition instanceof TimeLimit){
            if (matchCountdown != null){
                matchCountdown.stop();}
            final Match match = this;
            matchCountdown = new Countdown( BattleArena.getSelf(),
                                            ((TimeLimit) victoryCondition).getTime(), 
                                            1, 
                                            secs -> { match.secondTick( secs );
                                                      return true; } );
        }
        if ( victoryCondition instanceof ScoreTracker ) {
            if ( params.getMaxTeamSize() <= 2 ) {
                ((ScoreTracker)victoryCondition).setDisplayTeams(false);
            }
            ((ScoreTracker)victoryCondition).setScoreboard(scoreboard);
        }
    }

    /**
     * Remove a victory condition from this match
     * @param victoryCondition VictoryCondition to remove
     */
    public void removeVictoryCondition(VictoryCondition victoryCondition){
        victoryConditions.remove(victoryCondition);
        removeArenaListener(victoryCondition);
    }

    public VictoryCondition getVictoryCondition(Class<? extends VictoryCondition> clazz) {
        for (VictoryCondition vc : victoryConditions){
            if (vc.getClass() == clazz)
                return vc;
        }
        return null;
    }

    public boolean isEnding() { return isWon() || isFinished(); }
    public boolean isFinished() { return state == MatchState.ONCOMPLETE || state == MatchState.ONCANCEL; }
    public boolean isWon() { return state == MatchState.INVICTORY || state == MatchState.ONCOMPLETE || state == MatchState.ONCANCEL;}
    public boolean isStarted() { return state == MatchState.INGAME; }
    public boolean isInWaitRoomState() { return state.ordinal() < MatchState.ONSTART.ordinal(); }

    @Override
    protected void transitionTo( CompetitionState _state ) {
        state = (MatchState) _state;
        if ( !addedVictoryConditions.get() && _state == tinState ) 
            addVictoryConditions();
        times.put( state, System.currentTimeMillis() );
    }

    @Override
    public Long getTime(CompetitionState _state) { return times.get( _state ); }
      
    public List<ArenaTeam> getAliveTeams() {
        List<ArenaTeam> alive = new ArrayList<>();
        for (ArenaTeam t: teams){
            if (t.isDead())
                continue;
            alive.add(t);
        }
        return alive;
    }

    public Set<ArenaPlayer> getAlivePlayers() {
        HashSet<ArenaPlayer> players = new HashSet<>();
        for (ArenaTeam t: teams){
            if (t.isDead())
                continue;
            players.addAll(t.getLivingPlayers());
        }
        return players;
    }

    public SpawnLocation getTeamSpawn(ArenaTeam team, boolean random){
        return random ? arena.getSpawn( -1, true )
                      : arena.getSpawn( team.getIndex(), false );
    }   
    @Override
    public SpawnLocation getSpawn(int index, boolean random) {
        return random ? arena.getSpawn( -1, true )
                      : arena.getSpawn( index, false );
    }
    public SpawnLocation getWaitRoomSpawn(ArenaTeam team, boolean random){
        return random ? arena.getRandomWaitRoomSpawnLoc()
                      : arena.getWaitRoomSpawnLoc( team.getIndex() );
    }
    public SpawnLocation getWaitRoomSpawn(int index, boolean random){
        return random ? arena.getRandomWaitRoomSpawnLoc()
                      : arena.getWaitRoomSpawnLoc( index );
    }

    public void endMatchWithResult(MatchResult result){
        matchResult = result;
        matchWinLossOrDraw(result);
    }

    public void setVictor(ArenaPlayer p){
        ArenaTeam t = getTeam( p );
        
        if ( t != null ) setVictor( t );
    }

    /**
     * Set the victor of this match
     * @param team ArenaTeam
     */
    public synchronized void setVictor(final ArenaTeam team){
        setVictor(new ArrayList<>(Arrays.asList(team)));
    }

    public synchronized void setVictor(final Collection<ArenaTeam> winningTeams){
        
        if ( individualWins ) {
            MatchResult result = new MatchResult();
            result.setVictors(winningTeams);
            endMatchWithResult(result);
        } 
        else {
            matchResult.setVictors(winningTeams);
            ArrayList<ArenaTeam> losers= new ArrayList<>(teams);
            losers.removeAll(winningTeams);
            matchResult.addLosers(losers);
            matchResult.setResult(WLT.WIN);
            endMatchWithResult(matchResult);
        }
    }

    public synchronized void setDraw(){
        matchResult.setResult(WLT.TIE);
        matchResult.setDrawers(teams);
        endMatchWithResult(matchResult);
    }

    public synchronized void setLosers(){
        matchResult.setResult(WLT.LOSS);
        matchResult.setLosers(teams);
        endMatchWithResult(matchResult);
    }

    public synchronized void setNonEndingVictor(final ArenaTeam team){
        MatchResult result = new MatchResult();
        result.setVictor(team);
        nonEndingMatchWinLossOrDraw(result);
    }

    public Set<ArenaTeam> getVictors() { return matchResult.getVictors(); }
    public synchronized Set<ArenaTeam> getLosers() { return matchResult.getLosers(); }
    public Set<ArenaTeam> getDrawers() { return matchResult.getDrawers(); }
    public int indexOf(ArenaTeam t) { return teams.indexOf(t); }

    @Override
    public boolean isHandled(ArenaPlayer player) {
        GameManager gm = GameManager.getGameManager(params);
        boolean b = gm.isHandled(player);
        return isInMatch(player) || b;
    }

    protected Set<ArenaPlayer> checkReady(final ArenaTeam t, StateOptions mo) {
        Set<ArenaPlayer> alive = new HashSet<>();
        for (ArenaPlayer p : t.getPlayers()){
            if (checkReady(p,t,mo,true)){
                alive.add(p);}
        }
        return alive;
    }

    @Override
    public boolean checkReady(ArenaPlayer p, final ArenaTeam t, StateOptions mo, boolean announce) {
        boolean online = p.isOnline();
        boolean inBed = p.getPlayer().isSleeping();
        boolean inmatch = isInMatch(p);
        final String pname = p.getDisplayName();
        boolean ready = true;
        World w = arena.getSpawn(0,false).getLocation().getWorld();
        if ( Defaults.DEBUG ) Log.info( p.getName() + "  online=" + online + " isready=" + tops.playerReady(p,w) );
        if ( !online ) {
            t.sendToOtherMembers(p,"&4!!! &eYour teammate &6"+pname+"&e was killed for not being online");
            ready = false;
        } 
        else if ( inBed ) {
            t.sendToOtherMembers(p,"&4!!! &eYour teammate &6"+pname+"&e was killed for being in bed while the match starts");
            ready = false;
        } 
        else if ( p.isDead() ) {
            t.sendToOtherMembers(p,"&4!!! &eYour teammate &6"+pname+"&e was killed for being dead while the match starts");
            ready = false;
        } 
        else if ( !inmatch && !tops.playerReady(p,w) ) { /// Players are about to be teleported into arena
            t.sendToOtherMembers(p,"&4!!! &eYour teammate &6"+pname+"&e was killed for not being ready");
            String notReady = tops.getRequiredString(p,w,"&eYou needed");
            MessageUtil.sendMessage(p,"&eYou didn't compete because of the following.");
            MessageUtil.sendMessage(p,notReady);
            ready = false;
        }
        if ( !ready ) {
            t.killMember(p);
        }
        return ready;
    }

    public void sendMessage(String string) {
        for (ArenaTeam t: teams)
            t.sendMessage(string);
    }

    public String getMatchInfo() {
        StateOptions to = params.getStateOptions(state);
        StringBuilder sb = new StringBuilder("ArenaMatch " + this.toString() +" ");
        sb.append(params).append("\n");
        sb.append("state=&6").append(state).append("\n");
        sb.append("pvp=&6").append(to != null ? to.getPVP() : "on").append("\n");
        //		sb.append("playersInMatch=&6"+inMatch.get(p)+"\n");
        sb.append("result=&e(").append(matchResult).append("&e)\n");
        
        List<ArenaTeam> deadATeams = new ArrayList<>();
        List<ArenaTeam> aliveTeams = new ArrayList<>();
        
        for (ArenaTeam t: teams){
            if (t.size() == 0)
                continue;
            if (t.isDead())
                deadATeams.add(t);
            else
                aliveTeams.add(t);
        }
        for (ArenaTeam t: aliveTeams) sb.append(t.getTeamInfo(inMatch)).append("\n");
        for (ArenaTeam t: deadATeams) sb.append(t.getTeamInfo(inMatch)).append("\n");
        return sb.toString();
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder("[Match:"+id+":" + (arena != null ? arena.getName():"none") +" ,");
        for (ArenaTeam t: teams){
            sb.append("[").append(t.getDisplayName()).append("] ,");}
        sb.append("]");
        return sb.toString();
    }

    public boolean hasTeam(ArenaTeam team) { return teams.contains( team ); }

    public void timeExpired() {
        MatchFindCurrentLeaderEvent event = new MatchFindCurrentLeaderEvent(this,teams, true);
        callEvent(event);
        MatchResult result = event.getResult();
        /// No one has an opinion of how this match ends... so declare it a draw
        if (result.isUnknown() || (result.getDrawers().isEmpty() &&
                result.getLosers().isEmpty() && result.getVictors().isEmpty())){
            result = defaultObjective.getMatchResult(this);
        }

        matchMessager.sendTimeExpired();
        endingMatchWinLossOrDraw(result);
    }

    public void intervalTick(int remaining) {
        MatchFindCurrentLeaderEvent event = new MatchFindCurrentLeaderEvent(this,teams,false);
        callEvent(event);
        callEvent(new MatchTimerIntervalEvent(this, remaining));
        matchMessager.sendOnIntervalMsg(remaining, event.getCurrentLeaders());
    }

    public void secondTick(int remaining) {
        ArenaObjective obj = scoreboard.getObjective(SAPIDisplaySlot.SIDEBAR);
        if (obj != null){
            obj.setDisplayNameSuffix(" &e("+remaining+")");}
    }

    public AbstractJoinHandler getTeamJoinHandler() { return joinHandler; }
    public boolean hasWaitroom() { return arena.getWaitroom() != null; }
    public boolean hasSpectatorRoom() { return arena.getSpectatorRoom() != null; }

    public boolean isJoinablePostCreate(){
        return  joinHandler != null 
                && (    alwaysOpen 
                        || tops.hasOptionAt( MatchState.ONJOIN, TransitionOption.ALWAYSJOIN ) 
                        || !joinHandler.isFull() ) 
                        || params.hasOptionAt( MatchState.ONJOIN, TransitionOption.TELEPORTIN );
    }

    public boolean canStillJoin() {
        return  joinHandler != null 
                && (    (   alwaysOpen 
                            || tops.hasOptionAt(MatchState.ONJOIN, TransitionOption.ALWAYSJOIN) 
                            || (    (   hasWaitroom() 
                                        || hasSpectatorRoom() ) 
                                    && !joinHandler.isFull() 
                                    && (    isInWaitRoomState() 
                                            && (    joinCutoffTime == null 
                                            || System.currentTimeMillis() < joinCutoffTime ) ) ) ) 
                        || (    params.hasOptionAt( MatchState.ONJOIN, TransitionOption.TELEPORTIN ) 
                                && state.ordinal() < MatchState.ONSTART.ordinal() )                );
    }


    @Override
    public String getName() { return params.getName(); }


    public void killTeam(int teamIndex) {
        if (teams.size() <= teamIndex)
            return;
        ArenaTeam t = teams.get(teamIndex);
        if (t == null)
            return;
        killTeam(t);
    }

    public void killTeam(ArenaTeam t){
        for (ArenaPlayer ap: t.getLivingPlayers()){
            ArenaPlayerDeathEvent apde = new ArenaPlayerDeathEvent(ap,t);
            apde.setExiting(true);
            callEvent(apde);
        }
    }

    public boolean allTeamsReady() {
        for (ArenaTeam t: teams){
            if (!t.isReady())
                return false;
        }
        return true;
    }

    @Override
    public LocationType getLocationType() { return LocationType.ARENA; }

    public List<ArenaPlayer> getNonLeftPlayers() {
        List<ArenaPlayer> players = new ArrayList<>();
        for (ArenaTeam at: this.getTeams()){
            for (ArenaPlayer ap: at.getPlayers()){
                if (at.hasLeft(ap))
                    continue;
                players.add(ap);
            }
        }
        return players;
    }
   
    public void setOldArenaParams(MatchParams oldArenaParams) {
        if ( oldArenaState == null ) {
            oldArenaState = new ArenaPreviousState(arena);}
        oldArenaState.matchParams = oldArenaParams;
    }
    
   
    class ArenaPreviousState{
        final ContainerState waitroomCS;
        final ContainerState arenaCS;
        MatchParams matchParams;

        public ArenaPreviousState(Arena aren) {
            waitroomCS = (aren.getWaitroom()!=null) ? aren.getWaitroom().getContainerState() : null;
            arenaCS = aren.getContainerState();
        }

        public void revert(Arena aren) {
            if(matchParams != null)
                aren.setParams(matchParams);
            aren.setContainerState(arenaCS);
            if (waitroomCS != null && aren.getWaitroom()!=null){
                aren.getWaitroom().setContainerState(waitroomCS);}
        }
    }
}
