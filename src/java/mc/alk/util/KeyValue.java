package mc.alk.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class KeyValue<KEY,VALUE> {
	public KEY key;
	public VALUE value;

	public static KeyValue<String, String> split(String string, String splitOn) {
	    
		KeyValue<String,String> kv = new KeyValue<>();
		String[] split = string.split(splitOn);
		
		switch(split.length) {
    		case 2: kv.value = split[1]; 
    		// there is no break on this, it only stops after doing case 1 
    		case 1: kv.key = split[0];
    			return kv;
    		default:
    			return null;
		}
	}
}
