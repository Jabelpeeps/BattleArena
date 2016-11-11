package mc.alk.arena.controllers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import mc.alk.arena.Defaults;
import mc.alk.arena.objects.CompetitionState;
import mc.alk.arena.util.Log;

/**
 * @author alkarin
 */
public class StateController {
    final static AtomicInteger count = new AtomicInteger(0);
    final static List<Class<? extends Enum<? extends CompetitionState>>> enums = new ArrayList<>();


    public static CompetitionState[] values() {
        int size = 0;
        int i = 0;
        for ( Class<? extends Enum<? extends CompetitionState>> enumClass : enums ) {
            size += enumClass.getEnumConstants().length;
        }
        CompetitionState[] states = new CompetitionState[size];
        for (Class<? extends Enum<? extends CompetitionState>> enumClass : enums){
            for ( Enum<?> e : enumClass.getEnumConstants() ) {
                states[i++] = (CompetitionState) e;
            }
        }
        return states;
    }
   
    public static int register(Class<? extends Enum<? extends CompetitionState>> enumClass) {
        
        for ( Class<? extends Enum<? extends CompetitionState>> registeredEnum : enums ) {
            
            if ( registeredEnum.equals(enumClass) ) continue;
            
            for ( Enum<? extends CompetitionState> each : registeredEnum.getEnumConstants() ) {
                if ( each.name().equalsIgnoreCase( enumClass.getName() ) )
                    
                    throw new IllegalStateException( "You can't have multiple CompetitionStates with the same name \n" +
                            enumClass.getDeclaringClass().getSimpleName() + "." + enumClass.getName() + " and " + 
                            each.getDeclaringClass().getSimpleName() + "." + each.name() );
            }
        }
        if ( !enums.contains( enumClass ) ) {
            enums.add( enumClass );
            
            if ( Defaults.DEBUG_TRANSITIONS ) 
                Log.info( "StateController has registered:-" + enumClass.getSimpleName()  );
        }
        return count.incrementAndGet();
    }

    public static CompetitionState fromString(String arg) {
        for ( Class<? extends Enum<? extends CompetitionState>> enumClass : enums ) {
            Method m = null;
            try {
                m = enumClass.getMethod("fromString", String.class);
            } catch (Exception e) { }
            if (m == null) {
                try {
                    m = enumClass.getMethod("valueOf", String.class);
                } catch (Exception e ) { }
            }
            if ( m == null )  continue;
            
            try {
                Object o = m.invoke( null, arg );
                if ( o == null || !(o instanceof CompetitionState) ) continue;
                
                return ( CompetitionState ) o;
            } catch (Exception e) { }
        }
        return null;
    }
}
