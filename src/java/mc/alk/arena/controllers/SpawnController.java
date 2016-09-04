package mc.alk.arena.controllers;

import java.util.Map;
import java.util.PriorityQueue;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import lombok.AllArgsConstructor;
import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.objects.spawns.SpawnInstance;
import mc.alk.arena.objects.spawns.TimedSpawn;
import mc.alk.arena.util.CaseInsensitiveMap;
import mc.alk.arena.util.Log;

public class SpawnController {
    
    static Plugin plugin = BattleArena.getSelf();    
    static CaseInsensitiveMap<SpawnInstance> allSpawns = new CaseInsensitiveMap<>();
    PriorityQueue<NextSpawn> spawnQ;
    Map<Long, TimedSpawn> timedSpawns;
    Integer timerId;

    @AllArgsConstructor
    class NextSpawn {
        TimedSpawn is;
        Long timeToNext;
    }

    public SpawnController(Map<Long, TimedSpawn> spawnGroups) { timedSpawns = spawnGroups; }
    
    public static void registerSpawn( String s, SpawnInstance sg ) { allSpawns.put( s, sg ); }
    public static SpawnInstance getSpawnable( String name ) { return allSpawns.get( name ); }

    public void stop() {
        if (timerId != null) {
            Bukkit.getScheduler().cancelTask(timerId);
            timerId = null;
        }
        if (spawnQ != null) {
            for (NextSpawn ns : spawnQ) {
                try {
                    ns.is.despawn();
                } catch (Exception e) {
                    Log.printStackTrace(e);
                }
            }
        }
    }

    public void start() {
        if (timedSpawns != null && !timedSpawns.isEmpty()) {
            spawnQ = new PriorityQueue<>(timedSpawns.size(), (o1, o2) -> o1.timeToNext.compareTo(o2.timeToNext));
            
            for (TimedSpawn is : timedSpawns.values()) {
                long tts = is.getFirstSpawnTime();
                if (tts == 0) {
                    is.spawn();
                }
                if (is.getRespawnTime() <= 0) {
                    continue;
                }
                NextSpawn ns = new NextSpawn(is, tts);
                spawnQ.add(ns);
            }
            timerId = Scheduler.scheduleSynchronousTask( new SpawnNextEvent(0L) );
        }
    }

    @AllArgsConstructor
    public class SpawnNextEvent implements Runnable {
        Long nextTimeToSpawn;

        @Override
        public void run() {
            if (Defaults.DEBUG_SPAWNS) {
                Log.info("SpawnNextEvent::run " + nextTimeToSpawn);
            }
            
            for (NextSpawn next : spawnQ) { 
                next.timeToNext -= nextTimeToSpawn;
                if (Defaults.DEBUG_SPAWNS) {
                    Log.info("     " + next.timeToNext + "  " + next.is + "   ");
                }
            }
            NextSpawn ns;
            boolean stop = false;
            while (!spawnQ.isEmpty() && !stop) { 
                stop = spawnQ.peek().timeToNext != 0;
                if (!stop) {
                    ns = spawnQ.remove();
                    ns.is.spawn();

                    ns.timeToNext = ns.is.getRespawnTime();
                    if (ns.timeToNext <= 0)  {
                        continue;
                    }
                    spawnQ.add(ns);
                }
            }
            ns = spawnQ.peek();
            if (ns == null) return;
            
            nextTimeToSpawn = ns.timeToNext;
            if (Defaults.DEBUG_SPAWNS) {
                Log.info("run SpawnNextEvent " + spawnQ.size() + "  next=" + nextTimeToSpawn);
            }
            timerId = Scheduler.scheduleSynchronousTask( new SpawnNextEvent(nextTimeToSpawn), nextTimeToSpawn * 20 );
        }
    }
}
