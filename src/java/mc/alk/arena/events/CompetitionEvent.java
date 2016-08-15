package mc.alk.arena.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mc.alk.arena.competition.Competition;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class CompetitionEvent extends BAEvent {
	protected Competition competition;
}
