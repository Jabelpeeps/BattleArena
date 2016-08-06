package mc.alk.arena.objects;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import mc.alk.arena.objects.events.ArenaEventPriority;


@Retention(RetentionPolicy.RUNTIME)
public @interface MatchTransitionHandler {
	ArenaEventPriority priority() default ArenaEventPriority.NORMAL;
}