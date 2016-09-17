package mc.alk.arena.competition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.event.Listener;
import org.bukkit.util.ChatPaginator;

import lombok.Getter;
import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.controllers.BattleArenaController;
import mc.alk.arena.controllers.ParamController;
import mc.alk.arena.controllers.Scheduler;
import mc.alk.arena.controllers.joining.TeamJoinFactory;
import mc.alk.arena.events.events.TournamentRoundEvent;
import mc.alk.arena.events.matches.MatchCancelledEvent;
import mc.alk.arena.events.matches.MatchCompletedEvent;
import mc.alk.arena.events.matches.MatchCreatedEvent;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.EventParams;
import mc.alk.arena.objects.EventState;
import mc.alk.arena.objects.MatchResult;
import mc.alk.arena.objects.MatchState;
import mc.alk.arena.objects.Matchup;
import mc.alk.arena.objects.StateGraph;
import mc.alk.arena.objects.arenas.ArenaListener;
import mc.alk.arena.objects.events.ArenaEventHandler;
import mc.alk.arena.objects.exceptions.NeverWouldJoinException;
import mc.alk.arena.objects.joining.MatchTeamQObject;
import mc.alk.arena.objects.options.EventOpenOptions;
import mc.alk.arena.objects.options.JoinOptions;
import mc.alk.arena.objects.options.StateOptions;
import mc.alk.arena.objects.stats.ArenaStat;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.tracker.Tracker;
import mc.alk.arena.tracker.TrackerInterface;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.MessageUtil;

public class TournamentEvent extends AbstractComp implements Listener, ArenaListener {
    public long timeBetweenRounds;

    int curRound = -1;
    @Getter int nrounds = -1;
    boolean preliminary_round = false;
    ArrayList<ArenaTeam> aliveTeams = new ArrayList<>();
    ArrayList<ArenaTeam> competingTeams = new ArrayList<>();
    final EventParams singleGameParms; 
    Random rand = new Random();
    Integer curTimer = null;
    Map<Match, Matchup> matchups = Collections.synchronizedMap(new HashMap<Match,Matchup>());
    Set<Matchup> incompleteMatchups = new HashSet<>();
    final ArrayList<Round> rounds = new ArrayList<>(); 

    public TournamentEvent(EventParams _params, EventOpenOptions eoo) throws NeverWouldJoinException {
        super(_params);
        singleGameParms = new EventParams(eoo.getParams());
        Bukkit.getPluginManager().registerEvents(this, BattleArena.getSelf());
        timeBetweenRounds = _params.getTimeBetweenRounds();
        ChatColor color = MessageUtil.getFirstColor(singleGameParms.getPrefix());
        _params.setTeamSize(singleGameParms.getTeamSize());

        String str = color + "[" + singleGameParms.getName() + " " + _params.getName() + "]";
        _params.setPrefix(str);
        singleGameParms.setPrefix(str);

        str = singleGameParms.getName() + " " + _params.getName();
        _params.setName(str);
        singleGameParms.setName(str);
        setTeamJoinHandler(TeamJoinFactory.createTeamJoinHandler(_params, this));
    }

    @Override
    public void openEvent(){
        super.openEvent();
        aliveTeams.clear();
        competingTeams.clear();
        rounds.clear();
        curRound = -1;
        nrounds = -1;
        teamJoinHandler.removeImproperTeams();
    }

    static int getNTeams(Collection<ArenaTeam> teams) {
        int size = 0;
        for (ArenaTeam at : teams){
            if (at != null && at.size() > 0) {
                size++;
            }
        }
        return size;
    }

