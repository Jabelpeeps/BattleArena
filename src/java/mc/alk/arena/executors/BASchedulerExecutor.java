package mc.alk.arena.executors;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.command.CommandSender;

import mc.alk.arena.controllers.EventScheduler;
import mc.alk.arena.controllers.ParamController;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.pairs.EventPair;
import mc.alk.arena.util.MessageUtil;

public class BASchedulerExecutor extends CustomCommandExecutor{
	EventScheduler es;
	public BASchedulerExecutor(EventScheduler es){
		this.es = es;
	}

	@MCCommand( cmds = {"add"}, admin = true )
	public void schedule(CommandSender sender, String eventType, String[] args) {
		MatchParams ep = ParamController.getMatchParamCopy(eventType);
		
		if ( ep == null ) 
			MessageUtil.sendMessage(sender, "&cEvent type " + eventType+ " not found!");
		else if ( es.scheduleEvent( ep, Arrays.copyOfRange( args, 2, args.length ) ) )
		    MessageUtil.sendMessage(sender, "&2Event scheduled!. &6/bas list&2 to see a list of scheduled events");
		else 
		    MessageUtil.sendMessage(sender, "&cEvent not scheduled!. There was some error scheduling this events");
	}

	@MCCommand( cmds = {"delete","del"}, admin = true )
	public void delete(CommandSender sender, Integer index) {
		List<EventPair> events = es.getEvents();
		
		if ( events == null || events.isEmpty() ) 
			MessageUtil.sendMessage(sender, "&cNo &4BattleArena&c events have been scheduled");
		else if (events.size() < index || index <= 0) 
			MessageUtil.sendMessage(sender, "&cIndex is out of range.  Valid Range: &61-"+events.size());
		else {
		    es.deleteEvent(index-1);		
		    MessageUtil.sendMessage(sender, "&2Event &6"+index+"&2 deleted");
		}
	}

	@MCCommand( cmds = {"list"}, admin = true )
	public void list(CommandSender sender) {
		List<EventPair> events = es.getEvents();
		
		if ( events == null || events.isEmpty() ) {
			MessageUtil.sendMessage(sender, "&cNo &4BattleArena&c events have been scheduled");
            return;
        }
		for ( int i = 0; i < events.size(); i++ ) {
			EventPair ep = events.get(i);
			String[] args = ep.getArgs();
			String strargs = args == null ? "[]" : StringUtils.join(ep.getArgs(), " ");
			MessageUtil.sendMessage(sender, "&2" + ( i + 1 ) + "&e:&6" + ep.getEventParams().getName() + "&e args: &6" + strargs);
		}
		MessageUtil.sendMessage(sender, "&6/bas delete <number>:&e to delete an event");
	}

	@MCCommand( cmds = {"start"}, admin = true )
	public void start(CommandSender sender) {
		List<EventPair> events = es.getEvents();
		
		if ( events == null || events.isEmpty() ) 
			MessageUtil.sendMessage(sender, "&cNo &4BattleArena&c events have been scheduled");
		else if (es.isRunning())
			MessageUtil.sendMessage(sender, "&cScheduled events are already running!");
		else {
            es.start();
    		MessageUtil.sendMessage(sender, "&2Scheduled events are now &astarted");
		}
	}

	@MCCommand( cmds = {"stop"}, admin = true )
	public void stop(CommandSender sender) {
		if ( !es.isRunning() ) 
			MessageUtil.sendMessage(sender, "&cScheduled events are already stopped!");
		else {
            es.stop();
    		MessageUtil.sendMessage(sender, "&2Scheduled events are now &4stopped!");
		}
	}

	@MCCommand( cmds = {"startNext"}, admin = true )
	public void startNext(CommandSender sender) {
		List<EventPair> events = es.getEvents();
		
		if ( events == null || events.isEmpty() )
			MessageUtil.sendMessage(sender, "&cNo &4BattleArena&c events have been scheduled");
		else {
    		es.startNext();
    		MessageUtil.sendMessage(sender, "&2Next Scheduled event is now starting");
		}
	}
}
