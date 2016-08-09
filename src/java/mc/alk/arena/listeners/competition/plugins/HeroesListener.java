package mc.alk.arena.listeners.competition.plugins;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.api.events.ExperienceChangeEvent;
import com.herocraftonline.heroes.api.events.SkillUseEvent;

import mc.alk.arena.BattleArena;
import mc.alk.arena.events.players.ArenaPlayerEnterMatchEvent;
import mc.alk.arena.listeners.competition.InArenaListener;
import mc.alk.arena.plugins.HeroesUtil;
import mc.alk.util.MessageUtil;
import mc.alk.util.ServerUtil;

public enum HeroesListener implements Listener {
	INSTANCE;

	final Set<UUID> cancelExpLoss = Collections.synchronizedSet(new HashSet<UUID>());

	static HashSet<String> disabledSkills = new HashSet<>();

	public static void enable() {
		Bukkit.getPluginManager().registerEvents(INSTANCE, BattleArena.getSelf());
	}

	@EventHandler
	public void onArenaPlayerEnterEvent(ArenaPlayerEnterMatchEvent event){
		HeroesUtil.addedToTeam(event.getTeam(), event.getPlayer().getPlayer());
	}

	/**
	 * Need to be highest to override the standard renames
	 * @param event ExperienceChangeEvent
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void cancelExperienceLoss(ExperienceChangeEvent event) {
		if (event.isCancelled())
			return;
		if (cancelExpLoss.contains( event.getHero().getPlayer().getUniqueId() ) ){
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled=true)
	public void skillDisabled(SkillUseEvent event){
		if (event.getPlayer() == null){
			return;}
		if (!InArenaListener.inArena(event.getPlayer())){
			return;}
		if (event.getSkill().getName().equals("Revive")){
			Player p = event.getArgs().length > 0 ? ServerUtil.findOnlinePlayer(event.getArgs()[0]) : null;
			if (p != null && !InArenaListener.inArena(p)){
				MessageUtil.sendMessage(event.getPlayer(), "&cYou can't revive a player who is not in the arena!");
				event.setCancelled(true);
			}
		}
		if (!containsHeroesSkill(event.getSkill().getName()))
			return;
		event.setCancelled(true);
	}

	public static void setCancelExpLoss(Player player) {
		INSTANCE.cancelExpLoss.add( player.getUniqueId() );
	}

	public static void removeCancelExpLoss(Player player) {
		INSTANCE.cancelExpLoss.remove( player.getUniqueId() );
	}

	public static boolean containsHeroesSkill(String skill) {
		return disabledSkills.contains(skill.toLowerCase());
	}

	public static void addDisabledCommands(Collection<String> disabledCommands) {
		if (disabledSkills == null){
			disabledSkills = new HashSet<>();}
		for (String s: disabledCommands){
			disabledSkills.add(s.toLowerCase());}
	}

}
