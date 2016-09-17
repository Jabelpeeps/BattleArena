package mc.alk.arena.listeners;

import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.controllers.PlayerController;
import mc.alk.arena.controllers.TeleportController;
import mc.alk.arena.events.BAEvent;
import mc.alk.arena.events.players.ArenaPlayerTeleportEvent;
import mc.alk.arena.listeners.custom.MethodController;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.CompetitionState;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.MatchState;
import mc.alk.arena.objects.arenas.ArenaListener;
import mc.alk.arena.objects.options.StateOptions;
import mc.alk.arena.objects.options.TransitionOption;
import mc.alk.arena.objects.spawns.SpawnLocation;
import mc.alk.arena.objects.teams.ArenaTeam;

public abstract class PlayerHolder implements Listener, ArenaListener {

    protected MethodController methodController;
    @Getter @Setter protected MatchParams params;

    public enum LocationType { NONE, HOME, ARENA, WAITROOM, LOBBY, COURTYARD, SPECTATE, VISITOR, ANY }

    public abstract CompetitionState getState();
    public abstract boolean isHandled(ArenaPlayer player);
    public abstract LocationType getLocationType();
    public abstract ArenaTeam getTeam(ArenaPlayer player);
    
    public SpawnLocation getSpawn(int index, boolean random) { return null; }
    public boolean checkReady(ArenaPlayer player, ArenaTeam team, StateOptions mo ) { return false; }
    
    public void onPreJoin(ArenaPlayer player, ArenaPlayerTeleportEvent apte) { }
    public void onPostJoin(ArenaPlayer player, ArenaPlayerTeleportEvent apte) { }
    public void onPostEnter(ArenaPlayer player, ArenaPlayerTeleportEvent apte) { }
    public void onPostLeave(ArenaPlayer player, ArenaPlayerTeleportEvent apte) { }
    public void onPreLeave(ArenaPlayer player, ArenaPlayerTeleportEvent apte) { }
    public void onPreQuit(ArenaPlayer player, ArenaPlayerTeleportEvent apte) { }
    public void onPostQuit(ArenaPlayer player, ArenaPlayerTeleportEvent apte) { }
    public void onPreEnter(ArenaPlayer player, ArenaPlayerTeleportEvent apte) { }
    
    public void addArenaListener( ArenaListener arenaListener ) { methodController.addListener( arenaListener ); }
    public boolean removeArenaListener( ArenaListener al ) { return methodController.removeListener( al ); }
    protected void updateBukkitEvents( MatchState ms, ArenaPlayer ap ) { methodController.updateEvents( ms, ap ); }
    
    public void callEvent( BAEvent event ) { methodController.callEvent( event ); }
    public boolean hasOption( TransitionOption option ) { return params.hasOptionAt(getState(), option ); }
    
    public void respawnClick( PlayerInteractEvent event, Map<UUID,Integer> respawnTimer ) {
        ArenaPlayer ap = PlayerController.toArenaPlayer( event.getPlayer() );
    
        Bukkit.getScheduler().cancelTask( respawnTimer.remove( ap.getUniqueId() ) );
        SpawnLocation loc = getSpawn( getTeam( ap ).getIndex(),
                                      getParams().hasOptionAt( MatchState.ONSPAWN, TransitionOption.RANDOMRESPAWN ));
        TeleportController.teleport( ap, loc.getLocation() );
    }
}
