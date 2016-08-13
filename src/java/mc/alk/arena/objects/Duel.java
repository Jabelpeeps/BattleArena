package mc.alk.arena.objects;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.objects.options.DuelOptions;
import mc.alk.arena.objects.options.DuelOptions.DuelOption;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.objects.teams.TeamFactory;

public class Duel {

    @Getter final MatchParams matchParams;
    @Getter final ArenaTeam challengerTeam;
    final HashMap<ArenaPlayer, Boolean> challengedPlayers = new HashMap<>();
    @Getter final DuelOptions options;
    @Getter @Setter Double totalMoney = null;

    public Duel(MatchParams mp, ArenaTeam challenger, DuelOptions _options) {
        matchParams = mp;
        challengerTeam = challenger;
        options = _options;
        for ( ArenaPlayer ap : _options.getChallengedPlayers() ) {
            challengedPlayers.put(ap, false);
        }
    }

    public boolean isChallenged(ArenaPlayer ap) {
        return challengedPlayers.containsKey(ap);
    }

    public boolean hasChallenger(ArenaPlayer player) {
        return challengerTeam.hasMember(player);
    }

    public void accept(ArenaPlayer player) {
        challengedPlayers.put(player, true);
    }

    public boolean isReady() {
        for (Boolean r : challengedPlayers.values()) {
            if (!r) {
                return false;
            }
        }
        return true;
    }

    public ArenaTeam makeChallengedTeam() {
        return TeamFactory.createCompositeTeam(1, matchParams, challengedPlayers.keySet());
    }

    public Collection<ArenaPlayer> getChallengedPlayers() {
        return challengedPlayers.keySet();
    }

    public Object getDuelOptionValue(DuelOption option) {
        return options.getOptionValue(option);
    }

    public Set<ArenaPlayer> getAllPlayers() {
        HashSet<ArenaPlayer> players = new HashSet<>();
        players.addAll(challengedPlayers.keySet());
        players.addAll(challengerTeam.getPlayers());
        return players;
    }
}
