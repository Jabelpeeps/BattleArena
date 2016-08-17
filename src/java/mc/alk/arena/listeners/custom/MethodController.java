package mc.alk.arena.listeners.custom;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import mc.alk.arena.Defaults;
import mc.alk.arena.events.BAEvent;
import mc.alk.arena.listeners.custom.RListener.RListenerPriorityComparator;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchState;
import mc.alk.arena.objects.arenas.ArenaListener;
import mc.alk.arena.objects.events.ArenaEventHandler;
import mc.alk.arena.objects.events.ArenaEventMethod;
import mc.alk.arena.objects.events.ArenaEventPriority;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.MapOfTreeSet;
import mc.alk.arena.util.MessageUtil;
import mc.alk.arena.util.ServerUtil;
import mc.alk.arena.util.TimingUtil;
import mc.alk.arena.util.Util;
import mc.alk.arena.util.TimingUtil.TimingStat;


public class MethodController {

    /** Our Dynamic listeners, listening for bukkit events*/
    final static EnumMap<EventPriority, HashMap<Type, BukkitEventHandler>> bukkitListeners =
            new EnumMap<>(EventPriority.class);

    /** Our registered bukkit events and the methods to call when they happen*/
    final static HashMap<Class<? extends ArenaListener>,HashMap<Class<? extends Event>,List<ArenaEventMethod>>> bukkitEventMethods =
            new HashMap<>();

    /** Our registered match events and the methods to call when they happen*/
    final static HashMap<Class<? extends ArenaListener>,HashMap<Class<? extends BAEvent>,List<ArenaEventMethod>>> matchEventMethods =
            new HashMap<>();

    final private HashMap<Class<? extends Event>,List<RListener>> bukkitMethods = new HashMap<>();

    final private HashMap<Class<? extends BAEvent>,List<RListener>> matchMethods = new HashMap<>();

    final static Set<MethodController> controllers = new HashSet<>();
    static int controllerCount = 0;

    final Set<ArenaListener> listeners = new HashSet<>();
    final Object owner;
    final BAEventCaller baexecutor;
    static TimingUtil timings;
    MatchState curState = MatchState.NONE;

    public MethodController(Object owner){
        if (Defaults.DEBUG_EVENTS) controllers.add(this);
        this.owner = owner;
        controllerCount++;
        if (Bukkit.getPluginManager().useTimings() || Defaults.DEBUG_TIMINGS){
            
            if (timings == null) 
                timings = new TimingUtil();

            baexecutor = event -> {
                
                    TimingStat t = timings.getOrCreate(event.getClass().getSimpleName());
                    long startTime = System.nanoTime();

                    Class<?> clazz = event.getClass();
                    for (HashMap<Type,BukkitEventHandler> ls : bukkitListeners.values()){
                        BukkitEventHandler beh = ls.get(clazz);
                        if (beh == null)
                            continue;
                        beh.invokeArenaEvent(listeners, event);
                    }
                    event.callEvent();
                    t.count+=1;
                    t.totalTime += System.nanoTime() - startTime;       
                    };                   
        } 
        else {
            baexecutor = event -> {
 
                    Class<?> clazz = event.getClass();
                    
                    for (HashMap<Type,BukkitEventHandler> ls : bukkitListeners.values()){
                        
                        BukkitEventHandler beh = ls.get(clazz);
                        
                        if (beh == null) continue;
                        
                        beh.invokeArenaEvent(listeners, event);
                    }
                    event.callEvent(); 
                    };
        }
    }

    public static EnumMap<EventPriority, HashMap<Type, BukkitEventHandler>> getEventListeners() {
        return bukkitListeners;
    }

    public void updateEvents(MatchState matchState, ArenaPlayer player){
        List<UUID> players = new ArrayList<>();
        players.add(player.getUniqueId());
        updateEvents(null, matchState,players);
    }