    @Override
    public void startEvent() {
        super.startEvent();
        Server server = Bukkit.getServer();
        int osize = getNTeams(teams);
        nrounds = calcNRounds(osize);
        final int minTeams = params.getMinTeams();
        int roundteams = (int) Math.pow(minTeams, nrounds);
        
        server.broadcastMessage( 
                Log.colorChat( params.getPrefix() + "&e The " + singleGameParms.getName() + " is starting!"));

        TreeMap<Double,ArenaTeam> sortTeams = new TreeMap<>(Collections.reverseOrder());
        TrackerInterface sc = Tracker.getInterface( params );

        for (ArenaTeam t: teams) {
            if (t.size() <= 0) {
                continue;
            }
            ArenaStat stat = sc.getTeamRecord(t.getName());
            Double elo = (double) stat.getRating();
            while (sortTeams.containsKey(elo)) {
                elo += 0.0001;
            }
            sortTeams.put(elo, t);
        }
        teams.clear();
        aliveTeams.clear();
        ArrayList<ArenaTeam> ts = new ArrayList<>(sortTeams.values());
        
        for (ArenaTeam t: ts){
            teams.add(t);
            aliveTeams.add(t);
            competingTeams.add(t);
        }
        removeExtraneous();
        preliminary_round = teams.size() != roundteams;
        
        if (preliminary_round) nrounds++;
        
        server.broadcastMessage( Log.colorChat( params.getPrefix() + "&6 " + teams.size() + 
                                            " &e" + MessageUtil.getTeamsOrPlayers( teams.size() ) +
                                            " will compete in a &6" + nrounds + "&e round tournament" ) );
        if ( preliminary_round ) 
            makePreliminaryRound();
        else
            makeNextRound();
        
        startRound();
    }

    @ArenaEventHandler( end = MatchState.ONOPEN )
    public void onMatchCreatedEvent(MatchCreatedEvent event) {
        Matchup matchup = (((MatchTeamQObject) event.getOriginalObject().getOriginalQueuedObject()).getMatchup());
        matchups.put(event.getMatch(), matchup);
    }

    @ArenaEventHandler
    public void matchCompleted(MatchCompletedEvent event) { matchEnded( event.getMatch() ); }

    @ArenaEventHandler
    public void matchCancelled(MatchCancelledEvent event) { matchEnded( event.getMatch() ); }

    @Override
    public void endEvent(){
        super.endEvent();
        aliveTeams.clear();
        competingTeams.clear();
        matchups.clear();
        if (curTimer != null){
            Bukkit.getScheduler().cancelTask(curTimer);
            curTimer = null;
        }
    }

    private void matchEnded(Match am) {
        if (state == EventState.CLOSED || state == EventState.FINISHED)
            return;
        Matchup m = matchups.get(am);
        if ( m == null ) {
            cancelEvent();
            Log.err("[BA Error] match completed but not found in tournament");
            return;
        }
        incompleteMatchups.remove(m);
        MatchResult nmr = createNewMatchResult(am);
        if (nmr.getVictors().isEmpty()) { 
            return;
        }
        ArenaTeam victor = nmr.getVictors().iterator().next(); /// single winner
        m.setResult(nmr);
        for (ArenaTeam t: nmr.getLosers()){
            super.removedTeam(t);
        }
        aliveTeams.removeAll(nmr.getLosers());

        if (incompleteMatchups.isEmpty()){
            if (Defaults.DEBUG) Log.info("ROUND FINISHED !!!!!   " + aliveTeams);

            if (curRound +1 == nrounds || isFinished()){
                HashSet<ArenaTeam> losers = new HashSet<>(competingTeams);
                losers.remove(victor);
                Set<ArenaTeam> victors = new HashSet<>(Arrays.asList(victor));
                MatchResult result = new MatchResult();
                result.setVictors(victors);
                setEventResult(result,true);
                TransitionController.transition(am, TournamentTransition.FIRSTPLACE, victors, false);
                TransitionController.transition(am, TournamentTransition.PARTICIPANTS, losers, false);
                eventCompleted();
            } 
            else {
                callEvent(new TournamentRoundEvent(this, curRound));
                makeNextRound();
                startRound();
            }
        }
    }

