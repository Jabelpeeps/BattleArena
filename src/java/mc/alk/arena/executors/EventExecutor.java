package mc.alk.arena.executors;

import java.util.Arrays;

import org.bukkit.command.CommandSender;

import mc.alk.arena.BattleArena;
import mc.alk.arena.Permissions;
import mc.alk.arena.competition.AbstractComp;
import mc.alk.arena.controllers.BAEventController;
import mc.alk.arena.controllers.BAEventController.SizeEventPair;
import mc.alk.arena.controllers.TeamController;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.EventParams;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.StateGraph;
import mc.alk.arena.objects.exceptions.InvalidEventException;
import mc.alk.arena.objects.exceptions.InvalidOptionException;
import mc.alk.arena.objects.joining.TeamJoinObject;
import mc.alk.arena.objects.messaging.MessageHandler;
import mc.alk.arena.objects.options.EventOpenOptions;
import mc.alk.arena.objects.options.EventOpenOptions.EventOpenOption;
import mc.alk.arena.objects.options.JoinOptions;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.MessageUtil;
import mc.alk.arena.util.TimeUtil;


public class EventExecutor extends BAExecutor{
	protected final BAEventController controller;

	public EventExecutor(){
		super();
		controller = BattleArena.getBAEventController();
	}

	@MCCommand( cmds = {"options"}, admin = true, usage = "options", order = 2 )
	public void eventOptions(CommandSender sender,EventParams eventParams) {
		StateGraph tops = eventParams.getArenaStateGraph();
        MessageUtil.sendMessage(sender, tops.getOptionString());
	}

	@MCCommand( cmds = {"cancel"}, admin = true, order = 2 )
	public void eventCancel(CommandSender sender, EventParams eventParams) {
		AbstractComp event = findUnique(sender, eventParams);
		if ( event != null ) 
		    cancelEvent(sender, event);
    }

	protected AbstractComp findUnique(CommandSender sender, EventParams eventParams) {
		SizeEventPair result = controller.getUniqueEvent(eventParams);
		if (result.nEvents == 0){
		    MessageUtil.sendMessage(sender, "&cThere are no events open/running of this type");}
		else if (result.nEvents > 1){
		    MessageUtil.sendMessage(sender, "&cThere are multiple events ongoing, please specify the arena of the event. \n&6/"+
					eventParams.getCommand()+" ongoing &c for a list");}
		return result.event;
	}

	@MCCommand( cmds = {"cancel"}, admin = true, order = 4 )
	public void eventCancel(CommandSender sender, ArenaPlayer player) {
		AbstractComp event = controller.getEvent(player);
		if (event == null) 
			MessageUtil.sendMessage(sender, "&cThere was no event with " + player.getName() +" inside");
		else
		    cancelEvent(sender,event);
	}

	public void cancelEvent(CommandSender sender, AbstractComp event){
		if (!event.isRunning() && !event.isOpen()) 
			MessageUtil.sendMessage(sender,"&eA "+event.getCommand()+" is not running");
		else {
    		controller.cancelEvent(event);
    		MessageUtil.sendMessage(sender,"&eYou have canceled the &6" + event.getName());
		}
	}

	@MCCommand( cmds = {"start"}, admin = true, usage = "start", order = 2 )
	public void eventStart(CommandSender sender, final EventParams eventParams, String[] args) {
		AbstractComp event = controller.getOpenEvent(eventParams);
		if (event == null){
			MessageUtil.sendMessage(sender, "&cThere are no open events right now");
			return;
		}
		String name = event.getName();
		if (!event.isOpen()){
		    MessageUtil.sendMessage( sender, "&eYou need to open a " + name + " before starting one");
			MessageUtil.sendMessage( sender, "&eType &6/" + event.getCommand() + " open <params>&e : to open one");
            return;
		}
		if (    !( args.length > 1 && args[1].equalsIgnoreCase("force") ) 
		        && !event.hasEnoughTeams() ) {
			MessageUtil.sendMessage(sender,"&cThe "+name+" only has &6" + event.getNTeams() +" &cteams and it needs &6" +event.getParams().getMinTeams());
			MessageUtil.sendMessage(sender,"&cIf you really want to start the bukkitEvent anyways. &6/"+event.getCommand()+" start force");
            return;
		}
		try {
			controller.startEvent(event);
			MessageUtil.sendMessage(sender,"&2You have started the &6" + name);
		} 
		catch (Exception e) {
		    MessageUtil.sendMessage(sender,"&cError Starting the &6" + name);
			Log.printStackTrace(e);
			MessageUtil.sendMessage(sender,"&c" +e.getMessage());
		}
	}

	@MCCommand( cmds = {"info"}, usage = "info", order = 2 )
	public void eventInfo(CommandSender sender, EventParams eventParams){
		AbstractComp event = findUnique(sender, eventParams);
		if (event == null) return;

		if ( !event.isOpen() && !event.isRunning() ) {
			MessageUtil.sendMessage(sender,"&eThere is no open "+event.getCommand()+" right now");
            return;
        }
		String teamOrPlayers = MessageUtil.getTeamsOrPlayers(eventParams.getMaxTeamSize());
		MessageUtil.sendMessage(sender,"&eThere are currently &6" + event.getNTeams() +"&e "+teamOrPlayers);
        MessageUtil.sendMessage(sender, event.getInfo());
	}

	@MCCommand( cmds = {"check"}, usage = "check", order = 2 )
	public void eventCheck(CommandSender sender, EventParams eventParams) {
		AbstractComp event = findUnique(sender, eventParams);
		if (event == null) return;

		if (!event.isOpen()) {
			MessageUtil.sendMessage(sender,"&eThere is no open &6"+event.getCommand()+"&e right now");
			return;
		}
		String teamOrPlayers = MessageUtil.getTeamsOrPlayers(eventParams.getMaxTeamSize());
		MessageUtil.sendMessage(sender,"&eThere are currently &6" + event.getNTeams() +"&e "+teamOrPlayers+" that have joined");
	}

