package mc.alk.arena.controllers;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.objects.ArenaParams;
import mc.alk.arena.objects.EventParams;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.StateGraph;
import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.util.CaseInsensitiveMap;


public class ParamController {
    static final CaseInsensitiveMap<MatchParams> types = new CaseInsensitiveMap<>();
    static final Map<String, StateGraph> transitions = new ConcurrentHashMap<>();
    static final CaseInsensitiveMap<Set<String>> aliases = new CaseInsensitiveMap<>();

    public static void addMatchParams(MatchParams matchParams) {
        if (matchParams.getType() == null)
            return;
        types.put(matchParams.getType().getName(), matchParams);
        Set<String> a = aliases.get(matchParams.getType().getName());

        if (a != null) {
            for (String alias : a) {
                types.put(alias, matchParams);
            }
        }
        addAlias(matchParams.getCommand(), matchParams);
        if (Defaults.TESTSERVER)
            return;
        RoomController.updateRoomParams(matchParams);
        for (Arena arena : BattleArena.getBAController().getArenas(matchParams)) {
            arena.getParams().setParent(matchParams);
            RoomController.updateArenaParams(arena);
        }
    }

    public static void addAlias(String alias, MatchParams matchParams) {
        Set<String> set = aliases.get(matchParams.getType().getName());
        if (set == null){
            set = new HashSet<>();
            aliases.put(matchParams.getType().getName(), set);
        }
        types.put(alias, matchParams);
        set.add(alias.toUpperCase());
    }

    public static void removeMatchType(MatchParams matchParams) {
        if ( matchParams.getType() != null )
            types.remove( matchParams.getType().getName() );
        types.remove( matchParams.getCommand() );
    }

    public static Collection<MatchParams> getAllParams() {
        return types.values();
    }

    /**
     * Returns the found matchparams
     * If you want to change you should make a copy
     * @param type the ArenaType
     * @return MatchParams
     */
    public static MatchParams getMatchParams( String type ) {
        return types.get(type);
    }

    /**
     * Return a copy of the found matchparams
     * @param type ArenaType
     * @return MatchParams
     */
    public static MatchParams getMatchParamCopy( String type  ){
        MatchParams mp = types.get(type);
        if ( mp == null ) return null;
        return mp instanceof EventParams ? new EventParams(mp) : new MatchParams(mp);
    }

    public static String getAvaibleTypes(Set<String> disabled) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        HashSet<MatchParams> params = new HashSet<>();
        params.addAll(types.values());
        for (MatchParams mp: params){
            if (disabled != null && disabled.contains(mp.getName()))
                continue;
            if (!first) sb.append(", ");
            else first = false;
            sb.append(mp.getCommand());
        }
        return sb.toString();
    }

    public static EventParams getDefaultConfig() {
        return (EventParams) types.get(Defaults.DEFAULT_CONFIG_NAME);
    }

    public static ArenaParams copy(ArenaParams params) {
        if (params instanceof EventParams){
            return new EventParams((EventParams)params);
        } else if (params instanceof MatchParams){
            return new MatchParams((MatchParams)params);
        } else {
            return new ArenaParams(params);
        }
    }

    public static MatchParams copyParams(MatchParams params) {
        if (params instanceof EventParams){
            return new EventParams(params);
        }
        return new MatchParams(params);
    }

    public static EventParams copyParams(EventParams params) {
        return new EventParams(params);
    }
}
