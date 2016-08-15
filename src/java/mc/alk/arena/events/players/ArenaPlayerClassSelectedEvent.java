package mc.alk.arena.events.players;

import java.util.List;

import org.bukkit.inventory.ItemStack;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.objects.ArenaClass;
import mc.alk.arena.objects.ArenaPlayer;

public class ArenaPlayerClassSelectedEvent extends ArenaPlayerEvent{
	@Getter @Setter ArenaClass arenaClass;
	@Getter @Setter List<ItemStack> items = null;
	
	public ArenaPlayerClassSelectedEvent( ArenaPlayer _player, ArenaClass _arenaClass) {
		super( _player );
	    arenaClass = _arenaClass;
	}
}
