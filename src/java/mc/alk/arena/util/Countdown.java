package mc.alk.arena.util;

import org.bukkit.plugin.Plugin;

import lombok.Setter;
import mc.alk.arena.controllers.Scheduler;

public class Countdown implements Runnable {
    static int count = 0;
    int iD = count++;

    public static interface CountdownCallback{
        /**
         *
         * @param secondsRemaining how many seconds are still left
         * @return whether to cancel. Return true to continue, false to stop
         */
        public boolean intervalTick(int secondsRemaining);
    }

    final Long startTime, expectedEndTime;
    final long interval;
    final CountdownCallback callback;
    final Plugin plugin;
    Integer timerId;
    @Setter boolean cancelOnExpire = true;
    boolean stop = false;
    long seconds;

    public Countdown( Plugin _plugin, int _seconds, int intervalSeconds, CountdownCallback _callback ) {
        this(_plugin, (long)_seconds, (long) intervalSeconds,_callback);
    }

    public Countdown( Plugin _plugin, long _seconds, long intervalSeconds, CountdownCallback _callback){
        if (_seconds > Integer.MAX_VALUE)
            _seconds = Integer.MAX_VALUE;
        interval = intervalSeconds <= 0 ? _seconds 
                                        : intervalSeconds;
        callback = _callback;
        plugin = _plugin;
        long rem = _seconds % interval;
        /// Lets get rid of the remainder first, so that the rest of the events are a multiple of the timeInterval
        long time = (rem != 0 ? rem : interval) * 20L;
        seconds = _seconds - (rem != 0? rem : interval);
        if ( seconds < 0 ){
            seconds = 0;
            time = 0;
        }
        startTime = System.currentTimeMillis();
        expectedEndTime = startTime + _seconds * 1000;
        timerId  = Scheduler.scheduleSynchronousTask( _plugin, this, (int) time );
    }

    @Override
    public void run() {
        if (stop) return;
        
        boolean continueOn = callback.intervalTick((int)seconds);
        
        timerId = null;
        if (!continueOn) return;

        if (!stop && (seconds > 0 || !cancelOnExpire)){
            timerId  = Scheduler.scheduleSynchronousTask(plugin, this, interval * 20L);
        }
        seconds -= interval;
    }
    public void stop(){
        stop = true;
        if (timerId != null){
            Scheduler.cancelTask(timerId);
            timerId = null;
        }
    }
    @Override
    public String toString(){
        return "[Countdown id=" + iD + " " + seconds + ":" + interval + " timerid=" + timerId + "]";
    }
    public Long getTimeRemaining(){
        return expectedEndTime - System.currentTimeMillis();
    }
}

