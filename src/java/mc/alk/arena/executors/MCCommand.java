package mc.alk.arena.executors;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface MCCommand {
	/// the cmd and all its aliases, can be blank if you want to do something when they just type the command only
    String[] cmds() default {};

	/// subCommands
    String[] subCmds() default {};

    /// Verify the number of parameters,
    int min() default 0;
    int max() default Integer.MAX_VALUE;
	int exact() default -1;

    int order() default -1;
    float helpOrder() default Integer.MAX_VALUE;
	boolean admin() default false; 
    boolean op() default false; 

    String usage() default "";
	String perm() default "";
}
