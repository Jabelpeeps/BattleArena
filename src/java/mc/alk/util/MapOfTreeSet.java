package mc.alk.util;

import java.lang.reflect.Array;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

/**
 * Concurrent Map of List
 * @author alkarin
 *
 * @param <K>
 * @param <V>
 */
public class MapOfTreeSet<K,V> extends HashMap<K,TreeSet<V>>{
    
    private static final long serialVersionUID = 1L;
    Comparator<V> comparator = null;
    final Class<V> vClass;

    public MapOfTreeSet(Class<V> valueParameterClass) {
        super();
        vClass = valueParameterClass;
    }

    public MapOfTreeSet(Class<V> valueParameterClass, Comparator<V> _comparator) {
        comparator = _comparator;
        vClass = valueParameterClass;
    }

    public boolean add(K k, V v) {
        synchronized(this){
            
            TreeSet<V> set = get(k);
            if (set == null){
                if (comparator != null)
                    set = new TreeSet<>(comparator);
                else
                    set = new TreeSet<>();
                put(k, set);
            }
            return set.add(v);
        }
    }
    public boolean removeValue(K k, V v) {
        if (!containsKey(k)) return false;
        
        Set<V> set = get(k);
        if (set.remove(v) && set.isEmpty()){
            remove(k);
            return true;
        }
        return false;
    }

    @SuppressWarnings( "unchecked" )
    public V[] getSafe(K k){
        
        TreeSet<V> set = get(k);
        synchronized (this) {
            if (set == null) return null;
            
            return  set.toArray((V[]) Array.newInstance(vClass, set.size()));
        }
    }
}
