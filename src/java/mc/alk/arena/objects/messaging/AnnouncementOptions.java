package mc.alk.arena.objects.messaging;

import java.util.EnumMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.World;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.BattleArena;
import mc.alk.arena.objects.MatchState;
import mc.alk.arena.util.Log;
import net.milkbowl.vault.chat.Chat;

public class AnnouncementOptions {

	public enum AnnouncementOption {
		ANNOUNCE, DONTANNOUNCE, SERVER, CHANNEL, WORLD;

		public static AnnouncementOption fromName(String str){
			str = str.toUpperCase();
			AnnouncementOption ao = null;
			try {
				ao = AnnouncementOption.valueOf(str);				
			} catch (Exception e){ }
			
			if ( ao != null ) return ao;
			
			if ( str.contains("HC") || str.contains("HEROCHAT") ) return AnnouncementOption.CHANNEL;
			
			return null;
		}
	}
	@Setter static AnnouncementOptions defaultOptions;
	@Setter public static ChatPlugin chatPlugin = null;
	@Setter public static Chat vaultChat = null;

    @Getter final Map<MatchState, Map<AnnouncementOption,Object>> matchOptions = new EnumMap<>(MatchState.class);
    @Getter final Map<MatchState, Map<AnnouncementOption,Object>> eventOptions = new EnumMap<>(MatchState.class);

	public void setBroadcastOption(boolean match, MatchState ms, AnnouncementOption bo, String value) {
	    
		Map<MatchState, Map<AnnouncementOption,Object>> options = match ? matchOptions : eventOptions;
		Map<AnnouncementOption,Object> ops = options.get(ms);
		
		if (ops == null) {
			ops = new EnumMap<>(AnnouncementOption.class);
			options.put(ms, ops);
		}
		switch (bo){
		case CHANNEL:
			if (chatPlugin == null){
				Log.err(BattleArena.getNameAndVersion()+"config.yml Announcement option channel=" + value +
						", will be ignored as a Chat plugin is not enabled. Defaulting to Server Announcement" );
				ops.put(AnnouncementOption.SERVER, null);
				return;
			}
			Channel channel = chatPlugin.getChannel(value);
			if (channel == null){
				Log.err(BattleArena.getNameAndVersion()+"config.yml Announcement option channel=" + value +
						", will be ignored as channel " + value + " can not be found. Defaulting to Server Announcement");
				ops.put(AnnouncementOption.SERVER, null);
				return;
			}
			break;
		case WORLD:
			if (value == null){
				Log.err(BattleArena.getNameAndVersion() + "config.yml Announcement option world needs a value. Defaulting to Server Announcement");
				ops.put(AnnouncementOption.SERVER, null);
				return;
			}
			World w = Bukkit.getWorld(value);
			if (w == null){
				Log.err(BattleArena.getNameAndVersion() + "config.yml Announcement option world=" + value +
						", will be ignored as world " + value +" can not be found. Defaulting to Server Announcement");
				ops.put(AnnouncementOption.SERVER, null);
				return;
			} 
			break;
		default:
		}
		ops.put(bo, value);
	}

	public Channel getChannel(boolean match, MatchState state) {
		Map<AnnouncementOption,Object> ops = ( match ? matchOptions 
		                                             : eventOptions ).get(state);
		
		if ( ops == null || ops.containsKey( AnnouncementOption.DONTANNOUNCE ) )
			return Channels.NullChannel;

		if ( ops.containsKey( AnnouncementOption.CHANNEL ) ) {
			String hcChannelName = (String) ops.get( AnnouncementOption.CHANNEL );
			if ( chatPlugin == null ) {
				Log.warn( BattleArena.getNameAndVersion() + " channel plugin is not enabled, ignoring config.yml announcement option channel=" + hcChannelName );
				return Channels.ServerChannel;
			}
			Channel channel = chatPlugin.getChannel(hcChannelName);
			if (channel == null){
				Log.warn(BattleArena.getNameAndVersion()+" channel not found!. ignoring config.yml announcement option channel="+hcChannelName);
				return Channels.ServerChannel;
			}
            return channel;
		}
		if (ops.containsKey(AnnouncementOption.WORLD)){
			World w = Bukkit.getWorld((String)ops.get(AnnouncementOption.WORLD));
			if (w != null)
				return new Channels.WorldChannel(w);
		}
		return Channels.ServerChannel;
	}

	public static Channel getDefaultChannel(boolean match, MatchState state) {
		return defaultOptions.getChannel(match, state);
	}

	public boolean hasOption(boolean match, MatchState state) {
		Map<MatchState, Map<AnnouncementOption,Object>> options = match ? matchOptions : eventOptions;
		return options.containsKey(state);
	}
}
