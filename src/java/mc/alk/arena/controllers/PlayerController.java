package mc.alk.arena.controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import mc.alk.arena.Defaults;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.util.ServerUtil;

public final class PlayerController {
	private static HashMap<UUID, ArenaPlayer> players = new HashMap<>();

	/**
	 * wrap a player into an ArenaPlayer
	 * @param player Bukkit player
	 * @return ArenaPlayer
	 */
	public static ArenaPlayer toArenaPlayer( Player player ) {
		ArenaPlayer ap = players.get( player.getUniqueId() );
		
		if ( ap == null ) {
			ap = new ArenaPlayer(player);
			players.put( ap.getUniqueId(), ap );
		} 
		return ap;
	}
	
    public static ArenaPlayer toArenaPlayer( UUID id ) {
        
        Player player = Bukkit.getPlayer( id );
        
        if ( Defaults.DEBUG_VIRTUAL && player == null ) {
            player = ServerUtil.findPlayer(id);
        }
        if ( player == null ) return null;
        
        ArenaPlayer ap = players.get( id );
        
        if ( ap == null ) {
            ap = new ArenaPlayer( player );
            players.put( id, ap );
        } 
        return ap;
    }

	/**
	 * Returns the ArenaPlayer for the given player
	 * @param player Bukkit player
	 * @return player if found, null otherwise
	 */
	public static ArenaPlayer getArenaPlayer( Player player ) {
		return players.get( player.getUniqueId() );
	}

	public static boolean hasArenaPlayer( Player player ) {
		return players.containsKey( player.getUniqueId() );
	}

	public static List<ArenaPlayer> toArenaPlayerList( Collection<Player> _players ) {
		List<ArenaPlayer> aplayers = new ArrayList<>( _players.size() );
		for ( Player p : _players )
			aplayers.add( toArenaPlayer(p) );
		return aplayers;
	}

	public static Set<ArenaPlayer> toArenaPlayerSet(Collection<Player> _players){
		Set<ArenaPlayer> aplayers = new HashSet<>(_players.size());
		for (Player p: _players)
			aplayers.add(toArenaPlayer(p));
		return aplayers;
	}

	public static Set<Player> toPlayerSet(Collection<ArenaPlayer> arenaPlayers) {
		Set<Player> _players = new HashSet<>(arenaPlayers.size());
		for (ArenaPlayer ap: arenaPlayers)
			_players.add(ap.getPlayer());
		return _players;
	}

	public static List<Player> toPlayerList(Collection<ArenaPlayer> arenaPlayers) {
		List<Player> _players = new ArrayList<>(arenaPlayers.size());
		for (ArenaPlayer ap: arenaPlayers)
			_players.add(ap.getPlayer());
		return _players;
	}

    public static List<Player> UUIDToPlayerList(Collection<UUID> uuids) {
        List<Player> _players = new ArrayList<>(uuids.size());
        for (UUID id : uuids)
            _players.add(ServerUtil.findPlayer(id));
        return _players;
    }

    public static void clearArenaPlayers(){
		players.clear();
	}
}
