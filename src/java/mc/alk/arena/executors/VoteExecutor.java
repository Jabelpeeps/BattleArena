package mc.alk.arena.executors;

import mc.alk.arena.Permissions;
import mc.alk.arena.controllers.ParamController;
import mc.alk.arena.controllers.RoomController;
import mc.alk.arena.controllers.containers.LobbyContainer;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.util.MessageUtil;

public class VoteExecutor extends CustomCommandExecutor{

	@MCCommand
	public void voteForArena( ArenaPlayer ap, Arena arena ) {
	    
		LobbyContainer pc = RoomController.getLobby(arena.getArenaType());
		
		if (pc == null)
			MessageUtil.sendMessage( ap, "&cThere is no lobby for " + arena.getArenaType() );
		
		else if (!pc.isHandled(ap))
			MessageUtil.sendMessage( ap, "&cYou aren't inside the lobby for " + arena.getArenaType() );
		
		else {
    		MatchParams mp = ParamController.getMatchParamCopy( arena.getArenaType() );
    		
    		if (!Permissions.hasMatchPerm( ap.getPlayer(), mp, "add") )
    			MessageUtil.sendMessage( ap, "&cYou don't have permission to vote in a &6" + mp.getCommand() );
    		else
    		    pc.castVote( ap, mp, arena );
		}
	}
}