    private MatchResult createNewMatchResult(Match match) {
        MatchResult r = match.getMatchResult();
        MatchResult nmr;
        if (r.isDraw() || r.isUnknown()) { /// match was a draw, pick a random lucky winner
            nmr = createRandomWinner(r.getDrawers(), match);
            nmr.addLosers(r.getLosers());
        } 
        else if (r.hasVictor() && r.getVictors().size() != 1) {
            nmr = createRandomWinner(r.getVictors(), match);
            nmr.addLosers(r.getLosers());
            nmr.addLosers(r.getDrawers());
        } 
        else {
            nmr = r;
        }
        return nmr;
    }

    private MatchResult createRandomWinner(Collection<ArenaTeam> randos, Match match) {
        MatchResult mr = new MatchResult();
        ArenaTeam victor = null;

        List<ArenaTeam> ls = new ArrayList<>();
        for (ArenaTeam at : randos){
            if (at.size() == 0)
                continue;
            ls.add(at);
        }
        if (ls.isEmpty()) {
            List<ArenaPlayer> lp = new ArrayList<>();
            for (ArenaTeam at: match.getTeams()) {
                lp.addAll(at.getLeftPlayers());
            }
            ArenaPlayer v = lp.isEmpty() ? null : lp.get(rand.nextInt(lp.size()));
            
            if (v != null) {
                victor = match.getLeftTeam(v);}
            
            if (victor == null){
                Log.err("[BattleArena] Tournament found a match with no players, cancelling tournament");
                this.cancelEvent();
                return mr;
            }
        } 
        else victor = ls.get( rand.nextInt( ls.size() ) );

        victor.sendMessage("&2You drew your match but have been randomly selected as the winner!");
        mr.setVictor(victor);
        Set<ArenaTeam> losers = new HashSet<>(ls);
        losers.remove(victor);
        mr.addLosers(losers);
        for (ArenaTeam l : losers) {
            l.sendMessage("&cYou drew your match but someone else has been randomly selected as the winner!");
        }
        return mr;
    }

    private void removeExtraneous(){

        int minTeams = params.getMinTeams();
        final int needed_size = (int) Math.pow(minTeams, nrounds);
        final int nprelims = (teams.size() - needed_size) / (minTeams-1);

        int remaining = teams.size() - (needed_size + nprelims*(minTeams-1));
        if (remaining > 0){
            List<ArenaTeam> newTeams = new ArrayList<>();
            for (int i=0;i<remaining;i++){
                ArenaTeam t = teams.get(needed_size + i);
                newTeams.add(t);
                t.sendMessage("&c[Tourney] There weren't enough players for you to compete in this tourney");
            }
            teams.removeAll(newTeams);
            aliveTeams.removeAll(newTeams);
        }
    }

    private void makePreliminaryRound() {
        Matchup m;
        curRound++;
        incompleteMatchups.clear();
        Round tr = new Round();
        rounds.add(tr);
        int nRounds = calcNRounds(teams.size()) + 1;
        int minTeams = params.getMinTeams();
        final int needed_size = (int) Math.pow(minTeams, nRounds-1);
        final int nprelims = (aliveTeams.size() - needed_size) / (minTeams-1);

        int loffset = needed_size -1;
        int hoffset = needed_size;

        for (int i = 0;i< nprelims;i++){
            List<ArenaTeam> newTeams = new ArrayList<>();
            
            for (int j=0;j<minTeams/2;j++){
                newTeams.add(aliveTeams.get(loffset));
                newTeams.add(aliveTeams.get(hoffset));
                loffset--;
                hoffset++;
            }
            EventParams sgp = ParamController.copyParams(singleGameParms);
            JoinOptions jo = new JoinOptions();
            jo.setMatchParams(sgp);
            m = createMatchup(sgp, newTeams, jo);
            tr.addMatchup(m);
            incompleteMatchups.add(m);
        }
    }

    private Matchup createMatchup(EventParams matchupParams, Collection<ArenaTeam> _teams, JoinOptions jo){
        Matchup m = new Matchup(matchupParams,_teams, jo);
        Collection<ArenaListener> li = 
                methodController.getArenaListeners() != null ? new ArrayList<>(methodController.getArenaListeners()) 
                                                             : new ArrayList<>();
        for (ArenaListener al : li){
            m.addArenaListener(al);
        }
        m.addArenaListener(this);
        return m;
    }
    
