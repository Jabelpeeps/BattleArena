package mc.alk.arena.executors;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.competition.AbstractComp;
import mc.alk.arena.controllers.EventController;
import mc.alk.arena.controllers.PlayerController;
import mc.alk.arena.controllers.TeamController;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.objects.teams.FormingTeam;
import mc.alk.arena.plugins.HeroesController;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.MessageUtil;
import mc.alk.arena.util.ServerUtil;

public class TeamExecutor extends CustomCommandExecutor {
    private BAExecutor bae = BattleArena.getBAExecutor();

	@MCCommand( cmds = {"list"}, admin = true, usage = "list" )
	public void teamList(CommandSender sender) {
		StringBuilder sb = new StringBuilder();
        Collection<ArenaTeam> teams = TeamController.getSelfFormedTeams();
        
        for ( ArenaTeam t : teams ) 
			sb.append( t.getTeamInfo(null) ).append( "\n" );
        
		sb.append( "&e# of players = &6" ).append( teams.size() );
		MessageUtil.sendMessage(sender,sb.toString());
	}

	@MCCommand( cmds = {"join", "accept"}, usage = "join", perm = "arena.team.join" )
	public void teamJoin(ArenaPlayer player) {
		ArenaTeam t = TeamController.getTeam(player);
		if (t != null && t.size() >1)
			MessageUtil.sendMessage(player, "&cYou are already part of a team with &6" + t.getOtherNames(player));

		else if ( TeamController.getFormingTeam(player) == null ) {
		    MessageUtil.sendMessage(player,ChatColor.RED + "You are not part of a forming team");
			MessageUtil.sendMessage(player,ChatColor.YELLOW + "Usage: &6/team create <player2> [player3]...");
		}
		else {
    		FormingTeam ft = TeamController.getFormingTeam(player);
    
    		ft.sendJoinedPlayersMessage(ChatColor.YELLOW + player.getName() + " has joined the team");
    		ft.joinTeam(player);
    		MessageUtil.sendMessage(player,ChatColor.YELLOW + "You have joined the team with");
    		MessageUtil.sendMessage(player,ChatColor.GOLD + ft.toString());
    
    		if (ft.hasAllPlayers()){
    			ft.sendMessage("&2Your team is now complete.  you can now add an event or arena");
    			TeamController.removeFormingTeam(ft);
    			TeamController.addSelfFormedTeam(ft);
    		}
		}
	}

	@MCCommand( cmds = {"create"}, 
	            usage = "create <player 1> <player 2>...<player x>", 
	            perm = "arena.team.create" )
	public void teamCreate( ArenaPlayer player, String[] args ) {
		if ( args.length < 2 ) {
		    MessageUtil.sendMessage(player,ChatColor.YELLOW + "create <player 1> <player 2>...<player x>");
		    MessageUtil.sendMessage(player,ChatColor.YELLOW + "You need to have at least 1 person in the team");
			return;
		}
		if ( !bae.canJoin( player, true ) ) return;

		Set<String> players = new HashSet<>();
		Set<Player> foundplayers = new HashSet<>();
		Set<String> unfoundplayers = new HashSet<>();
		
        players.addAll( Arrays.asList( args ).subList( 1, args.length ) );
        
		if ( players.contains( player.getName() ) ) {
			MessageUtil.sendMessage( player, ChatColor.YELLOW + "You can not invite yourself to a team" );
			return;
		}
		ServerUtil.findOnlinePlayers( players, foundplayers, unfoundplayers );
		if ( foundplayers.size() < players.size() ) {
		    MessageUtil.sendMessage( player, ChatColor.YELLOW + "The following teammates were not found or were not online" );
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (String n : unfoundplayers){
				if (!first) sb.append(",");
				sb.append(n);
				first = false;
			}
			MessageUtil.sendMessage( player, ChatColor.YELLOW + sb.toString() );
			return;
		}
		Set<ArenaPlayer> foundArenaPlayers = PlayerController.toArenaPlayerSet( foundplayers );
		for ( ArenaPlayer p : foundArenaPlayers ) {
			if ( Defaults.DEBUG ) Log.info( "player=" + player.getName() );
			
			ArenaTeam t = TeamController.getTeam( p );
			if ( t != null || !bae.canJoin( p, false ) ) {
			    MessageUtil.sendMessage( player,"&6"+ p.getName() + "&e is already part of a team or is in an Event" );
				MessageUtil.sendMessage( player,"&eCreate team &4cancelled!" );
	            return;
			}
			if ( TeamController.getFormingTeam( p ) != null ) {
			    MessageUtil.sendMessage( player, "&6" + p.getName() + "&e is already part of a forming team" );
				MessageUtil.sendMessage( player,"&eCreate team &4cancelled!" );
	            return;
			}
		}
		foundArenaPlayers.add( player );
		if ( Defaults.DEBUG ) Log.info( player.getName() + "  players=" + foundArenaPlayers.size() );

		if ( !arenaController.hasArenaSize( foundArenaPlayers.size() ) ) {
		    MessageUtil.sendMessage( player, "&6[Warning]&eAn arena for that many players has not been created yet!" );
            return;
		}
		/// Finally ready to create a team
		FormingTeam ft = new FormingTeam( player, foundArenaPlayers );
		TeamController.addFormingTeam( ft );
		MessageUtil.sendMessage( player,
		        ChatColor.YELLOW + "You are now forming a team. The others must accept by using &6/team accept" );

		/// Send a message to the other teammates
		for ( ArenaPlayer p : ft.getPlayers() ) {
			if ( player.equals(p) ) continue;
			MessageUtil.sendMessage( p, "&eYou have been invited to a team with &6" + ft.getOtherNames( player ) );
			MessageUtil.sendMessage( p, "&6/team accept&e : to accept: &6/team decline&e to refuse " );
		}
	}

