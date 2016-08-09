package mc.alk.scoreboardapi;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.alk.arena.executors.BaseExecutor;
import mc.alk.arena.executors.MCCommand;
import mc.alk.util.MessageUtil;


public class ScoreboardAPIExecutor extends BaseExecutor {
	ScoreboardHandler sh = new ScoreboardHandler();

	@MCCommand(cmds={"createScoreboard","cs"}, op=true)
	public boolean createScoreboard(CommandSender sender, String scoreboardName) {
		if (sh.bs == null) {
            SScoreboard s = SAPIFactory.createScoreboard(ScoreboardAPI.getSelf(), scoreboardName);
            sh.addScoreboard(s);
        }
        return MessageUtil.sendMessage(sender,"Added scoreboard");
	}

	@MCCommand(cmds={"createObjective","co"}, op=true)
	public boolean createObjective(CommandSender sender, String objectiveName, String slot,
			String criteria, String displayName) {
		SObjective o = sh.bs.getObjective(objectiveName);
		if (o != null){
			return MessageUtil.sendMessage(sender,"&cobjective " + o.getID()+" already exists");
		}

		if (displayName.contains("\\\""))
            displayName = displayName.replaceAll("\\\"", "");
		SAPIDisplaySlot s = SAPIDisplaySlot.fromValue(slot);
		o = sh.bs.registerNewObjective(objectiveName,criteria,displayName, s);
		return MessageUtil.sendMessage(sender,"Added objective " + o.getDisplayName());
	}

	@MCCommand(cmds={"addEntry","ae"}, op=true)
	public boolean addEntry(CommandSender sender, String objective, String id, String display, int points) {
		SObjective o = sh.bs.getObjective(objective);
		if (o == null){
			return MessageUtil.sendMessage(sender,"&cobjective " + objective +" doesnt exist");
		}

		sh.bs.createEntry(id,display);
		o.addEntry(id,points);
		return true;
	}

	@MCCommand(cmds={"addExistingEntry"}, op=true)
	public boolean addExistingEntry(CommandSender sender, String objective, String id, int points) {
		SObjective o = sh.bs.getObjective(objective);
		if (o == null){
			return MessageUtil.sendMessage(sender,"&cobjective " + objective +" doesnt exist");
		}
		SEntry e = sh.bs.getEntry(id);
		if (e == null)
			return MessageUtil.sendMessage(sender,"&cEntry " + id +" doesnt exist");

		o.addEntry(e, points);
		return true;
	}

	@MCCommand(cmds={"addPlayerEntry","ape"}, op=true)
	public boolean addPlayerEntry(CommandSender sender, String objective, Player p, String display, int points) {
		SObjective o = sh.bs.getObjective(objective);
		if (o == null){
			return MessageUtil.sendMessage(sender,"&cobjective " + objective +" doesnt exist");
		}
		if (display.contains("\\\""))
            display = display.replaceAll("\\\"", "");

		sh.bs.createEntry(p,display);
		o.addEntry(p,points);
		return true;
	}

	@MCCommand(cmds={"addTeam","at"}, op=true)
	public boolean addTeam(CommandSender sender, String id, String display, String prefix) {
        STeam t = sh.bs.createTeamEntry(id,display);
		t.setPrefix(prefix);
		return true;
	}

	@MCCommand(cmds={"setDisplayPlayers"}, op=true)
	public boolean setDisplayPlayers(CommandSender sender, String objective, boolean b) {
		SObjective o = sh.bs.getObjective(objective);
		if (o == null){
			return MessageUtil.sendMessage(sender,"&cobjective " + objective +" doesnt exist");
		}
		o.setDisplayPlayers(b);
        return MessageUtil.sendMessage(sender,"&2objective " + objective +" setDisplayPlayers " + b);
	}

    @MCCommand(cmds={"setDisplayTeams"}, op=true)
    public boolean setDisplayTeams(CommandSender sender, String objective, boolean b) {
        SObjective o = sh.bs.getObjective(objective);
        if (o == null){
            return MessageUtil.sendMessage(sender,"&cobjective " + objective +" doesnt exist");}
        o.setDisplayTeams(b);
        return MessageUtil.sendMessage(sender,"&2objective " + objective +" setDisplayTeams " + b);
    }

    @MCCommand(cmds={"clearBoard"}, op=true)
    public boolean clearBoard(CommandSender sender) {
        sh.bs.clear();
        return MessageUtil.sendMessage(sender,"&2 " + sh.bs.getName()+" cleared");
    }

    @MCCommand(cmds={"addToTeam","att"}, op=true)
	public boolean addToTeam(CommandSender sender, String teamName, Player p) {
		STeam t = sh.bs.getTeam(teamName);
		if (t == null){
			return MessageUtil.sendMessage(sender,"&team " + teamName+" doesnt exist");}
		if (p == null){
			return MessageUtil.sendMessage(sender,"&player " + p+" doesnt exist");}
		t.addPlayer(p);
		return MessageUtil.sendMessage(sender,"&added "+p.getName() +" to " + t.getDisplayName());
	}

