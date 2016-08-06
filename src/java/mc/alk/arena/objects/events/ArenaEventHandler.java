package mc.alk.arena.objects.events;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.bukkit.event.EventPriority;

import mc.alk.arena.objects.MatchState;

@Retention(RetentionPolicy.RUNTIME)
public @interface ArenaEventHandler {
	MatchState begin() default MatchState.NONE;
	MatchState end() default MatchState.NONE;
	ArenaEventPriority priority() default ArenaEventPriority.NORMAL;
	boolean needsPlayer() default true;
	String entityMethod() default "";
    boolean suppressCastWarnings() default false;
    boolean suppressWarnings() default false;
	EventPriority bukkitPriority() default EventPriority.HIGHEST;
}
