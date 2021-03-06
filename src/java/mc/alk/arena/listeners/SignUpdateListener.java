package mc.alk.arena.listeners;

import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import lombok.Getter;
import mc.alk.arena.events.matches.MatchFinishedEvent;
import mc.alk.arena.events.matches.MatchStartEvent;
import mc.alk.arena.events.players.ArenaPlayerEnterQueueEvent;
import mc.alk.arena.events.players.ArenaPlayerLeaveQueueEvent;
import mc.alk.arena.objects.ArenaSize;
import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.objects.joining.ArenaMatchQueue;
import mc.alk.arena.objects.signs.ArenaCommandSign;
import mc.alk.arena.util.MapOfTreeSet;
import mc.alk.arena.util.MessageUtil;

public class SignUpdateListener implements Listener {
    
    @Getter MapOfTreeSet<String,ArenaCommandSign> arenaSigns =
            new MapOfTreeSet<>( ArenaCommandSign.class, ( o1, o2 ) -> o1.hashCode() - o2.hashCode() );


    private String getMatchState(String str){
        if (str != null && (str.startsWith("\\d") || str.indexOf(' ') > 0 )){
            int index = str.indexOf(' ');
            return index != -1 ? str.substring(0, index) : str;
        }
        return "Open";
    }

    private String getQCount(String str){
        if (str != null && (str.startsWith("\\d") || str.indexOf(' ') > 0 )){
            int index = str.indexOf(' ');
            return index != -1 ? str.substring(index+1, str.length()) : str;
        }
        return "";
    }

    private void setPeopleInQueue(Arena arena, int playersInQueue, int neededPlayers, int maxPlayers) {
        
        ArenaCommandSign[] signLocs = arenaSigns.getSafe(arena.getName());
        if (signLocs == null || signLocs.length == 0){
            return;
        }
        final String strcount;
        if (neededPlayers == maxPlayers){
            strcount = (neededPlayers == ArenaSize.MAX) ?
                    playersInQueue +"&6/\u221E" : playersInQueue+"&6/"+neededPlayers;
        } else {
            strcount =(maxPlayers == ArenaSize.MAX) ?
                    playersInQueue +"&6/"+neededPlayers+"/\u221E" : playersInQueue+"&6/"+neededPlayers+"/"+maxPlayers;
        }
        for (ArenaCommandSign l : signLocs){
            Sign s = l.getSign();
            if (s == null)
                continue;
            s.setLine(3, MessageUtil.colorChat(getMatchState(s.getLine(3))+" " + strcount));
            s.update();
        }
    }

    private void setMatchState(Arena arena, String state) {
        ArenaCommandSign[] signLocs = arenaSigns.getSafe(arena.getName());
        if (signLocs == null || signLocs.length==0){
            return;
        }
        for (ArenaCommandSign l : signLocs){
            Sign s = l.getSign();
            if (s == null)
                continue;
            s.setLine(3, MessageUtil.colorChat(state + " "+ getQCount(s.getLine(3))));
            s.update();
        }
    }

    @EventHandler( priority = EventPriority.MONITOR )
    public void onMatchStartEvent( MatchStartEvent event ) {
        setMatchState( event.getMatch().getArena(), "Active" );
    }

    @EventHandler( priority = EventPriority.MONITOR )
    public void onMatchFinishedEvent( MatchFinishedEvent event ) {
        setMatchState( event.getMatch().getArena(), "Open" );
    }

    @EventHandler
    public void onArenaPlayerEnterQueueEvent( ArenaPlayerEnterQueueEvent event ) {
        if ( event.getArena() == null ) return;
        setPeopleInQueue( event.getArena(), 
                          event.getQueueResult().playersInQueue,
                          event.getQueueResult().params.getMinPlayers(),
                          event.getQueueResult().maxPlayers );
    }

    @EventHandler( priority = EventPriority.MONITOR )
    public void onArenaPlayerLeaveQueueEvent( ArenaPlayerLeaveQueueEvent event ) {
        if ( event.getArena() == null ) return;
        setPeopleInQueue( event.getArena(), 
                          ArenaMatchQueue.getPlayersInArenaQueue( event.getArena() ),
                          event.getParams().getMinPlayers(),
                          event.getParams().getMaxPlayers());
    }

    public void addSign( ArenaCommandSign acs ) {
        if ( acs.getSign() == null || acs.getOptions1() == null ) return;
        Arena a = acs.getArena();
        if ( a == null ) return;
        
        arenaSigns.add( a.getName(), acs );
    }
}
