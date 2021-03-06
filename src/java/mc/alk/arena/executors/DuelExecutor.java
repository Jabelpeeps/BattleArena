package mc.alk.arena.executors;

import org.bukkit.command.CommandSender;

import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.util.MessageUtil;



public class DuelExecutor extends BAExecutor {
	@Override
	@MCCommand( cmds = {}, min = 1, helpOrder = 10 )
	public void duel(ArenaPlayer player, MatchParams mp, String args[]) {
		String newargs[] = new String[args.length + 1];
		for ( int i = 0; i < args.length; i++ ) {
			if ( i == 0 ) 
				newargs[i] = "duel";
			newargs[i + 1] = args[i];
		}
		super.duel(player, mp, newargs);
	}
	@Override
	public void join(ArenaPlayer player, MatchParams mp, String args[]) {
		MessageUtil.sendMessage(player, "&cYou can only duel with this type");
	}
	@Override
	public void arenaForceStart(CommandSender sender, MatchParams mp) {
		MessageUtil.sendMessage(sender, "&cThis command doesn't work for duel only arenas");
	}
}
