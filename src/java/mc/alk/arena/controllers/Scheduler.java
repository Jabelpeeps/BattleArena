package mc.alk.arena.controllers;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import mc.alk.arena.BattleArena;


/**
 *
 * @author alkarin
 *
 */
public class Scheduler {
	static int count = 0; /// count of current async timers

	public static int scheduleAsynchronousTask(Runnable task) {
	    return Bukkit.getScheduler().runTaskLaterAsynchronously(BattleArena.getSelf(),task,0 ).getTaskId();
	}

	public static int scheduleAsynchronousTask(Runnable task, long ticks) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(BattleArena.getSelf(),task, ticks ).getTaskId();
    }

	public static int scheduleSynchronousTask(Runnable task){
        return Bukkit.getScheduler().scheduleSyncDelayedTask(BattleArena.getSelf(), task, 0);
    }

    public static int scheduleSynchronousTask(Runnable task, long ticks) {
        return Bukkit.getScheduler().scheduleSyncDelayedTask(BattleArena.getSelf(), task, ticks);
    }

    public static int scheduleSynchronousTask(Plugin plugin, Runnable task){
        return Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, task, 0);
    }

    public static int scheduleSynchronousTask(Plugin plugin, Runnable task, long ticks){
        return Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, task, ticks);
    }

    public static void cancelTask(int taskId) {
        Bukkit.getScheduler().cancelTask(taskId);
    }

}
