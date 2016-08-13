package mc.alk.arena.objects.pairs;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PlayerLeftPair {
	final boolean left;
	final String msg;
	
	public PlayerLeftPair(boolean result){
		this.left = result;
		this.msg = null;
	}
}
