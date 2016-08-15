package mc.alk.arena.events.prizes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import mc.alk.arena.competition.Competition;
import mc.alk.arena.events.BAEvent;
import mc.alk.arena.objects.teams.ArenaTeam;

/**
 * Represents a reward event, called when rewards are given out to players.
 * See also: ArenaDrawersPrizeEvent, ArenaLosersPrizeEvent, ArenaWinnersPrizeEvent
 */
@RequiredArgsConstructor
public class ArenaPrizeEvent extends BAEvent {
	final Competition competition;
	@Getter final Collection<ArenaTeam> teams;

	@Getter @Setter Integer exp;
	@Getter @Setter Double money;
	@Getter @Setter List<ItemStack> items;
	@Getter @Setter List<PotionEffect> effects;
	@Getter @Setter List<Reward> rewards;

	/**
	 * Adds a new reward for the supplied teams.
	 * @param reward The reward to give the teams involved in this event.
	 *
	 * Usage:
	 * Reward r = new Reward(){
	 *		@Override
	 *		public void reward(Team team) {
	 *			//Whatever you'd like to do with the winning team...
	 * 			//Teleport, give potion effects, kill them all.
	 *		}
	 *	};
	 */
	public void addReward(Reward reward){
		if ( rewards == null )
			rewards = new ArrayList<>();
		rewards.add(reward);
	}
}