	@Override
	@MCCommand( cmds = {"join"} )
	public void join(ArenaPlayer player, MatchParams mp, String args[]) {
        if ( mp instanceof EventParams ) 
			eventJoin(player, (EventParams)mp, args);
	}

	@MCCommand( cmds = {"join"}, order = 2 )
	public void eventJoin(ArenaPlayer player, EventParams eventParams, String[] args) {
		eventJoin( player, eventParams, args, false);
	}

	private void eventJoin(ArenaPlayer p, EventParams eventParams, String[] args, boolean adminCommand) {
		if (!adminCommand && !Permissions.hasMatchPerm(p.getPlayer(), eventParams, "join")){
			MessageUtil.sendSystemMessage(p,"no_join_perms", eventParams.getCommand());
			return;
		}
		if (isDisabled(p.getPlayer(), eventParams)) return;
		
		AbstractComp event = controller.getOpenEvent(eventParams);
		if (event == null){
		    MessageUtil.sendSystemMessage(p, "no_event_open");
			return;
		}
		if ( !event.isOpen() ){
		    MessageUtil.sendSystemMessage(p, "you_cant_join_event_while", event.getCommand(), event.getState());
			return;
		}
		if ( !canJoin( p, true ) ) return;

		if (event.waitingToJoin(p)){
		    MessageUtil.sendSystemMessage(p, "you_will_join_when_matched");
			return;
		}
		MatchParams sq = event.getParams();
		StateGraph tops = sq.getStateGraph();
		/// Perform is ready check
		if(!tops.playerReady(p,null)){
			String notReadyMsg = tops.getRequiredString(MessageHandler.getSystemMessage("need_the_following")+"\n");
			MessageUtil.sendMessage(p,notReadyMsg);
			return;
		}
		ArenaTeam t = TeamController.getTeam(p);
		if (t==null)
			t = TeamController.createTeam(eventParams, p); 

		if ( !canJoin( t ) ) {
		    MessageUtil.sendSystemMessage(p, "teammate_cant_join");
			MessageUtil.sendMessage(p,"&6/team leave: &cto leave the team");
            return;
		}
		JoinOptions jp;
		try {
			jp = JoinOptions.parseOptions( sq, p, Arrays.copyOfRange( args, 1, args.length ) );
		} 
		catch (InvalidOptionException e) {
			MessageUtil.sendMessage( p, e.getMessage() );
            return;
		} 
		if (sq.getMaxTeamSize() < t.size()){
		    MessageUtil.sendSystemMessage(p, "event_invalid_team_size", sq.getMaxTeamSize(), t.size());
			return;
		}
		if (!checkAndRemoveFee(sq, t)) return;
		
		TeamJoinObject tqo = new TeamJoinObject(t,sq,jp);

		event.joining(tqo);

		if (((EventParams) sq).getSecondsTillStart() != null){
			Long time = event.getTimeTillStart();
			if (time != null)
			    MessageUtil.sendSystemMessage( p, "event_will_start_in", TimeUtil.convertMillisToString( time ) );
		}
	}

	@MCCommand( cmds = {"teams"}, usage = "teams", admin = true, order = 2 )
	public void eventTeams(CommandSender sender, EventParams eventParams) {
		AbstractComp event = findUnique(sender, eventParams);
		if ( event != null ) {
		    eventTeams(sender, event);
		}
    }

	private void eventTeams(CommandSender sender, AbstractComp event) {
		StringBuilder sb = new StringBuilder();
		for (ArenaTeam t: event.getTeams()){
			sb.append("\n").append(t.getTeamInfo(null)); }

		MessageUtil.sendMessage(sender,sb.toString());
	}

	@MCCommand( cmds = {"status"}, usage = "status", order = 4 )
	public void eventStatus(CommandSender sender, EventParams eventParams) {
		AbstractComp event = findUnique(sender, eventParams);
		if (event == null) return;
		
		StringBuilder sb = new StringBuilder(event.getStatus());
		appendTeamStatus(sender, event, sb);
		MessageUtil.sendMessage(sender,sb.toString());
	}

	private void appendTeamStatus(CommandSender sender, AbstractComp event, StringBuilder sb) {
		if (Permissions.isAdmin(sender) || sender.hasPermission("arena.event.status"))
			for (ArenaTeam t: event.getTeams())
				sb.append("\n").append(t.getTeamInfo(null));
	}

	@MCCommand( cmds = {"result"}, usage = "result", order = 2 )
	public void eventResult(CommandSender sender, EventParams eventParams) {
		AbstractComp event = findUnique(sender, eventParams);
		if (event == null) return;

		StringBuilder sb = new StringBuilder(event.getResultString());
		if (sb.length() == 0)
			MessageUtil.sendMessage(sender,"&eThere are no results for a previous &6" +event.getDisplayName() +"&e right now");
		else
		    MessageUtil.sendMessage(sender,"&eResults for the &6" + event.getDisplayName() + "&e\n" + sb.toString());
	}

    protected void openEvent(AbstractComp event, EventOpenOptions eoo) throws InvalidEventException {
        if (eoo.hasOption(EventOpenOption.SILENT)) event.setSilent(true);
        controller.addOpenEvent(event);
        
        if (eoo.hasOption(EventOpenOption.AUTO)) event.autoEvent();
        else event.openEvent();
        
        if (eoo.hasOption(EventOpenOption.FORCEJOIN)) event.addAllOnline();
    }
}
