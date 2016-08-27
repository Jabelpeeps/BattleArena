package mc.alk.arena.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

/**
 * @author alkarin
 */
public class TimingUtil {
    @Getter static List<TimingUtil> timers = new ArrayList<>();

    @Getter Map<String,TimingStat> timings = new HashMap<>();
   
    public TimingUtil() {
        timers.add( this );
    }
    
    public class TimingStat {
        protected TimingStat() {}
        public int count = 0;
        public long totalTime = 0;
        public long getAverage() { return totalTime/count; }
    }

    public static void resetTimers() {
        for ( TimingUtil t : timers ) {
            t.timings.clear();
        }
    } 
    public void put(String key, TimingStat t) {
        timings.put(key, t);
    }
    
    public TimingStat getOrCreate( String key ) {
        TimingStat t = timings.get( key );
        if ( t == null ) {
            t = new TimingStat();
            timings.put( key, t );
        }
        return t;
    }
}
