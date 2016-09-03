package mc.alk.arena.plugins;

import com.dthielke.herochat.Herochat;

import lombok.AllArgsConstructor;
import mc.alk.arena.objects.messaging.Channel;
import mc.alk.arena.objects.messaging.ChatPlugin;
import mc.alk.arena.util.MessageUtil;

public class HerochatPlugin implements ChatPlugin {

	@Override
	public Channel getChannel( String value ) {
		return new HerochatChannel( Herochat.getChannelManager().getChannel( value ) );
	}
	
	@AllArgsConstructor
	public class HerochatChannel implements Channel {
	    com.dthielke.herochat.Channel channel;

	    @Override
	    public void broadcast( String msg ) {
	        if (msg == null) return;
	        channel.announce( MessageUtil.colorChat( msg ) );
	    }
	}
}
