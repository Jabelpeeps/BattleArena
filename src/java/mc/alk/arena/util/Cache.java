package mc.alk.arena.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.mutable.MutableBoolean;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.Defaults;


/**
 * Author: alkarin
 *
 * A Cache class that allows quick access to stored objects
 * This will also store if the value is NOT found.
 * @param <Key>
 * @param <Value>
 */
public class Cache <Key, Value> {
	public static final String version = "1.2";

	/**
	 * ease of use cache to allows subclasses to define themselves dirty
	 * @param <K>
	 * @param <V>
	 */
	public static abstract class CacheObject<K,V> implements UniqueKey<K>{
		@Setter protected Cache<K, V> cache;
		
		protected void setDirty() {
		    if ( cache != null ) 
		        cache.setDirty(getKey());
		}
	}

	/**
	 * All cache classes must be able to return a Unique Key, (not necessarily just an int ala hashCode() )
	 * @param <K>
	 */
	public interface UniqueKey<K> {
		public K getKey();
	}

	/**
	 * Interface for the class that will Save and Load the objects in the cache
	 *
	 * @param <K> Key Object
	 * @param <T> Value Object
	 */
	public interface CacheSerializer<K,T>{
		public T load(K key, MutableBoolean dirty, Object... varArgs);
		public void save(List<T> types);
	}

	/**
	 * A cache element along with when it was used
	 */
	public class CacheElement{
		public UniqueKey<Key> v;
		public Long lastUsed;
		public CacheElement(UniqueKey<Key> value){
			v = value;
		}
		public void setUsed(){
			lastUsed = System.currentTimeMillis();
		}
	}
	CacheSerializer<Key,UniqueKey<Key>> serializer; /// Our serializer for the cache data
	Map<Key,CacheElement> map = new HashMap<>(); /// a mapping of the key to the cache objects
	@Getter boolean modified = false;
	Set<Key> dirty = new HashSet<>(); /// which keys have been modified
	Boolean autoFlush = false;
	Long autoFlushTime = null;
	Long lastCheckedTime = System.currentTimeMillis();

	/**
	 * Create a new cache with the object that will save/load
	 * @param _serializer
	 */
	public Cache(CacheSerializer<Key,Value> _serializer) {
		setSerializer(_serializer);
	}

	/**
	 * Specify that this cache should try to save and remove old records every specified time
	 * This is a "loose" time, and will only be checked on a get or setDirty call
	 * @param time
	 */
	public void setSaveEvery(long time){
		autoFlush = true;
		autoFlushTime = time;
	}


	/**
	 * get a key.  if varArgs is not null these values will be passed to the serializer in the case that the
	 * cache object does not exist for loading.
	 * @param key cache key
	 * @param varArgs arguments that will be passed to serializer when a key is not found
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Value get(Key key, Object... varArgs) {
		if (Defaults.DEBUG_TRACKING) Log.info( " - getting key = " + key + " contains=" + map.containsKey(key));
		CacheElement o=null;
		if (map.containsKey(key)){
			o = map.get(key);
		} 
		else {
			MutableBoolean isdirty = new MutableBoolean(false);
			UniqueKey<Key> t = serializer.load(key, isdirty, varArgs);
			if (Defaults.DEBUG_TRACKING) Log.info( "  - loaded element  = " + t + " ");
			
			o = new CacheElement(t);
			synchronized(map){
				map.put(key, o);
			}
			if (Defaults.DEBUG_TRACKING) Log.info( "  - adding key = " + key + " contains=" + map.containsKey(key) +"  dirty="+isdirty);
			if (isdirty.booleanValue()){ /// If its dirty, add to our dirty set
				synchronized(dirty){
					dirty.add(key);
				}
			}
		}
		o.setUsed();
		
		if (autoFlush && autoFlushTime != null)
			flushOld(autoFlushTime);
		
		return (Value) o.v;
	}

	public boolean contains(UniqueKey<Key> obj){
		return map.containsKey(obj.getKey());
	}

	public UniqueKey<Key> put(UniqueKey<Key> obj) {
		CacheElement o = new CacheElement(obj);
		final Key key = obj.getKey();
		synchronized(map){
			map.put(key, o);
		}
		synchronized(dirty){
			dirty.add(key);
		}
		if (Defaults.DEBUG_TRACKING) Log.info( "  - adding key = " + key + " contains=" + map.containsKey(key) +"  dirty="+dirty);
		o.setUsed();
		if (autoFlush && autoFlushTime != null){
			flushOld(autoFlushTime);}
		return obj;
	}

	/**
	 * get a cache object using the key from the given param
	 * @param signType
	 * @return
	 */
	public Value get(UniqueKey<Key> obj){
		return get(obj.getKey());
	}

