package mc.alk.util;

import java.util.LinkedHashMap;

public class CaseInsensitiveMap<V> extends LinkedHashMap<String, V> {
	private static final long serialVersionUID = 1L;

	@Override
	public V get(Object key){
		 return super.get(key.toString().toUpperCase());
	}

	public V get(String key) {
	    return super.get( key.toUpperCase() );
	}
	
	@Override
    public V put(String key, V value) {
        return super.put(key.toUpperCase(), value);
    }

	@Override
    public boolean containsKey(Object key) {
        return super.containsKey(key.toString().toUpperCase());
    }

	public boolean containsKey(String key) {
	    return super.containsKey( key.toUpperCase() );
	}
	
	@Override
    public V remove(Object key) {
        return super.remove(key.toString().toUpperCase());
    }
	
	public V remove(String key) {
	    return super.remove( key.toUpperCase() );
	}
}
