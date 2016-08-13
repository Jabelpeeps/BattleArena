package mc.alk.arena.objects.pairs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.objects.MatchParams;

@Getter @Setter @AllArgsConstructor
public class EventPair{
	MatchParams eventParams;
	String[] args;
}
