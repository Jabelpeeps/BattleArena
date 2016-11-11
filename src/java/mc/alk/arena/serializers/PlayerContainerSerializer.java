package mc.alk.arena.serializers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import mc.alk.arena.controllers.RoomController;
import mc.alk.arena.controllers.containers.RoomContainer;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.spawns.FixedLocation;
import mc.alk.arena.objects.spawns.SpawnLocation;
import mc.alk.arena.util.SerializerUtil;


public class PlayerContainerSerializer extends BaseConfig {

	public void load( MatchParams params ) {
		ConfigurationSection cs = config.getConfigurationSection( "lobbies." + params.getType() );
		if ( cs != null ) {
			List<String> strlocs = cs.getStringList( "spawns" );
			if ( strlocs == null || strlocs.isEmpty() )
				return;

			for ( int i = 0; i < strlocs.size(); i++ ) {
				Location loc = SerializerUtil.getLocation( strlocs.get(i) );
				RoomController.addLobby( params.getType(), i, 0, new FixedLocation( loc ) );
			}
		}
	}

	@Override
	public void save() {
		for ( RoomContainer lobby : RoomController.getLobbies() ) {
			HashMap<String, Object> amap = new HashMap<>();
			List<List<SpawnLocation>> locs = lobby.getSpawns();
			
			if ( locs != null ) {
                Map<String, List<String>> strlocs = 
                        SerializerUtil.createSaveableLocations( SerializerUtil.toMap( locs ) );
                amap.put( "spawns", strlocs );
            }
			config.createSection( "lobbies" )
			      .set( lobby.getParams().getType().getName(), amap );
		}
		super.save();
	}
}
