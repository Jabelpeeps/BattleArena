package mc.alk.arena.objects;

import org.bukkit.plugin.Plugin;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.BattleArena;
import mc.alk.arena.controllers.BattleArenaController;
import mc.alk.arena.executors.CustomCommandExecutor;
import mc.alk.arena.objects.arenas.ArenaType;
import mc.alk.arena.serializers.ArenaSerializer;
import mc.alk.arena.serializers.ConfigSerializer;
import mc.alk.arena.serializers.MessageSerializer;
import mc.alk.util.Log;

public class RegisteredCompetition {
	@Getter final Plugin plugin;
	@Getter final String competitionName;
	@Getter @Setter ConfigSerializer configSerializer;
	@Getter @Setter ArenaSerializer arenaSerializer;
	@Getter @Setter CustomCommandExecutor customExecutor;

	public RegisteredCompetition(Plugin _plugin, String _competitionName){
		plugin = _plugin;
		competitionName = _competitionName;
	}

	public void reload(){
		reloadConfigType();
		reloadExecutors();
		reloadArenas();
		reloadMessages();
	}

	private void reloadMessages() { MessageSerializer.reloadConfig(competitionName); }

	public MessageSerializer getMessageSerializer(){ return MessageSerializer.getMessageSerializer(competitionName); }

	private void reloadExecutors(){ /* TODO allow them to switch executors restart */ }

	private void reloadArenas(){
		BattleArenaController ac = BattleArena.getBAController();
		for (ArenaType type : ArenaType.getTypes(plugin)){
			ac.removeAllArenas(type);
		}
		for (ArenaType type : ArenaType.getTypes(plugin)){
			ArenaSerializer.loadAllArenas(plugin,type);
		}
	}

	private void reloadConfigType() {
		configSerializer.reloadFile();
		try {
			configSerializer.loadMatchParams();
		} catch (Exception e) {
			Log.printStackTrace(e);
		}
		if (plugin != BattleArena.getSelf())
			plugin.reloadConfig();
	}

	public void saveParams(MatchParams params){
		configSerializer.save(params);
	}
}
