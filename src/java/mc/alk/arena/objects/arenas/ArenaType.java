package mc.alk.arena.objects.arenas;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.plugin.Plugin;

import lombok.Getter;
import mc.alk.arena.controllers.ParamController;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.util.CaseInsensitiveMap;
import mc.alk.arena.util.Log;

public class ArenaType implements Comparable<ArenaType> {

    private static final CaseInsensitiveMap<ArenaFactory> factories = new CaseInsensitiveMap<>();
//    private static final CaseInsensitiveMap<Class<? extends Arena>> classes = new CaseInsensitiveMap<>();
    private static final CaseInsensitiveMap<ArenaType> types = new CaseInsensitiveMap<>();
    private final Set<ArenaType> compatibleTypes = new HashSet<>();

    @Getter private Class<? extends Arena> clazz;
    @Getter final String name;
    @Getter final Plugin plugin;
    
    static int count = 0;
    final int id = count++;

    private ArenaType( String _name, Plugin _plugin ) {
        name = _name;
        plugin = _plugin;
        if ( !types.containsKey( _name ) ) {
            types.put( _name, this );
        }
    }

    @Override
    public String toString() { return name; }

    public boolean matches(ArenaType arenaType) {
        return this == arenaType || compatibleTypes.contains(arenaType);
    }

    public Collection<String> getInvalidMatchReasons(ArenaType arenaType) {
        List<String> reasons = new ArrayList<>();
        if ( this != arenaType ) {
            reasons.add( "Arena type is " + this + ". You requested " + arenaType );
        }
        return reasons;
    }

    public String toPrettyString(int min, int max) {
        if ( name.equals("ARENA") ||  name.equals("SKIRMISH") ) {
            return min + "v" + max;
        }
        return toString();
    }

    public String getCompatibleTypes() {
        if ( compatibleTypes.isEmpty() ) return name;

        StringBuilder sb = new StringBuilder(name);
        for ( ArenaType at : compatibleTypes ) {
            sb.append(", ").append(at.name);
        }
        return sb.toString();
    }

    public int ordinal() { return id; }

    private void addCompatibleType(ArenaType at) { compatibleTypes.add(at); }
    @Override
    public int compareTo(ArenaType type) { return Integer.compare( id, type.id ); }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) return true;

        if ((obj == null) || (obj.getClass() != this.getClass())) {
            return false;
        }
        return compareTo((ArenaType) obj) == 0;
    }

    @Override
    public int hashCode() { return id; }

    public static ArenaType fromString( String arenatype ) {
        if ( arenatype == null ) return null;

        return types.get(arenatype.toUpperCase());
    }

    public static String getValidList() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for ( ArenaType at : types.values() ) {
            if ( !first ) {
                sb.append(", ");
            }
            first = false;
            sb.append(at.name);
        }
        return sb.toString();
    }

    public static ArenaType register( String arenaType, Class<? extends Arena> arenaClass, Plugin plugin ) {
        String uarenaType = arenaType.toUpperCase();
//        if ( !classes.containsKey(uarenaType) || classes.get(uarenaType) == null )
//            classes.put(uarenaType, arenaClass);

        if ( !factories.containsKey( uarenaType ) || factories.get( uarenaType ) == null )
            factories.put( uarenaType, createArenaFactory( arenaClass ) );
        
        if ( !types.containsKey( uarenaType ) ) 
            return new ArenaType( arenaType, plugin );

        types.get( uarenaType ).clazz = arenaClass;           
        return types.get( uarenaType );
    }
    
    public static ArenaType register( String arenaType, ArenaFactory factory, Plugin plugin ) {
        String uarenaType = arenaType.toUpperCase();
        if ( !factories.containsKey( uarenaType ) || factories.get( uarenaType ) == null ) {
            factories.put( uarenaType, factory );
        }
        if (!types.containsKey( uarenaType ) ) {
            return new ArenaType( arenaType, plugin );
        }
        types.get( uarenaType ).clazz = factory.newArena().getClass();
        return types.get(uarenaType);
    }