    private void makeNextRound() {
        Matchup m;
        curRound++;
        incompleteMatchups.clear();
        Round tr = new Round();
        rounds.add(tr);
        int minTeams = params.getMinTeams();
        int size = aliveTeams.size();
        final int nMatches = size/minTeams;
        
        for ( int i = 0; i < nMatches; i++ ) {
            List<ArenaTeam> newTeams = new ArrayList<>();
            
            for ( int j = 0; j < minTeams / 2; j++ ) {
                int index = i + j * nMatches;
                newTeams.add( aliveTeams.get( index ) );
                newTeams.add( aliveTeams.get( size - 1 - index ) );
            }
            JoinOptions jo = new JoinOptions();
            EventParams sgp = ParamController.copyParams(singleGameParms);
            jo.setMatchParams(sgp);
            m = createMatchup(sgp, newTeams, jo);
            tr.addMatchup(m);
            incompleteMatchups.add(m);
        }
    }

    public boolean startRound() {

        if ( curRound < 0 || state == EventState.CLOSED )
            return false;
        
        announceRound();

        /// Section to start the match
        curTimer = Scheduler.scheduleSynchronousTask( 
                    () -> { Round tr = rounds.get(curRound);
                            BattleArenaController ac = BattleArena.getBAController();
                            
                            for ( Matchup m : tr.getMatchups() ) 
                                ac.addMatchup( new MatchTeamQObject( m ) );  
                              
                          }, timeBetweenRounds * 20L );
        return true;
    }

    private void announceRound() {
        Round tr = rounds.get(curRound);

        String strround = preliminary_round && curRound ==0 ? "PreliminaryRound" : ("Round "+(curRound +1));
        if (curRound +1 == nrounds){
            strround = "Final Round";}
        if (preliminary_round){
            preliminary_round = false;
            int nprelims = tr.getMatchups().size()*params.getMinTeams();
            for (int i=0;i< aliveTeams.size()-nprelims;i++){
                ArenaTeam t = aliveTeams.get(i);
                t.sendMessage("&4[" + strround + "]&e You have a &5bye&e this round");
            }
        }
        TrackerInterface sc = Tracker.getInterface( params );
        String prefix = params.getPrefix();
        
        if (tr.getMatchups().size() <= 8){
            
            for (Matchup m: tr.getMatchups()){
                
                List<String> names = new ArrayList<>();
                
                for (ArenaTeam t: m.getTeams()){
                    ArenaStat st = sc.getTeamRecord(t.getName());
                    names.add( "&8" + t.getDisplayName() + "&6[" + st.getRating() + "]") ;
                }
                String msg = "&e" + strround + ": " + String.join(" vs ", names);
                
                if (ChatPaginator.GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH > msg.length() + prefix.length())
                    broadcastAlive( prefix + " " + msg );
                else 
                    broadcastAlive(msg);             
            }
        } 
        else broadcastAlive( prefix + "&e Round " + strround + " has " + tr.getMatchups().size() + " " +
                    MessageUtil.teamsOrPlayers(params.getMinTeamSize())+" competing. &6/tourney status:&e for updates");
        
        if (curRound != nrounds)
            broadcast(prefix+"&e "+strround+" will start in &4" + timeBetweenRounds +" &eseconds!");
        else
            broadcast(prefix +"&e The "+strround+" will start in &4" + timeBetweenRounds +" &eseconds!");
    }

    @Override
    public void broadcast(String msg) { for (ArenaTeam t : competingTeams) t.sendMessage(msg); }
    public void broadcastAlive(String msg) { for (ArenaTeam t : aliveTeams) t.sendMessage(msg); }
 
