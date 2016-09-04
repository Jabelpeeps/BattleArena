package mc.alk.arena.util;

import java.util.List;

import org.apache.commons.lang.mutable.MutableBoolean;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import mc.alk.arena.util.AutoClearingTimer.LongObject;
import mc.alk.arena.util.Cache.CacheObject;


public class AutoClearingTimer<Key> extends Cache<Key, LongObject<Key>>{
 
	public AutoClearingTimer() {
		super( new CacheSerializer<Key,LongObject<Key>>() {
			@Override
			public LongObject<Key> load(Key key, MutableBoolean dirty, Object... varArgs) { return null; }
			@Override
			public void save(List<LongObject<Key>> types) { }
		});
	}

	@RequiredArgsConstructor @Getter
	public static class LongObject<Key> extends CacheObject<Key, Long> {
		final Key key;
		Long value = System.currentTimeMillis();
	}

	public boolean withinTime(Key key, Long timeInterval) {
		LongObject<Key> l = this.get(key);
		if (l == null)
			return false;
		Long curTime = System.currentTimeMillis();
		return (curTime - l.getValue()) < timeInterval;
	}

	public void put(Key key){
		put( new LongObject<>(key) );
	}
}
