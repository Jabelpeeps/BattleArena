package mc.alk.arena.executors;

import java.util.Arrays;
import java.util.HashSet;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import mc.alk.arena.BattleArena;
import mc.alk.arena.competition.AbstractComp;
import mc.alk.arena.competition.TournamentEvent;
import mc.alk.arena.controllers.BAEventController.SizeEventPair;
import mc.alk.arena.controllers.ParamController;
import mc.alk.arena.objects.ArenaSize;
import mc.alk.arena.objects.EventParams;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.objects.exceptions.InvalidEventException;
import mc.alk.arena.objects.exceptions.InvalidOptionException;
import mc.alk.arena.objects.exceptions.NeverWouldJoinException;
import mc.alk.arena.objects.options.EventOpenOptions;
import mc.alk.util.Log;
import mc.alk.util.MessageUtil;


public class TournamentExecutor extends EventExecutor implements CommandExecutor {

    public TournamentExecutor() {
        super();
    }

    @Override
    @MCCommand(cmds={"open","auto"},admin=true)
    public boolean arenaAuto(CommandSender sender, MatchParams params, String args[]) {
        return open(sender, (EventParams)params, args);
    }

    public boolean open(CommandSender sender, EventParams eventParams, String[] args) {
        try {
            openIt(sender,eventParams,args);
        } catch (InvalidEventException e) {
            MessageUtil.sendMessage(sender,e.getMessage());
        } catch (Exception e){
            MessageUtil.sendMessage(sender,e.getMessage());
            Log.printStackTrace(e);
        }
        return true;
    }

    public AbstractComp openIt(CommandSender sender, EventParams eventParams, String[] args) throws InvalidEventException{
        SizeEventPair result = controller.getUniqueEvent(eventParams);
        AbstractComp event = result.event;
        if (event != null){
            MessageUtil.sendMessage(sender,"&4There is already a tournament in progress");
            return null;
        }
        if (args.length < 2){
            MessageUtil.sendMessage(sender,"&cIncorrect command: &6/tourney <open|auto> <matchType> [options...]");
            MessageUtil.sendMessage(sender,"&cExample: &6/tourney auto arena");
            return null;
        }
        MatchParams mp = ParamController.getMatchParamCopy(args[1]);
        if (mp == null){
            MessageUtil.sendMessage(sender, "&6" + args[1] +"&c is not a valid match type!");
            MessageUtil.sendMessage(sender,"&cCommand: &6/tourney <open|auto> <matchType> [options...]");
            return null;
        }
        eventParams = ParamController.copyParams(eventParams);
        EventOpenOptions eoo;
        MatchParams sgp;
        try {
            HashSet<Integer> ignoreArgs = new HashSet<>(Arrays.asList(1)); /// ignore the matchType argument
            eoo = EventOpenOptions.parseOptions(args,ignoreArgs, mp);
            sgp = eoo.getParams(); /// single game params
            event = new TournamentEvent(eventParams,eoo);

            if (!isPowerOfTwo(sgp.getMinTeams())){
                MessageUtil.sendMessage(sender, "&cTournament nteams has to be a power of 2! like 2,4,8,16,etc. You have "+
                        sgp.getMinTeams());
                MessageUtil.sendMessage(sender, "&c/tourney auto <type> nTeams=2");
                return null;
            }
            if (!isPowerOfTwo(sgp.getMinTeamSize())){
                MessageUtil.sendMessage(sender, "&cTournament teamSize has to be a power of 2! like 1,2,4,8,16,etc. You have "+
                        sgp.getMinTeamSize());
                MessageUtil.sendMessage(sender, "&c/tourney auto <type> teamSize=1");
                return null;
            }
            if (ArenaSize.MAX == sgp.getMaxTeams() || sgp.getMinTeams() != sgp.getMaxTeams() ){
                MessageUtil.sendMessage(sender, "&cNumber of tournament teams must not be a range. Setting to &6nTeam="+sgp.getMinTeams());
                sgp.setMaxTeams(sgp.getMinTeams());
            }
            if (sgp.getMaxTeamSize() == ArenaSize.MAX || sgp.getMaxTeamSize() != sgp.getMinTeamSize()){
                MessageUtil.sendMessage(sender, "&cTournament teams must have a finite size. &eSetting to &6teamSize=" + sgp.getMinTeamSize());
                sgp.setMaxTeamSize(sgp.getMinTeamSize());
            }
            Arena arena = BattleArena.getBAController().getArenaByMatchParams(sgp);
            if (arena == null){
                MessageUtil.sendMessage(sender, "&cThere is no arena that will fit these parameters. nTeams=" +
                        sgp.getNTeams() + " teamSize=" + sgp.getTeamSize());
            }
            openEvent(event,eoo);
        } catch (InvalidOptionException e) {
            MessageUtil.sendMessage(sender, e.getMessage());
            return null;
        } catch (NeverWouldJoinException e) {
            MessageUtil.sendMessage(sender, e.getMessage());
            return null;
        } catch (Exception e){
            Log.printStackTrace(e);
            return null;
        }
        final int max = eventParams.getMaxPlayers();
        final String maxPlayers = max == ArenaSize.MAX ? "&6any&2 number of players" : max+"&2 players";
        MessageUtil.sendMessage(sender,"&2You have "+eoo.getOpenCmd()+"ed a &6" + event.getDisplayName() +
                " &2TeamSize=&6" + sgp.getTeamSize() +"&2 #Teams=&6"+
                sgp.getNTeams() +"&2 supporting "+maxPlayers);
        return event;
    }

    public static boolean isPowerOfTwo(int num)  {
        return num > 0 && (num == 1 || (num & 1) == 0 && isPowerOfTwo(num >> 1));
    }

    @MCCommand(cmds={"status"}, usage="status", order=1)
    public boolean eventStatus(CommandSender sender, EventParams eventParams, Integer round) {
        SizeEventPair result = controller.getUniqueEvent(eventParams);
        if (result.nEvents == 0){
            return MessageUtil.sendMessage(sender, "&cThere are no events open/running of this type");}
        else if (result.nEvents > 1){
            return MessageUtil.sendMessage(sender, "&cThere are multiple events ongoing, please specify the arena of the event. \n&6/"+
                    eventParams.getCommand()+" ongoing &c for a list");}
        if (!(result.event instanceof TournamentEvent)){
            return MessageUtil.sendMessage(sender, "&cThis event isn't a tournament");}

        TournamentEvent te = (TournamentEvent) result.event;
        return MessageUtil.sendMessage(sender, te.getStatus(round));
    }

}