    @Override
    public void addedToTeam(ArenaTeam team, ArenaPlayer ap) {
        super.addedToTeam(team, ap);
        if (team.size() == 1) { /// it's finally a valid team
            announceTourneySize();
        }
    }
    @Override
    public boolean addedTeam(ArenaTeam team){
        if (super.addedTeam(team)){
            announceTourneySize();
            return true;
        }
        return false;
    }

    private void announceTourneySize() {
        int size = 0;
        for (ArenaTeam t: teams){
            if (t.size() > 0)
                size++;
        }
        int calcNrounds = calcNRounds(size);
        int idealteam = (int) Math.pow(params.getMinTeams(), calcNrounds);
        if (calcNrounds > 1 && size % idealteam == 0){
            MessageUtil.broadcastMessage(Log.colorChat(params.getPrefix()+"&6" + size +" "+MessageUtil.getTeamsOrPlayers(teams.size())+
                    "&e have joined, Current tournament will have &6" + calcNrounds+"&e rounds"));
        }
    }

    private int calcNRounds( int size ){ return (int) Math.floor( Math.log( size ) / Math.log( params.getMinTeams() ) ); }

    @Override
    public boolean canLeave(ArenaPlayer p) {
        ArenaTeam t = getTeam(p);
        return isOpen() || (t != null && !aliveTeams.contains(t));
    }

    @Override
    public String getStatus() { return getStatus(curRound); }

    public String getStatus(int round) {
        StringBuilder sb = new StringBuilder();
        sb.append(super.getStatus());
        if (round <0){
            return sb.toString();}
        sb.append("&e Alive Teams=&6 ").append(aliveTeams.size()).append("\n");
        Round tr = rounds.get(round);
        int ncomplete = tr.getCompleteMatchups().size();
        final int total = tr.getMatchups().size();
        sb.append(preliminary_round && round==0 ? "&ePreliminaryRound" : "&eRound");
        sb.append("&4 ").append(round + 1).append(" &eComplete Matches: &6 ").append(ncomplete).append("/").append(total);
        return sb.toString();
    }

    @Override
    public EventState getState() { return null; }
    @Override
    public boolean isHandled(ArenaPlayer player) { return false; }
    @Override
    public LocationType getLocationType() { return LocationType.ARENA; }

    /**
     * Show Results from the previous Event
     * @return result
     */
    @Override
    public String getResultString() {
        StringBuilder sb = new StringBuilder();
        if (rounds.isEmpty()){
            return "&eThere are no results yet";
        }
        if (!isFinished() && !isClosed()){
            sb.append("&eEvent is still &6").append(state).append("\n");
        }

        boolean useRounds = rounds.size() > 1;
        for (int r = 0;r<rounds.size();r++) {
            
            Round round = rounds.get(r);
            if (useRounds) sb.append("&5***&4 Round ").append(r + 1).append("&5 ***\n");
            
            boolean useMatchups = round.getMatchups().size() > 1;
            
            for (Matchup m: round.getMatchups()) {
                
                if (useMatchups) sb.append("&4Matchup :");
                
                MatchResult result = m.getResult();
                
                if (result == null || result.getVictors() == null) {
                    
                    for (ArenaTeam t: m.getTeams())
                        sb.append(t.getTeamSummary()).append(" "); 
                    
                    sb.append("\n");
                } 
                else sb.append( result.toPrettyString() ).append("\n");
            } 
        }
        return sb.toString();
    }

    @Override
    public String getInfo() {
        StringBuilder sb = new StringBuilder().append( StateOptions.getInfo(singleGameParms, singleGameParms.getName()) );
        StateGraph so = singleGameParms.getStateGraph();
        
        String firstPlacePrizes = so.getGiveString(TournamentTransition.FIRSTPLACE);
        String participantPrizes = so.getGiveString(TournamentTransition.PARTICIPANTS);
        
        if ( participantPrizes != null ) 
            sb.append("\n&ePrize for &6participation:&e ").append( participantPrizes );
        
        if ( firstPlacePrizes != null ) 
            sb.append("\n&ePrize for getting &b1st &eplace:").append( firstPlacePrizes );
        
        return sb.toString();
    }
}
