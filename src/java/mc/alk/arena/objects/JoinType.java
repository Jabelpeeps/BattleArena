package mc.alk.arena.objects;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum JoinType {
	QUEUE("Queue"), JOINPHASE("JoinPhase");

	final String name;
	
	@Override
	public String toString(){return name;}

	public static JoinType fromString(String str){
		str = str.toUpperCase();
		return JoinType.valueOf(str);
	}
}
