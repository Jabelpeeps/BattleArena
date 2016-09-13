package mc.alk.arena.controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.competition.Match;
import mc.alk.arena.events.matches.MatchCancelledEvent;
import mc.alk.arena.events.matches.MatchCompletedEvent;
import mc.alk.arena.events.matches.MatchCreatedEvent;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.Duel;
import mc.alk.arena.objects.MatchResult;
import mc.alk.arena.objects.MatchState;
import mc.alk.arena.objects.Matchup;
import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.objects.arenas.ArenaListener;
import mc.alk.arena.objects.events.ArenaEventHandler;
import mc.alk.arena.objects.joining.MatchTeamQObject;
import mc.alk.arena.objects.options.DuelOptions.DuelOption;
import mc.alk.arena.objects.options.JoinOptions;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.util.MessageUtil;

public class DuelController implements ArenaListener {

    List<Duel> formingDuels = new CopyOnWriteArrayList<>();
    HashMap<UUID, Long> rejectTimers = new HashMap<>();
    HashMap<Matchup, Duel> ongoingDuels = new HashMap<>();
    Map<Match, Matchup> matchups = Collections.synchronizedMap(new HashMap<Match, Matchup>());

    public void addOutstandingDuel(Duel duel) {
        formingDuels.add(duel);
    }

    public Duel accept(ArenaPlayer player) {
        Duel d = getChallengedDuel(player);
        if (d != null) {
            d.accept(player);
            if (d.isReady()) {
                /// Do a final check to see if they have completed all the duel options
                if (!checkWager(d)) {
                    return null;
                }

                ArenaTeam t = d.getChallengerTeam();
                ArenaTeam t2 = d.makeChallengedTeam();
                List<ArenaTeam> teams = new ArrayList<>();
                teams.add(t);
                teams.add(t2);
                JoinOptions jo = new JoinOptions();
                jo.setMatchParams(d.getMatchParams());
                jo.setJoinLocation(player.getLocation());
                if (d.getOptions().hasOption(DuelOption.ARENA)) {
                    jo.setArena((Arena) d.getOptions().getOptionValue(DuelOption.ARENA));
                }
                Matchup m = new Matchup(d.getMatchParams(), teams, jo);
                m.addArenaListener(this);
                formingDuels.remove(d);
                ongoingDuels.put(m, d);
                MatchTeamQObject mo = new MatchTeamQObject(m);
                BattleArena.getBAController().addMatchup(mo);
            }
        }
        return d;
    }

    @ArenaEventHandler
    public void matchCancelled(MatchCancelledEvent event) {
        Match match = event.getMatch();
        Matchup matchup = matchups.remove(match);
        if (matchup == null) {
            return;
        }
        Duel d = ongoingDuels.remove(matchup);
        if (d == null) {
            return;
        }
        Double money = (Double) d.getDuelOptionValue(DuelOption.MONEY);
        if (money != null) {
            refundMoney(money, match.getTeams());
        }
    }

    private void refundMoney(Double money, Collection<ArenaTeam> teams) {
        for (ArenaTeam t : teams) {
            for (ArenaPlayer ap : t.getPlayers()) {
                MessageUtil.sendMessage(ap, "&4[Duel] &6" + money + " " + Defaults.MONEY_STR + "&e has been refunded");
                MoneyController.add( ap.getPlayer(), money );
            }
        }
    }

    @ArenaEventHandler
    public void matchComplete(MatchCompletedEvent event) {
        Match match = event.getMatch();
        Matchup matchup = matchups.remove(match);
        if (matchup == null) {
            return;
        }
        Duel d = ongoingDuels.remove(matchup);
        if (d == null) {
            return;
        }
        MatchResult mr = match.getMatchResult();

        Double money = (Double) d.getDuelOptionValue(DuelOption.MONEY);
        Double rake = (Double) d.getDuelOptionValue(DuelOption.RAKE);
        rake = (rake == null) ? 0.0 : rake;
        if (money != null) {
            if (mr.hasVictor()) {
                Collection<ArenaTeam> winningTeams = mr.getVictors();
                int winningSize = 0;
                for (ArenaTeam winTeam : winningTeams) {
                    winningSize += winTeam.size();
                }
                final double total = d.getTotalMoney();
                final double totalMinusRake = ((100.0 - rake) / 100.0) * total;
                final double split = totalMinusRake / winningSize;
                for (ArenaTeam winTeam : winningTeams) {
                    for (ArenaPlayer ap : winTeam.getPlayers()) {
                        MessageUtil.sendMessage(ap, "&4[Duel] &eYou have won &6" + split + " " + Defaults.MONEY_STR + "&e for your victory!");
                        MoneyController.add( ap.getPlayer(), split );
                    }
                }
            } else {
                refundMoney(money, mr.getDrawers());
            }
        }
    }