	/**
	 * get a cache object using the key from the given param.
	 * @param type
	 * @param varArgs
	 * @return
	 */
	public Value get(UniqueKey<Key> type,Object... varArgs) {
		return get(type.getKey(),varArgs);
	}

	/**
	 * remove a cache object using the type given
	 * @param type
	 * @return
	 */
	public Value remove(UniqueKey<Key> type){
		return remove(type.getKey());
	}


	/**
	 * remove a cache object using the key from the given param
	 * @param key
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Value remove(Key key){
		if (Defaults.DEBUG_TRACKING) Log.info( " - remove key = " + key + " contains=" + map.containsKey(key));
		CacheElement o = map.remove(key);
		
		if (o == null){
		    dirty.remove(key);
	        return null;
		}
        if ( dirty.contains(key) ){
            List<UniqueKey<Key>> types = new ArrayList<>(1);
            types.add(o.v);
            serializer.save(types);
        }
        return (Value) o.v;
	}

	@SuppressWarnings("unchecked")
	public Collection<Value> values() {
		return (Collection<Value>) map.values();
	}

	/**
	 * Specify that a cache object is 'dirty' and needs to be saved to db
	 * @param key
	 */
	public void setDirty(Key key) {
		modified = true;
		synchronized(dirty) {
			if (Defaults.DEBUG_TRACKING) Log.info( " - setting dirty key = " + key + " v=" + map.get(key));
			dirty.add(key);			
		}
		if (autoFlush && autoFlushTime != null){
			flushOld(autoFlushTime);}

	}

	/**
	 * Explicitly save
	 * @param element
	 */
	public void save(UniqueKey<Key> element) {
		List<UniqueKey<Key>> types = new ArrayList<>(1);
		types.add(element);
		synchronized(dirty){
			dirty.remove(element.getKey());
		}
		serializer.save(types);
	}
	/**
	 * Explicitly save
	 * @param element
	 */
	public void save(Key key) {
		List<UniqueKey<Key>> types = new ArrayList<>(1);
		if (map.containsKey(key)){
			types.add(map.get(key).v);
		}
		synchronized(dirty){
			dirty.remove(key);
		}
		serializer.save(types);
	}

	/**
	 * Save all dirty cache records
	 */
	public void save() {
		List<UniqueKey<Key>> types = new ArrayList<>(dirty.size());
		synchronized(dirty){ synchronized(map){
			dirty.remove(null);
			for (Key key: dirty){
				if (Defaults.DEBUG_TRACKING) Log.info( " - saving key = " + key + " v=" + map.get(key));
				CacheElement e = map.get(key);
				if (e != null && e.v != null)
					types.add(e.v);
			}
			dirty.clear();
			modified = false;
		}}
		serializer.save(types);
	}

	/**
	 * Specify the class to be used to serialize the data
	 * @param cachable
	 */
    @SuppressWarnings("unchecked")
    public void setSerializer(CacheSerializer<Key, Value> _serializer){
		serializer = (CacheSerializer<Key, UniqueKey<Key>>) _serializer;
	}

	/**
	 * write out all dirty records.  and empty the cache
	 */
	public void flush() {
		save();
		synchronized(dirty){ synchronized(map){
			map.clear();
			dirty.clear();
			modified = false;
		}}
	}

	/**
	 * write out all records ands clear any records older than the specified time (in milliseconds)
	 */
	public void flushOld(Long time) {
		final long now = System.currentTimeMillis();
		if (now - lastCheckedTime < time)
			return;
		lastCheckedTime = now;
		save();
		List<Key> old = new ArrayList<>();
		synchronized(dirty){ synchronized(map){
			for (CacheElement e: map.values()){
				if (now - e.lastUsed > time && e.v != null){
					if (Defaults.DEBUG_TRACKING) Log.info( "  - flushing old cache element =" + e.v +"  lastUsed=" + (now - e.lastUsed));
					old.add(e.v.getKey());
				}
			}
			dirty.clear();
			modified = false;
			for (Key k: old){
				map.remove(k);
			}
		}}
	}

	/**
	 * Clear the cache and all records.
	 */
	public void clear(){
		synchronized(dirty){ synchronized(map){
			map.clear();
			dirty.clear();
			modified = false;
		}}
	}
}