	@MCCommand( cmds = {"info"}, usage="info" )
	public void teamInfo(ArenaPlayer player) {
        ArenaTeam team = TeamController.getTeam(player);
        if (team == null)
            MessageUtil.sendMessage(player, "&eYou are not in a team");
        else
            MessageUtil.sendMessage(player, team.getTeamInfo(null));
    }

	@MCCommand( cmds = {"info"}, min = 2, admin = true, usage = "info <player>", order = 1 )
	public void teamInfoOther(CommandSender sender,ArenaPlayer player) {
        ArenaTeam team = TeamController.getTeam(player);
        if (team == null)
            MessageUtil.sendMessage(sender,"&ePlayer &6" + player.getName() +"&e is not in a team");
        else
            MessageUtil.sendMessage(sender, team.getTeamInfo(null));
	}

	@MCCommand( cmds = {"disband","leave"}, usage = "disband" )
	public void teamDisband(ArenaPlayer player) {
		/// Try to disband a forming team first
		FormingTeam ft = TeamController.getFormingTeam(player);
		if (ft != null){
			TeamController.removeFormingTeam(ft);
			ft.sendToOtherMembers(player,"&eYour team has been disbanded by " + player.getName());
			MessageUtil.sendMessage(player, "&2You have disbanded your team with " + ft.getName());
			return;
		}

		ArenaTeam t = TeamController.getTeam(player);
		if (t== null) {
			MessageUtil.sendMessage(player,"&eYou aren't part of a team");
            return;
        }

		if (HeroesController.enabled())
			HeroesController.removedFromTeam(t, player.getPlayer());

		TeamController.removeSelfFormedTeam(t);
		t.sendToOtherMembers(player,"&eYour team has been disbanded by " + player.getName());
		MessageUtil.sendMessage(player, "&2You have disbanded your team with " + t.getName());
	}

	@MCCommand( cmds = {"delete"}, usage = "delete" )
	public void teamDelete(CommandSender sender, ArenaPlayer player) {
	    
		ArenaTeam t = TeamController.getTeam(player);
		if ( t == null ) {
			MessageUtil.sendMessage( sender, ChatColor.YELLOW + player.getName() + " is not part of a team" );
            return;
		}
		AbstractComp ae = EventController.insideEvent(player);	
		if (ae != null)
			ae.leave(player);
		else
			TeamController.removeSelfFormedTeam(t);
		
		t.sendMessage(ChatColor.YELLOW + "The team has been deleted ");
	}

	@MCCommand( cmds = {"decline"}, usage = "decline" )
	public void teamDecline( ArenaPlayer p ) {
	    
		FormingTeam t = TeamController.getFormingTeam(p);
		if ( t == null )
		    MessageUtil.sendMessage( p, ChatColor.YELLOW + "You are not part of a forming team" );
		else {
    		t.sendMessage( ChatColor.YELLOW + "The team has been disbanded as " + p.getDisplayName() + " has declined" );
    		TeamController.removeFormingTeam(t);
		}
	}
}
