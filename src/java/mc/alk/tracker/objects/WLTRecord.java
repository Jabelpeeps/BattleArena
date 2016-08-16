package mc.alk.tracker.objects;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class WLTRecord {
    public WLT wlt;
	@Getter public final Long date;

	public WLTRecord(WLT _wlt) {
		wlt = _wlt;
		date = System.currentTimeMillis();
	}	
	public int compareTo(WLTRecord o) {
		return date.compareTo(o.date);
	}
	@Override
    public String toString(){
		return "[WLT " + wlt + " " + date + "]";
	}
	public void reverse() {
		wlt = wlt.reverse(); 
	}
	
	public enum WLT {
	    LOSS, WIN, TIE;

	    public static WLT valueOf(int value) {
	        if (value >= WLT.values().length || value < 0)
	            return null;
	        return WLT.values()[value];
	    }

	    public WLT reverse() {
	        switch(this){
	        case LOSS: return WIN;
	        case WIN : return LOSS;
	        case TIE: return TIE;
	        }
	        return null;
	    }
	}
}