	@MCCommand(cmds={"removeFromTeam","rft"}, op=true)
	public boolean removeFromTeam(CommandSender sender, String teamName, Player p) {
        STeam t = sh.bs.getTeam(teamName);
		if (t == null){
			return MessageUtil.sendMessage(sender,"&team " + teamName+" doesnt exist");}
		t.removePlayer(p);
		return MessageUtil.sendMessage(sender,"&2removed "+p.getName() +" from " + t.getDisplayName());
	}


	@MCCommand(cmds={"changeEntry","ce"}, op=true)
	public boolean changeEntry(CommandSender sender, String objective, String id, int points) {
		SObjective o = sh.bs.getObjective(objective);
		o.setPoints(id, points);
		return true;
	}

	@MCCommand(cmds={"giveScoreboard","gs"}, op=true)
	public boolean giveScoreboard(CommandSender sender, Player p) {
		sh.bs.setScoreboard(p);
		return MessageUtil.sendMessage(sender, "&2Gave "+ p.getName()+" scoreboard");
	}

	@MCCommand(cmds={"removeEntry","re"}, op=true)
	public boolean removeEntry(CommandSender sender, String id) {
		sh.bs.removeEntry(sh.bs.getEntry(id));
		return true;
	}

	@MCCommand(cmds={"removeteam","rt"}, op=true)
	public boolean removeTeam(CommandSender sender, String teamid) {
        STeam t = sh.bs.getTeam(teamid);
		if (t == null){
			return MessageUtil.sendMessage(sender,"&team " + teamid+" doesnt exist");}
		sh.bs.removeEntry(t);
		return true;
	}

	@MCCommand(cmds={"setObjectiveName","son"}, op=true)
	public boolean setDisplayName(CommandSender sender, String objective, String newDisplayName) {
		SObjective o = sh.bs.getObjective(objective);
		o.setDisplayName(newDisplayName);
		return true;
	}

	@MCCommand(cmds={"setObjectiveSlot","sol"}, op=true)
	public boolean setObjectiveSlot(CommandSender sender, String objective, String slot) {
		SObjective o = sh.bs.getObjective(objective);
		o.setDisplaySlot(SAPIDisplaySlot.fromValue(slot));
		return MessageUtil.sendMessage(sender, "&6setObjectiveSlot "+ o.getDisplayName()+" " + slot);
	}

	@MCCommand(cmds={"setEntryDisplayName","sen"}, op=true)
	public boolean setEntryName(CommandSender sender, String id, String newDisplayName) {
		if (newDisplayName.contains("\\\""))
            newDisplayName = newDisplayName.replaceAll("\\\"", "");
		sh.bs.setEntryDisplayName(id, newDisplayName);
        return MessageUtil.sendMessage(sender, "&6setEntryName "+ newDisplayName+" " + sh.bs.getEntry(id).getDisplayName());
	}

    @MCCommand(cmds={"setEntryPrefix","sen"}, op=true)
    public boolean setEntryPrefix(CommandSender sender, String id, String prefix) {
        if (prefix.contains("\\\""))
            prefix = prefix.replaceAll("\\\"", "");
        sh.bs.setEntryNamePrefix(id, prefix);
        return MessageUtil.sendMessage(sender, "&6setEntryPrefix "+ prefix+" " + sh.bs.getEntry(id).getDisplayName());
    }

	@MCCommand(cmds={"setScore","ss"}, op=true)
	public boolean setScore(CommandSender sender,String objective, String id, int score) {
        SObjective o = sh.bs.getObjective(objective);
        if (o == null){
            return MessageUtil.sendMessage(sender,"&cobjective " + objective +" doesnt exist");}

        SEntry e = sh.bs.getEntry(id);
        if (e==null){
            return MessageUtil.sendMessage(sender,"&centry " + id+" doesnt exist");
        }
        o.setPoints(e, score);
        return MessageUtil.sendMessage(sender, "&2setScore "+ id+" " + score);
    }

	@MCCommand(cmds={"printScoreboard","ps"}, op=true)
	public boolean printScoreboard(CommandSender sender) {
        if (sh.bs != null){
            return MessageUtil.sendMessage(sender, "&c"+sh.bs.getPrintString());
        } else if (sender instanceof Player) {
            Player p = (Player) sender;
            p.getScoreboard();
            return MessageUtil.sendMessage(sender, "&cYou need to be in game");
        } else {
            return MessageUtil.sendMessage(sender, "&cYou need to be in game");
        }
	}

	@MCCommand(cmds={"setTeamScore","ss"}, op=true)
	public boolean setTeamScore(CommandSender sender, String objective, String teamName, int points) {
        STeam t = sh.bs.getTeam(teamName);
		if (t == null){
			return MessageUtil.sendMessage(sender,"&team " + teamName+" doesnt exist");}
		SObjective o = sh.bs.getObjective(objective);
		if (o == null){
			return MessageUtil.sendMessage(sender,"&cobjective " + objective +" doesnt exist");
		}
		o.setTeamPoints(t, points);
		return true;
	}
}
