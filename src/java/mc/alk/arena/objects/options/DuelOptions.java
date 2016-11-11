package mc.alk.arena.objects.options;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.bukkit.entity.Player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.Permissions;
import mc.alk.arena.controllers.BattleArenaController;
import mc.alk.arena.controllers.MoneyController;
import mc.alk.arena.controllers.PlayerController;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.MatchState;
import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.objects.exceptions.InvalidOptionException;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.MessageUtil;
import mc.alk.arena.util.ServerUtil;

public class DuelOptions {

    @AllArgsConstructor
    public static enum DuelOption {

        ARENA("<arena>", false), 
        RATED("<rated>", false), 
        UNRATED("<unrated>", false),
        MONEY("<money>", true), 
        RAKE("<rake>", true);

        final String name;
        final public boolean needsValue;

        public String getName() {
            if ( this == DuelOption.MONEY )
                return Defaults.MONEY_STR;
            return name;
        }

        public static DuelOption fromName( String str ) {
            str = str.toUpperCase();
            try {
                return DuelOption.valueOf(str);
            } catch (Exception e) { }
            
            if ( str.equals("BET") || str.equals("WAGER") || str.equals(Defaults.MONEY_STR)) {
                return DuelOption.MONEY;
            }
            throw new IllegalArgumentException();
        }

        public static String getValidList() {
            StringJoiner joiner = new StringJoiner( ", " );
            
            for ( DuelOption option : DuelOption.values() ) {               
                switch ( option ) {
                    case MONEY:
                        joiner.add( option.getName() + " <amount>" );
                        break;
                    default:
                        joiner.add( option.getName() );
                }
            }
            return joiner.toString();
        }
    }

    @Getter final List<ArenaPlayer> challengedPlayers = new ArrayList<>();
    final Map<DuelOption, Object> options = new EnumMap<>(DuelOption.class);
    static DuelOptions defaultOptions = new DuelOptions();

    public static DuelOptions parseOptions(String[] args) throws InvalidOptionException {
        return parseOptions( null, null, args );
    }

    public static DuelOptions parseOptions( MatchParams params, ArenaPlayer challenger, String[] args) 
                                                                                throws InvalidOptionException {
        DuelOptions dop = new DuelOptions();
        dop.options.putAll(defaultOptions.options);
        Map<DuelOption, Object> ops = dop.options;

        for (int i = 0; i < args.length; i++) {
            String op = args[i];
            Player p = ServerUtil.findPlayer(op);
            if (p != null) {
                if (!p.isOnline()) {
                    throw new InvalidOptionException("&cPlayer &6" + p.getDisplayName() + "&c is not online!");
                }
                if (challenger != null && p.getName().equals(challenger.getName())) {
                    throw new InvalidOptionException("&cYou can't challenge yourself!");
                }
                if (p.hasPermission(Permissions.DUEL_EXEMPT) && !Defaults.DUEL_CHALLENGE_ADMINS) {
                    throw new InvalidOptionException("&cThis player can not be challenged!");
                }
                dop.challengedPlayers.add(PlayerController.toArenaPlayer(p));
                continue;
            }
            Object obj = null;

            DuelOption to;
            String val;
            if (op.contains("=")) {
                String[] split = op.split("=");
                op = split[0];
                val = split[1];
            } else {
                val = i + 1 < args.length ? args[i + 1] : null;
            }
            to = parseOp(op, val);
            switch (to) {
                case RATED:
                    if (!Defaults.DUEL_ALLOW_RATED) {
                        throw new InvalidOptionException("&cRated formingDuels are not allowed!");
                    }
                    break;
                default:
                    break;
            }

            if (!to.needsValue) {
                ops.put(to, null);
                continue;
            }
            i++; /// another increment to get past the value
            switch (to) {
                case RAKE:
                case MONEY:
                    Double money;
                    try {
                        money = Double.valueOf(val);
                    } catch (Exception e) {
                        throw new InvalidOptionException("&cmoney needs to be a number! Example: &6money=100");
                    }
                    if (!MoneyController.hasEconomy()) {
                        if (challenger != null) {
                            MessageUtil.sendMessage(challenger, "&cignoring duel option money as there is no economy!");
                        }
                        Log.warn("[BA Error] ignoring duel option money as there is no economy!");
                        continue;
                    }
                    obj = money;
                    break;
                case ARENA:
                    Arena a = BattleArenaController.getArena(val);
                    if (a == null) {
                        throw new InvalidOptionException("&cCouldnt find the arena &6" + val);
                    }
                    if (params != null && !a.getArenaType().matches(params.getType())) {
                        throw new InvalidOptionException("&cThe arena is used for a different type!");
                    }
                    obj = a;
                default:
                    break;
            }
            ops.put(to, obj);
        }
        if (challenger != null && dop.challengedPlayers.isEmpty()) {
            throw new InvalidOptionException("&cYou need to challenge at least one player!");
        }
        return dop;
    }

    private static DuelOption parseOp(String op, String value) throws InvalidOptionException {
        DuelOption to;
        try {
            to = DuelOption.fromName(op);
            if (to.needsValue && value == null) {
                throw new InvalidOptionException("&cThe option " + to.name() + " needs a value!");
            }
        } catch (IllegalArgumentException e) {
            throw new InvalidOptionException("&cThe player or option " + op + " does not exist, \n&cvalid options=&6"
                    + DuelOption.getValidList());
        }
        return to;
    }

    public String optionsString(MatchParams mp) {
        StringBuilder sb = new StringBuilder();
        sb.append("rated=").append(mp.isRated()).append(" ");
        for (DuelOption op : options.keySet()) {
            sb.append(op.getName());
            if (op.needsValue) {
                sb.append("=").append(options.get(op));
            }
            sb.append(" ");
        }
        return sb.toString();
    }

    public String getChallengedTeamString() {
        return MessageUtil.joinPlayers( challengedPlayers, ", " );
    }

    public String getOtherChallengedString(ArenaPlayer ap) {
        List<ArenaPlayer> players = new ArrayList<>(challengedPlayers);
        players.remove(ap);
        return MessageUtil.joinPlayers(players, ", ");
    }

    public boolean hasOption(DuelOption option) {
        return options.containsKey(option);
    }

    public Object getOptionValue(DuelOption option) {
        return options.get(option);
    }
    
    public void setOptionValue(DuelOption option, Object value) {
        options.put(option, value);
    }

    public static void setDefaults(DuelOptions dop) {
        DuelOptions.defaultOptions = dop;
    }

    public boolean matches(ArenaPlayer player, MatchParams mp) {
        if (mp.getArenaStateGraph().hasOptionAt(MatchState.PREREQS, TransitionOption.WITHINDISTANCE)) {
            Double distance = mp.getStateGraph().getOptions(MatchState.PREREQS).getWithinDistance();
            if (options.containsKey(DuelOption.ARENA)) {
                Arena arena = (Arena) options.get(DuelOption.ARENA);
                return arena.withinDistance(player.getLocation(), distance);
            }
            for (Arena arena : BattleArena.getBAController().getArenas(mp)) {
                if (arena.withinDistance(player.getLocation(), distance)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }
}