//    /**
//     * Create an arena from a name and parameters This will not load persistable
//     * objects, which must be done by the caller
//     *
//     * @param arenaName name of the arena
//     * @param arenaParams parameters for the arena
//     * @return Arena
//     */
//    public static Arena createArena(String arenaName, MatchParams arenaParams) {
//        return createArena( arenaParams.getType(), arenaName, arenaParams, true );
//    }
//
    /**
     * Create an arena from a name and parameters This will not load persistable
     * objects, which must be done by the caller
     *
     * @param arenaName name of the arena
     * @param arenaParams parameters for the arena
     * @param init : whether we should call init directly after arena creation
     * @return Arena
     */
    public static Arena createArena( String arenaName, MatchParams arenaParams, boolean init ) {
        
        ArenaType arenaType = arenaParams.getType();
        Arena arena = null;
        Class<? extends Arena> arenaClass = arenaType.getClazz();
//                classes.get(arenaType.name);
        ArenaFactory factory = factories.get(arenaType.name);

        if ( arenaClass == null && factory == null ) {
            Log.err("[BA Error] arenaClass " + arenaType.name + " is not found");
            return null;
        }
        
        if ( factory != null ) {
            arena = factory.newArena();
        }
        else if ( arenaClass != null ) {
            factory = createArenaFactory( arenaClass );
            factories.put( arenaName, factory );
            arena = factory.newArena();
        }   
        
        if ( arena != null ) {
            arenaParams.setName( arenaName );
            arenaParams.setType( arenaType );
            arena.setName( arenaName );
            arenaParams.setParent( ParamController.getMatchParams( arenaParams.getType().getName() ) );
            arena.setParams( arenaParams );
            if ( init ) {
                arena.publicInit();
            }
            return arena;
        }
        return null;
    }

    public static void addCompatibleTypes(String type1, String type2) {
        ArenaType at1 = fromString(type1);
        ArenaType at2 = fromString(type2);
        if (at1 == null || at2 == null) {
            return;
        }
        at1.addCompatibleType(at2);
        at2.addCompatibleType(at1);
    }

    public static void addAliasForType( String type, String alias ) {
        type = type.toUpperCase();
        alias = alias.toUpperCase();
        if ( type.equals( alias ) ) return;
   
        ArenaType at = fromString( type );
        if ( at == null ) return;

        types.put( alias, at );
        
//        Class<? extends Arena> c = classes.get( at.getName() );
        ArenaFactory f = factories.get( at.getName() );
//        if (c != null) {
//            classes.put(alias, c);
//        } else 
            
        if (f != null) {
            factories.put(alias, f);
        }
        MatchParams mp = ParamController.getMatchParams(type);
        if ( mp == null ) return;
        
        ParamController.addAlias(alias, mp);
    }

    public static Collection<ArenaType> getTypes() { return new HashSet<>(types.values()); }

    public static Collection<ArenaType> getTypes(Plugin plugin) {
        Set<ArenaType> result = new HashSet<>();
        for (ArenaType type : types.values()) {
            if (type.getPlugin().equals(plugin)) {
                result.add(type);
            }
        }
        return result;
    }

    public static ArenaFactory getArenaFactory(ArenaType arenaType) {
        return factories.get( arenaType.getName() );
    }

    public static boolean contains(String arenaType) {
        return types.containsKey(arenaType);
    }

    public static boolean isSame(String checkType, ArenaType arenaType) {
        ArenaType at = types.get(checkType);
        return at != null && at.equals(arenaType);
    }

    public static ArenaType getType(String value) {
        return types.get(value);
    }

    private static ArenaFactory createArenaFactory( Class<? extends Arena> arenaClass ) {
        if ( arenaClass == null ) return null;
        
        return () -> {
                Class<?>[] args = {};
                try {
                    Constructor<?> constructor = arenaClass.getConstructor(args);
                    Arena arena = (Arena) constructor.newInstance((Object[]) args);                  
                    return arena;
                } 
                catch (NoSuchMethodException ex) {
                    Log.err("If you have custom constructors for your class you must also have a public default constructor");
                    Log.err("Add the following line to your Arena Class '" + arenaClass.getSimpleName() + ".java'");
                    Log.err("public " + arenaClass.getSimpleName() + "(){}");
//                    Log.err("Or you can create your own ArenaFactory to support custom constructors");
                    Log.printStackTrace(ex);
                } 
                catch ( IllegalAccessException | InstantiationException 
                        | IllegalArgumentException | InvocationTargetException ex ) {
                    Log.printStackTrace(ex);
                }
                return null;
            };
    }
}
