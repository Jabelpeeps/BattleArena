package mc.alk.arena.objects.events;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.bukkit.event.EventPriority;

import mc.alk.arena.objects.MatchState;

@Retention(RetentionPolicy.RUNTIME)
public @interface ArenaEventHandler {
    public enum ArenaEventPriority { LOWEST,LOWER,LOW,NORMAL,HIGH,HIGHER,HIGHEST }
    
	MatchState begin() default MatchState.NONE;
	MatchState end() default MatchState.NONE;
	ArenaEventPriority priority() default ArenaEventPriority.NORMAL;
    EventPriority bukkitPriority() default EventPriority.HIGHEST;
	boolean needsPlayer() default true;
    boolean suppressCastWarnings() default false;
//  boolean suppressWarnings() default false;
//  String entityMethod() default "";
}
