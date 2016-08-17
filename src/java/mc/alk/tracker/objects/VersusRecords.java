package mc.alk.tracker.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.mutable.MutableBoolean;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.serializers.tracker.SQLInstance;
import mc.alk.arena.util.Cache;
import mc.alk.arena.util.Cache.CacheObject;
import mc.alk.arena.util.Cache.CacheSerializer;
import mc.alk.tracker.objects.VersusRecords.VersusRecord;
import mc.alk.tracker.objects.WLTRecord.WLT;

public class VersusRecords implements CacheSerializer<List<String>,VersusRecord>{
	String id;
	SQLInstance sql;

	Cache<List<String>, VersusRecord> totals = new Cache<>(this);
	@Getter @Setter HashMap<String,List<WLTRecord>> individualRecords;
	@Setter boolean saveIndividual = true;

	public VersusRecords(String myid, SQLInstance _sql){
		id = myid;
		sql = _sql;
	}

	public static class VersusRecord extends CacheObject<List<String>,VersusRecord>{
		public int wins,losses, ties;
		final public List<String> ids = new ArrayList<>(2);
		public VersusRecord(String id1, String id2){
			ids.add(id1); ids.add(id2);
		}
		
		@Override
		public List<String> getKey() { return ids; }
		static public List<String>getKey(String id, String oid) {
		    return Arrays.asList(new String[]{id,oid});
		}
		public void incWin() {    wins++;     setDirty(); }
		public void incLosses() { losses++;   setDirty(); }
		public void incTies() {   ties++;     setDirty(); }
	}

	private List<WLTRecord> getIndRecord(String opponent){
		if (individualRecords == null)
			individualRecords = new HashMap<>();
		
		List<WLTRecord> record = individualRecords.get(opponent);
		
		if (record == null) {
			record = new ArrayList<>();
			individualRecords.put(opponent, record);
		}
		return record;
	}

	public void addWin(String oid) {
		totals.get(VersusRecord.getKey(id, oid)).incWin();
		if (saveIndividual)
			getIndRecord(oid).add(new WLTRecord(WLT.WIN));
	}
	public void addLoss(String oid) {
		totals.get(VersusRecord.getKey(id, oid)).incLosses();
		if (saveIndividual)
			getIndRecord(oid).add(new WLTRecord(WLT.LOSS));
	}
	public void addTie(String oid) {
		totals.get(VersusRecord.getKey(id, oid)).incTies();
		if (saveIndividual)
			getIndRecord(oid).add(new WLTRecord(WLT.TIE));
	}
	public VersusRecord getRecordVersus(String opponentId) {
		return totals.get(new ArrayList<>(VersusRecord.getKey(id, opponentId)));
	}

	@Override
	public VersusRecord load(List<String> key, MutableBoolean dirty, Object... varArgs) {
		VersusRecord or = sql.getVersusRecord(key.get(0), key.get(1));
		if (or != null){
			or.setCache(totals);
			dirty.setValue(false);
		} else {
			or = new VersusRecord(key.get(0), key.get(1));
			dirty.setValue(true);
		}
		return or;
	}

	@Override
	public void save(List<VersusRecord> types) {
		sql.realsaveVersusRecords(types);
		sql.saveIndividualRecords(id, individualRecords);
		individualRecords = null;
	}

	public Collection<VersusRecord> getOverallRecords() {
		if (totals == null)
			return null;
		return totals.values();
	}

	public void flushOverallRecords() {
		totals.flush();
	}
}