    @ArenaEventHandler(begin = MatchState.ONCREATE, end = MatchState.ONOPEN)
    public void onMatchCreatedEvent(MatchCreatedEvent event) {
        Matchup matchup = (((MatchTeamQObject) event.getOriginalObject().getOriginalQueuedObject()).getMatchup());
        matchups.put(event.getMatch(), matchup);
    }

    private boolean checkWager(Duel d) {
        Double wager = (Double) d.getDuelOptionValue(DuelOption.MONEY);
        if (wager == null) {
            return true;
        }
        HashSet<ArenaPlayer> players = new HashSet<>(d.getChallengedPlayers());
        players.addAll(d.getChallengerTeam().getPlayers());
        for (ArenaPlayer ap : players) {
            if ( MoneyController.hasEnough( ap.getPlayer(), wager ) ) {
                MessageUtil.sendMessage(ap, "&4[Duel] &cYou don't have enough money to accept the wager!");
                cancelFormingDuel(d, "&4[Duel]&6" + ap.getDisplayName() + " didn't have enough money for the wager");
                return false;
            }
        }
        for (ArenaPlayer ap : players) {
            MessageUtil.sendMessage(ap, "&4[Duel] &6" + wager + " " + Defaults.MONEY_STR + "&e has been subtracted from your account");
            MoneyController.subtract( ap.getPlayer(), wager );
        }
        d.setTotalMoney((wager) * players.size());
        return true;
    }

    public Duel reject(ArenaPlayer player) {
        Duel d = getChallengedDuel(player);
        if (d != null) {
            formingDuels.remove(d);
            rejectTimers.put(player.getUniqueId(), System.currentTimeMillis());
        }
        return d;
    }

    public boolean hasChallenger(ArenaPlayer player) {
        for (Duel d : formingDuels) {
            if (d.hasChallenger(player)) {
                return true;
            }
        }
        return false;
    }

    public Duel getDuel(ArenaPlayer player) {
        for (Duel d : formingDuels) {
            if (d.hasChallenger(player)) {
                return d;
            }
        }
        return getChallengedDuel(player);
    }

    public Duel getChallengedDuel(ArenaPlayer player) {
        for (Duel d : formingDuels) {
            if (d.isChallenged(player)) {
                return d;
            }
        }
        return null;
    }

    public boolean isChallenged(ArenaPlayer ap) {
        for (Duel d : formingDuels) {
            if (d.isChallenged(ap)) {
                return true;
            }
        }
        return false;
    }

    public Duel rescind(ArenaPlayer player) {
        Duel d = getDuel(player);
        if (d != null) {
            formingDuels.remove(d);
        }
        return d;
    }

    public Long getLastRejectTime(ArenaPlayer ap) {
        Long t = rejectTimers.get(ap.getUniqueId());
        if (t == null) {
            return null;
        }
        if (Defaults.DUEL_CHALLENGE_INTERVAL * 1000 < System.currentTimeMillis() - t) {
            rejectTimers.remove(ap.getUniqueId());
        }
        return t;
    }

    public void cancelFormingDuel(Duel d, String msg) {
        formingDuels.remove(d);
        Collection<ArenaPlayer> players = d.getChallengedPlayers();
        for (ArenaPlayer p : players) {
            MessageUtil.sendMessage(p, msg);
        }
        ArenaTeam t = d.getChallengerTeam();
        t.sendMessage(msg);
    }

}