    public void updateEvents(MatchState matchState, Collection<ArenaPlayer> players){
        List<UUID> strplayers = new ArrayList<>();
        for (ArenaPlayer ap: players){
            strplayers.add(ap.getUniqueId());}
        updateEvents(null, matchState,strplayers);
    }

    public void updateEvents(ArenaListener listener, MatchState matchState, Collection<ArenaPlayer> players){
        List<UUID> strplayers = new ArrayList<>();
        for (ArenaPlayer ap: players){
            strplayers.add(ap.getUniqueId());}
        updateEvents(listener, matchState,strplayers);
    }

    public void updateEventsRange(ArenaListener listener, MatchState beginState, MatchState endState, List<UUID> players) {
        for (MatchState ms : MatchState.values()){
            if (ms.ordinal() < beginState.ordinal())
                continue;
            if (ms.ordinal() > endState.ordinal())
                break;
            updateEvents(listener,ms, players);
        }
    }

    public void updateEvents(ArenaListener listener, MatchState matchState, List<UUID> players) {
        try {
            Collection<Class<? extends Event>> keys = bukkitMethods.keySet();
            for (Class<? extends Event> event: keys){
                updateEvent(listener, matchState, players, event);}
            Collection<Class<? extends BAEvent>> mkeys = matchMethods.keySet();
            for (Class<? extends BAEvent> event: mkeys){
                updateBAEvent(listener, matchState, players, event);}
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    /**
     *
     * @param matchState MatchState
     * @param player ArenaPlayer
     * @param events Events
     */
    public void updateSpecificEvents(MatchState matchState, ArenaPlayer player, Class<? extends Event>... events) {
        try {
            List<UUID> players = new ArrayList<>();
            players.add(player.getUniqueId());

            for (Class<? extends Event> event: events){
                updateEvent(null, matchState,players, event);}
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    private void updateEvent(ArenaListener listener, MatchState matchState,
                             Collection<UUID> players, final Class<? extends Event> event) {
        final List<RListener> rls = bukkitMethods.get(event);
        if (rls == null || rls.isEmpty()){
            return;}
        if (Defaults.DEBUG_EVENTS) System.out.println("updateEventListener "+  event.getSimpleName() +"    " + matchState);

        for (RListener rl : rls){
            if (listener != null && !rl.getListener().equals(listener))
                continue;
            ArenaEventMethod mem = rl.getMethod();
            if (Defaults.DEBUG_EVENTS) System.out.println("  updateEventListener "+  event.getSimpleName() +
                    "    " + matchState +":" +rl +",   " + players);
            if (mem.getBeginState() == matchState){
                BukkitEventHandler bel = getCreate(event,mem);
                bel.addListener(rl,players);
            } else if (mem.getEndState() == matchState) {
                for (HashMap<Type,BukkitEventHandler> ls : bukkitListeners.values()){
                    BukkitEventHandler bel = ls.get(event);
                    if (bel != null){
                        bel.removeListener(rl, players);
                    }
                }
            }
        }
    }

    private BukkitEventHandler getCreateBA(Class<? extends BAEvent> event, ArenaEventMethod mem){
        HashMap<Type,BukkitEventHandler> gels = bukkitListeners.get(mem.getBukkitPriority());
        if (gels == null){
            gels = new HashMap<>();
            bukkitListeners.put(mem.getBukkitPriority(), gels);
        }
        BukkitEventHandler gel = gels.get(event);
        if (Defaults.DEBUG_EVENTS) System.out.println("***************************** checking for " + event);

        if (gel == null){
            if (Defaults.DEBUG_EVENTS) System.out.println("***************************** making new gel for type " + event);
            gel = new BukkitEventHandler(event,mem);
            gels.put(event, gel);
        }
        return gel;
    }

    private void updateBAEvent(ArenaListener listener, MatchState matchState,
                               Collection<UUID> players, final Class<? extends BAEvent> event) {
        final List<RListener> rls = matchMethods.get(event);
        if (rls == null || rls.isEmpty()){
            return;}
        if (Defaults.DEBUG_EVENTS) System.out.println("updateBAEventListener "+  event.getSimpleName() +"    " + matchState);

        for (RListener rl : rls){
            if (listener != null && !rl.getListener().equals(listener))
                continue;
            ArenaEventMethod mem = rl.getMethod();
            if (Defaults.DEBUG_EVENTS) System.out.println("  updateBAEventListener "+  event.getSimpleName() +"    " + matchState +":" +
                    rl +",   " + players);
            if (mem.getBeginState() == matchState){
                BukkitEventHandler bel = getCreateBA(event,mem);
                bel.addListener(rl,players);
            } 
            else if (mem.getEndState() == matchState ) {
                for (HashMap<Type,BukkitEventHandler> ls : bukkitListeners.values()){
                    BukkitEventHandler bel = ls.get(event);
                    if (bel != null){
                        bel.removeListener(rl, players);
                    }
                }
            }
        }
    }

    private BukkitEventHandler getCreate(Class<? extends Event> event, ArenaEventMethod mem){
        HashMap<Type,BukkitEventHandler> gels = bukkitListeners.get(mem.getBukkitPriority());
        if (gels == null){
            gels = new HashMap<>();
            bukkitListeners.put(mem.getBukkitPriority(), gels);
        }
        BukkitEventHandler gel = gels.get(event);
        if (Defaults.DEBUG_EVENTS) 
            System.out.println("***************************** checking for " + event.getSimpleName());

        if (gel == null){
            if (Defaults.DEBUG_EVENTS) 
                System.out.println("***************************** making new gel for type " + event.getSimpleName());
            gel = new BukkitEventHandler(event,mem);
            gels.put(event, gel);
        }
        return gel;
    }

    public static List<ArenaEventMethod> getMethods(ArenaListener ael, Event event) {
        return getMethods(ael,event.getClass());
    }

    private static List<ArenaEventMethod> getMethods(ArenaListener ael, Class<? extends Event> eventClass) {
        HashMap<Class<? extends Event>,List<ArenaEventMethod>> typeMap = bukkitEventMethods.get(ael.getClass());
        if (Defaults.DEBUG_EVENTS) 
            System.out.println("!! getEvent "+ael.getClass()+ " " +eventClass+"  methods="+
                (typeMap==null?"null" :typeMap.size() +":"+ (typeMap.get(eventClass) != null ? typeMap.get(eventClass).size() : 0) ) );
        if (typeMap == null)
            return null;
        return typeMap.get(eventClass);
    }

    private static Map<Class<? extends Event>,List<ArenaEventMethod>> getBukkitMethods(ArenaListener ael) {
        if (Defaults.DEBUG_EVENTS) 
            System.out.println("!!!! getEvent "+ael.getClass().getSimpleName()+" contains=" + bukkitEventMethods.containsKey(ael.getClass()));
        return bukkitEventMethods.get(ael.getClass());
    }

    private static Map<Class<? extends BAEvent>,List<ArenaEventMethod>> getMatchMethods(ArenaListener ael) {
        if (Defaults.DEBUG_EVENTS) 
            System.out.println("!!!! getEvent "+ael.getClass().getSimpleName()+" contains=" + bukkitEventMethods.containsKey(ael.getClass()));
        return matchEventMethods.get(ael.getClass());
    }


    @SuppressWarnings({"unchecked"})
    private static void addMethods(Class<? extends ArenaListener> alClass){
        
        HashMap<Class<? extends Event>, List<ArenaEventMethod>> bukkitTypeMap = new HashMap<>();
        HashMap<Class<? extends BAEvent>, List<ArenaEventMethod>> matchTypeMap = new HashMap<>();

        Method[] methodArray = alClass.getMethods();

        for (Method method : methodArray){

            ArenaEventHandler aeh = method.getAnnotation(ArenaEventHandler.class);
            
            if (aeh == null) continue;

            MatchState beginState = aeh.begin();
            MatchState endState = aeh.end();
            MatchState cancelState = MatchState.NONE;
            boolean needsPlayer = aeh.needsPlayer();
//            final String entityMethod = aeh.entityMethod();
            boolean supressCastWarnings = aeh.suppressCastWarnings();
//            boolean suppressWarnings = aeh.suppressWarnings();
            EventPriority bukkitPriority = aeh.bukkitPriority();
            ArenaEventPriority priority = aeh.priority();

            /// Make sure there is some sort of bukkitEvent here
            Class<?>[] classes = method.getParameterTypes();
            if ( classes.length == 0 || !Event.class.isAssignableFrom( classes[0] ) ) {
                Log.info( "Bukkit Event was null for method " + method );
                continue;
            }

            Class<? extends Event> bukkitEvent = (Class<? extends Event>)classes[0];
            boolean baEvent = BAEvent.class.isAssignableFrom(bukkitEvent);

            boolean needsTeamOrPlayer;
            Method getPlayerMethod = null;
            Method getLivingMethod = null;
            Method getEntityMethod = null;

            if ( needsPlayer ){
                List<Method> playerMethods = new ArrayList<>();
                List<Method> entityMethods = new ArrayList<>();

                /// From our bukkitEvent. find any methods that return a Player, HumanEntity, or LivingEntity
                for ( Method eachMethod : bukkitEvent.getMethods() ) {
                    
//                    /// Check first for a specified method
//                    if ( !entityMethod.isEmpty() && eachMethod.getName().equals( entityMethod ) ){
//                        getPlayerMethod = eachMethod;
//                        break;
//                    }
                    
                    Type t = eachMethod.getReturnType();
                    if (    Player.class.isAssignableFrom((Class<?>) t) 
                            || HumanEntity.class.isAssignableFrom((Class<?>) t) 
                            || ArenaPlayer.class.isAssignableFrom((Class<?>) t) ) {
                        playerMethods.add( eachMethod );
                        getPlayerMethod = eachMethod;
                    } 
                    else if ( Entity.class.isAssignableFrom((Class<?>) t) ) {
                        entityMethods.add( eachMethod );
                        getLivingMethod = eachMethod;
                    }
                }
                /// If we haven't already found the specified player method.. try and get it from our lists
                if ( getPlayerMethod == null ) {
                    if ( !playerMethods.isEmpty() ) {
                        if ( playerMethods.size() > 1){
                            for ( Method eachMethod : playerMethods ) {

                                if ( eachMethod.getName().equals( "getPlayer" ) ) {
                                    Log.info( " Class " + alClass.getName() + " has multiple methods that return a player" );
                                    Log.info( " defaulting to getPlayer()" );
//                                    Log.info( " To specify use @ArenaEventHandler(entityMethod=\"methodWhichYouWantToUse\")" );
//                                    Log.info( " to suppress this warning @ArenaEventHandler(suppressWarnings=true)" );
                                    getPlayerMethod = eachMethod;
                                }
                            }
                            if ( getPlayerMethod == null ) {
                                Log.info( " Class " + alClass.getName() + " has multiple methods that return a player");
//                                Log.info( " Use @ArenaEventHandler(entityMethod=\"methodWhichYouWantToUse\")" );
                                return;
                            }
                        } 
                        else {
                            getPlayerMethod = playerMethods.get(0);
                        }
                    } 
                    else if ( !entityMethods.isEmpty() ) {
                        if ( bukkitEvent == EntityDeathEvent.class ) {
                            try {
                                getEntityMethod = EntityDeathEvent.class.getMethod( "getEntity" );
                            } 
                            catch (Exception e) {
                                Log.printStackTrace(e);
                                continue;
                            }
                        } 
                        else if ( entityMethods.size() > 1 && !EntityDamageEvent.class.isAssignableFrom( bukkitEvent ) ) {
                            Log.info( " Class " + alClass.getName() + " has multiple methods that return an entity" );
//                            Log.info( " Use @MatchEventHandler(entityMethod=\"methodWhichYouWantToUse\")" );
                            return;
                        } 
                        else {
                            getEntityMethod = entityMethods.get(0);
                        }
                    }
                }

            }

            /// Go over the rest of the parameters to see if we should give a Team or Player
            for ( int i =1; i < classes.length; i++ ) {
                Class<?> clazz = classes[i];
                needsTeamOrPlayer = Player.class.isAssignableFrom( clazz ) || ArenaTeam.class.isAssignableFrom( clazz );
                
                if ( !needsTeamOrPlayer ) continue;

                boolean noEntityMethod = getEntityMethod == null 
                                         && getLivingMethod == null 
                                         && getPlayerMethod == null;
                if ( noEntityMethod ) {
                    Log.info( " Class " + method.getName() + " needs a player or team, but the bukkitEvent " +
                            bukkitEvent.getCanonicalName() + "returns no player, and no entities. Class=" + alClass);
                    return;
                } 
                else if (getLivingMethod != null && !supressCastWarnings ) { //&& !suppressWarnings){
                    if (!EntityDamageByEntityEvent.class.isAssignableFrom(bukkitEvent))
                        Log.warn( "[BattleArena] Warning. " + alClass + ". Method " + method.getName() + 
                                " needs a player or team, but the bukkitEvent " + bukkitEvent.getCanonicalName() + 
                                " returns only a living entity. Cast to Player will be attempted at runtime" );
                } 
                else if (getEntityMethod != null && !supressCastWarnings ) { //&& !suppressWarnings){
                    if (!EntityDamageByEntityEvent.class.isAssignableFrom(bukkitEvent))
                        Log.warn( "[BattleArena] Warning. " + alClass + ". Method "+method.getName() + 
                                " needs a player or team, but the bukkitEvent " + bukkitEvent.getCanonicalName() + 
                                " returns only an Entity. Cast to Player will be attempted at runtime" );
                }
            }

            if (getPlayerMethod == null) getPlayerMethod = getLivingMethod; /// if playermethod is null maybe we have a living
            if (getPlayerMethod == null) getPlayerMethod = getEntityMethod;/// ok.. maybe at least an entity?

            List<ArenaEventMethod> mths = baEvent ? matchTypeMap.get(bukkitEvent) 
                                                  : bukkitTypeMap.get(bukkitEvent);
            if (mths == null) {
                mths = new ArrayList<>();
                if ( baEvent )
                    matchTypeMap.put((Class<? extends BAEvent>) bukkitEvent, mths);
                else 
                    bukkitTypeMap.put( bukkitEvent, mths );
            }

            if ( getPlayerMethod != null ) {
                if (beginState == MatchState.NONE) beginState = MatchState.ONENTER;
                if (endState == MatchState.NONE) endState = MatchState.ONLEAVE;
                if (cancelState == MatchState.NONE) cancelState = MatchState.ONCANCEL;
                mths.add( new ArenaEventMethod( method, bukkitEvent, getPlayerMethod,
                        beginState, endState, cancelState, priority, bukkitPriority, baEvent ));
            } 
            else {
                if (beginState == MatchState.NONE) beginState = MatchState.ONCREATE;
                if (endState == MatchState.NONE) endState = MatchState.ONFINISH;
                if (cancelState == MatchState.NONE) cancelState = MatchState.ONCANCEL;
                mths.add( new ArenaEventMethod( method, bukkitEvent, beginState,
                        endState, cancelState, priority, bukkitPriority, baEvent ) );
            }
            Collections.sort( mths, (o1, o2) -> { return o1.getPriority().compareTo( o2.getPriority() ); });
        }
        bukkitEventMethods.put(alClass, bukkitTypeMap);
        matchEventMethods.put(alClass, matchTypeMap);
    }

    private boolean removeListener(ArenaListener listener, HashMap<?,List<RListener>> methods) {
        
        synchronized( methods ) {
            
            for ( List<RListener> rls : methods.values() ) {
                Iterator<RListener> iter = rls.iterator();
                
                while( iter.hasNext() ) {
                    RListener rl = iter.next();
                    if ( rl.getListener() == listener ) {
                        iter.remove();
                        
                        for ( HashMap<Type, BukkitEventHandler> ls : bukkitListeners.values() ) {
                            BukkitEventHandler bel = ls.get( rl.getMethod().getEvent() );
                            if ( bel != null ) {
                                bel.removeAllListener( rl );
                            }
                        }
                    }
                }
            }
            return true;
        }
    }

    public boolean removeListener(ArenaListener listener) {
        listeners.remove(listener);
        removeListener(listener, bukkitMethods);
        removeListener(listener, matchMethods);
        return true;
    }

    public void addListener(ArenaListener listener) {
        listeners.add(listener);
        addAllEvents(listener);
    }

    /**
     * Add all of the bukkit events found in the listener
     * @param listener ArenaListener
     */
    public void addAllEvents(ArenaListener listener) {
        if (!bukkitEventMethods.containsKey(listener.getClass()) && !matchEventMethods.containsKey(listener.getClass())){
            addMethods(listener.getClass());
        }
        Map<Class<? extends Event>,List<ArenaEventMethod>> map = getBukkitMethods(listener);
        if (map != null){
            for (Class<? extends Event> clazz : map.keySet()){
                addEventMethod(listener, map, clazz);
            }
        }
        Map<Class<? extends BAEvent>,List<ArenaEventMethod>> map2 = getMatchMethods(listener);
        if (map2 != null){
            for (Class<? extends BAEvent> clazz : map2.keySet()){
                addBAEventMethod(listener, map2, clazz);
            }
        }
    }


    /**
     * Add a subset of the events found in the listener to the MethodController
     * @param listener ArenaListener
     * @param events Events to add
     */
    @SuppressWarnings("unchecked")
    public void addSpecificEvents(ArenaListener listener, List<Class<? extends Event>> events) {
        
        if (    !bukkitEventMethods.containsKey( listener.getClass() ) 
                && !matchEventMethods.containsKey( listener.getClass() ) ) {
            addMethods(listener.getClass());
        }
        
        for ( Class<? extends Event> clazz : events ) {
            
            if ( BAEvent.class.isAssignableFrom( clazz ) )
                addBAEventMethod(listener, getMatchMethods(listener), (Class<? extends BAEvent>) clazz);
            else 
                addEventMethod(listener, getBukkitMethods(listener), clazz);
        }
    }


    private void addEventMethod(ArenaListener listener, Map<Class<? extends Event>, List<ArenaEventMethod>> map,
                                Class<? extends Event> clazz) {
        
        List<ArenaEventMethod> list = map.get(clazz);
        
        if (list == null || list.isEmpty()) return;

        List<RListener> rls = bukkitMethods.get(clazz);
        if ( rls == null ) {
            rls = new ArrayList<>();
            bukkitMethods.put(clazz, rls);
        }
        for ( ArenaEventMethod mem : list ) {
            rls.add(new RListener( listener, mem ) );
        }
        Collections.sort(rls, new RListenerPriorityComparator());
    }

    private void addBAEventMethod(ArenaListener listener, Map<Class<? extends BAEvent>, List<ArenaEventMethod>> map,
                                  Class<? extends BAEvent> clazz) {
        List<ArenaEventMethod> list = map.get(clazz);
        if (list == null || list.isEmpty())
            return;
        List<RListener> rls = matchMethods.get(clazz);
        if (rls == null){
            rls = new ArrayList<>();
            matchMethods.put(clazz, rls);
        }
        for (ArenaEventMethod mem: list){
            RListener rl = new RListener(listener, mem);
            rls.add(rl);
        }
        Collections.sort(rls, new RListenerPriorityComparator());
    }

    public void deconstruct() {
        if (Defaults.DEBUG_EVENTS) controllers.remove(this);
        controllerCount--;
    }

    public static boolean showAllListeners(String player) {
        return showAllListeners(Bukkit.getConsoleSender(),player);
    }

    public static boolean showAllListeners(CommandSender sender, String limitToPlayer) {
        MessageUtil.sendMessage(
                sender, "&2# &e-!! controller=&5" + controllers.size() + "&e : &5" + controllerCount + "&e !!- &2#");

        EnumMap<EventPriority, HashMap<Type, BukkitEventHandler>> gels = getEventListeners();

        for (EventPriority bp: gels.keySet()){
            MessageUtil.sendMessage(sender, "&4#### &e----!! Bukkit Priority=&5" + bp + "&e !!---- &4####");
            HashMap<Type, BukkitEventHandler> types = gels.get(bp);
            
            for (BukkitEventHandler bel: types.values()){
                
                if (bel.getSpecificPlayerListener() != null){
                    
                    MapOfTreeSet<UUID,RListener> lists2 = bel.getSpecificPlayerListener().getListeners();
                    
                    String str = StringUtils.join(bel.getSpecificPlayerListener().getPlayers(), ", ");
                    String has = bel.hasListeners() ? "&2true" : "&cfalse";
                    if (!lists2.isEmpty())
                        MessageUtil.sendMessage(sender, "---- Event &e" +
                                bel.getSpecificPlayerListener().getEvent().getSimpleName() + "&e:" + has + "&e, players=" + str);
                    for (UUID id : lists2.keySet()) {
                        
                        Player p = ServerUtil.findPlayer(id);
                        
                        if (limitToPlayer != null && p != null && !p.getName().equalsIgnoreCase(limitToPlayer) )
                            continue;
                        Collection<RListener> rls = lists2.get(id);
                        for (RListener rl : rls) {
                            MessageUtil.sendMessage(sender, "!! " + rl.getPriority() + "  " + p +
                                    "  Listener  " + rl.getListener().getClass().getSimpleName() +
                                    " hash=" + Util.toString(rl.getListener()));
                        }
                    }
                }
                if (bel.getSpecificArenaPlayerListener() != null){
                    
                    MapOfTreeSet<UUID,RListener> lists2 = bel.getSpecificArenaPlayerListener().getListeners();
                    
//                    String str = StringUtils.join(PlayerController.UUIDToPlayerList(bel.getSpecificArenaPlayerListener().getPlayers()), ", ");
                    String str = StringUtils.join(bel.getSpecificArenaPlayerListener().getPlayers(), ", ");

                    String has = bel.hasListeners() ? "&2true" : "&cfalse";
                    if (!lists2.isEmpty())
                        MessageUtil.sendMessage(sender, "---- ArenaPlayerEvent &e" +
                                bel.getSpecificArenaPlayerListener().getEvent().getSimpleName() + "&e:" + has + "&e, players=" + str);
                    for (UUID id : lists2.keySet()){
                        
                        Player p = ServerUtil.findPlayer(id);

                        if (limitToPlayer != null && p != null && !p.getName().equalsIgnoreCase(limitToPlayer) )
                            continue;
                        Collection<RListener> rls = lists2.get(id);
                        for (RListener rl : rls){
                            MessageUtil.sendMessage(sender, "!!! " + rl.getPriority() + "  " + p +
                                    "  Listener  " + rl.getListener().getClass().getSimpleName() +
                                    " hash=" + Util.toString(rl.getListener()));
                        }
                    }
                }

                if (bel.getMatchListener()!=null){
                    EnumMap<ArenaEventPriority, Map<RListener,Integer>> lists = bel.getMatchListener().getListeners();
                    for (ArenaEventPriority ep: lists.keySet()){
                        for (Entry<RListener,Integer> entry : lists.get(ep).entrySet()){
                            MessageUtil.sendMessage(sender, "! " + ep + "  -  " + entry.getKey() + "  count=" + entry.getValue());
                        }
                    }
                }
            }
        }

//		}
        return true;
    }

    interface BAEventCaller {
        void callEvent(BAEvent event);
    }


    public void callEvent(BAEvent event) {
        baexecutor.callEvent(event);
    }

    public Collection<ArenaListener> getArenaListeners() {
        return listeners;
    }
}
