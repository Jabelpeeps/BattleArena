package mc.alk.arena.objects.victoryconditions;

import mc.alk.arena.BattleArena;
import mc.alk.arena.competition.Match;

public abstract class VictoryCondition extends ChangeStateCondition  {
	static int count = 0;
	protected final int id = count++;

	public VictoryCondition(Match _match){
		super(_match);
		if (!VictoryType.registered(this)){
			VictoryType.register(this.getClass(), BattleArena.getSelf());
		}
	}

	@Override
	public String toString(){
		return getName();
	}

	public String getName() {
		return "[VC "+this.getClass().getSimpleName()+" : " + id+"]";
	}

}
