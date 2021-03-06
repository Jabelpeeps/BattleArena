package mc.alk.arena.controllers;

import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import lombok.AllArgsConstructor;
import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.controllers.containers.RoomContainer;
import mc.alk.arena.listeners.PlayerHolder;
import mc.alk.arena.objects.CompetitionState;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.objects.arenas.ArenaType;
import mc.alk.arena.objects.exceptions.InvalidOptionException;
import mc.alk.arena.objects.options.AlterParamOption;
import mc.alk.arena.objects.options.TransitionOption;
import mc.alk.arena.objects.regions.WorldGuardRegion;
import mc.alk.arena.objects.spawns.FixedLocation;
import mc.alk.arena.objects.spawns.SpawnIndex;
import mc.alk.arena.plugins.WorldEditUtil;
import mc.alk.arena.plugins.WorldGuardController;
import mc.alk.arena.serializers.ArenaSerializer;
import mc.alk.arena.serializers.PlayerContainerSerializer;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.MessageUtil;
import mc.alk.arena.util.TeamUtil;
import mc.alk.arena.util.Util;

public class ArenaAlterController {

    public static class ArenaOptionPair {
        public ChangeType ao;
        public Object value;
    }

    @AllArgsConstructor
    public enum ChangeType {
        WAITROOM(true,true),
        SPECTATE(true,true),
        LOBBY(true,true),
        SPAWNLOC(true,true),
        VLOC(true,true),
        TYPE(true,false),
        ADDREGION(false,true);

        final boolean needsValue; 
        final boolean needsPlayer; 

        public boolean needsPlayer() { return needsPlayer; }
        public boolean hasValue(){ return needsValue; }

        public static ChangeType fromName(String str) {
            str = str.toUpperCase();
            ChangeType ct = null;
            try {
                ct = ChangeType.valueOf(str);
            } 
            catch (Exception e) { }
            
            if (ct != null) return ct;
            if (str.equalsIgnoreCase("wr")) return WAITROOM;
            if (str.equalsIgnoreCase("s")) return SPECTATE;
            if (str.equalsIgnoreCase("l")) return LOBBY;
            if (str.equalsIgnoreCase("v") || str.equalsIgnoreCase("visitor")) return VLOC;
            if (str.equalsIgnoreCase("spawn") || str.equalsIgnoreCase("teamSpawn")) return SPAWNLOC;
            
            try {
                if (Integer.valueOf(str) != null)
                    return SPAWNLOC;
            } 
            catch (Exception e) { }
            
            if (str.equalsIgnoreCase("main")) return SPAWNLOC;
            if (TeamUtil.getFromHumanTeamIndex(str) != null) return SPAWNLOC;
            
            return null;
        }

        public static String getValidList() {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (ChangeType r: ChangeType.values()){
                if (!first) sb.append(", ");
                sb.append(r);
                first = false;
            }
            return sb.toString();
        }

        public static Object getValue(ChangeType ct, int curIndex, String[] args) {
            String value = args[curIndex];
            switch (ct) {
                case SPAWNLOC:
                    try{
                        if (value.equalsIgnoreCase("spawn")) {
                            curIndex++;
                            value = args[curIndex];
                        }
                    } catch (Exception e){
                        /* do nothing*/
                    }
                case WAITROOM:
                case SPECTATE:
                case LOBBY:
                case VLOC:
                    Integer locindex = null;
                    try {
                        locindex = Integer.parseInt(value) - 1;
                    } catch (Exception e) {/* do nothing */}
                    if (locindex == null && value.equalsIgnoreCase("main"))
                        locindex = Integer.MAX_VALUE;
                    if (locindex == null){
                        locindex = TeamUtil.getFromHumanTeamIndex(value);
                        if (locindex == null || locindex > Defaults.MAX_SPAWNS) {
                            return null;
                        }
                    }

                    if (args.length > curIndex+1){
                        Integer spawnindex = TeamUtil.getFromHumanTeamIndex(args[curIndex+1]);
                        if (spawnindex == null || spawnindex > Defaults.MAX_SPAWNS || spawnindex < 0) {
                            return new SpawnIndex(locindex);
                        }
                        return new SpawnIndex(locindex,spawnindex);
                    }
                    return new SpawnIndex(locindex);
                    
                case TYPE:
                    return ArenaType.getType(value);
                case ADDREGION:
                    return value;
            }
            return null;
        }
    }

    public static boolean setArenaOption(CommandSender sender, Arena arena, AlterParamOption go, Object value)
            throws IllegalStateException {
        MatchParams params = arena.getParams();
        ParamAlterController.setOption(sender, params, go, value);
        RoomController.updateArenaParams(arena);
        ArenaSerializer.saveArenas(params.getType().getPlugin());
        return true;
    }

    public static boolean setArenaOption(CommandSender sender, Arena arena, CompetitionState state,
                                         TransitionOption op, Object value) throws IllegalStateException, InvalidOptionException {
        MatchParams params = arena.getParams();
        ParamAlterController.setOption(sender, params, state, op, value);
        RoomController.updateArenaParams(arena);
        ArenaSerializer.saveArenas(params.getType().getPlugin());
        return true;
    }


    public static boolean setArenaOption(CommandSender sender, Arena arena, ChangeType ct, Object value)
            throws IllegalStateException {
        BattleArenaController ac = BattleArena.getBAController();
        MatchParams params = arena.getParams();

        boolean success = false;

        Player player = null;
        if (sender instanceof Player){
            player = (Player) sender;
        }

        switch(ct){
            case TYPE: success = changeType(sender, arena, ac, (String) value); break;
            case SPAWNLOC: success = changeSpawn(player, arena, ac, (SpawnIndex)value); break;
            case VLOC: success = changeVisitorSpawn(player,arena,ac,(SpawnIndex)value); break;
            case WAITROOM: success = changeWaitroomSpawn(player,arena,ac,(SpawnIndex)value); break;
            case SPECTATE: success = changeSpectateSpawn(player,arena,ac,(SpawnIndex)value); break;
            case LOBBY: success = changeLobbySpawn(player, params,(SpawnIndex)value); break;
            case ADDREGION: success = addWorldGuardRegion(player,arena); break;
            default:
                MessageUtil.sendMessage( sender, 
                        ChatColor.RED + "Option: &6" + ct + "&c does not exist. \n&cValid options are &6" + ChangeType.getValidList() );
                break;
        }
        if (success)
            ArenaSerializer.saveArenas(params.getType().getPlugin());
        return success;
    }

    private static boolean checkWorldGuard(CommandSender sender){
        if ( !WorldGuardController.hasWorldGuard() ){
            MessageUtil.sendMessage(sender,"&cWorldGuard is not enabled");
            return false;
        }
        if (!(sender instanceof Player)){
            MessageUtil.sendMessage(sender,"&cYou need to be in game to use this command");
            return false;
        }
        return true;
    }

    private static boolean addWorldGuardRegion(Player sender, Arena arena) {
        if ( !checkWorldGuard(sender) )
            return false;
        Selection sel = WorldEditUtil.getSelection(sender);
        if (sel == null){
            MessageUtil.sendMessage(sender,"&cYou need to select a region to use this command.");
            return false;
        }

        WorldGuardRegion region = arena.getWorldGuardRegion();
        World w = sel.getWorld();
        try{
            String id = makeRegionName(arena);
            if (region != null){
                WorldGuardController.updateProtectedRegion(sender,id);
                MessageUtil.sendMessage(sender,"&2Region updated! ");
            } else {
                ProtectedRegion WGregion = WorldGuardController.createRegion(sender, id);
                if (WGregion != null) {
                    MessageUtil.sendMessage(sender,"&2Region "+WGregion.getId()+" added! ");
                } else {
                    MessageUtil.sendMessage(sender,"&cRegion addition failed! ");
                    return false;
                }
            }

            ConfigurationSection cs = BattleArena.getBAConfigSerializer().getWorldGuardConfig();
            if (cs != null && cs.contains("defaultWGFlags")) {
                cs = cs.getConfigurationSection("defaultWGFlags");
                Set<String> set = cs.getKeys(false);
                if (set != null) {
                    for (String key : set) {
                        WorldGuardController.setFlag(region, key, cs.getBoolean(key, false));
                    }
                }
            }

            arena.setWorldGuardRegion(region);
            WorldGuardController.saveSchematic(sender, id);
            MatchParams mp = ParamController.getMatchParams(arena.getArenaType().getName());
            if (mp != null && mp.getArenaStateGraph().hasOption(TransitionOption.WGNOENTER)){
                WorldGuardController.trackRegion(w.getName(), id);
                WorldGuardController.setFlag(region, "entry", false);
            }
            if (mp != null && mp.getArenaStateGraph().hasOption(TransitionOption.WGNOLEAVE)){
                WorldGuardController.trackRegion(w.getName(), id);
                WorldGuardController.setFlag(region, "exit", false);
            }
        } catch (Exception e) {
            MessageUtil.sendMessage(sender,"&cAdding WorldGuard region failed!");
            MessageUtil.sendMessage(sender, "&c" + e.getMessage());
            Log.printStackTrace(e);
        }
        return true;
    }

    public static String makeRegionName(Arena arena){
        return "ba-"+arena.getName().toLowerCase();
    }

    private static boolean changeSpawn( Player sender, Arena arena, BattleArenaController ac, SpawnIndex index ) {
        Location loc = sender.getLocation();
        try {
            arena.setSpawnLoc( index.teamIndex,index.spawnIndex, new FixedLocation(loc) );
        }
        catch ( IllegalStateException e ) { 
            e.printStackTrace();
            return false; 
        }
        
        ac.updateArena(arena);
        MessageUtil.sendMessage( sender, "&2 " + getSpawnName( index.teamIndex ) + " &2team spawn #&6" + 
                ( index.spawnIndex + 1 ) + "&2 set to location=&6" + Util.getLocString(loc) );
        return true;
    }

    private static String getSpawnName(int index) {
        return index == Integer.MAX_VALUE ? "main" : TeamUtil.getTeamName(index);
    }

    private static boolean changeLobbySpawn(Player sender, MatchParams params, SpawnIndex index) {
        Location loc = sender.getLocation();
        RoomController.addLobby(params.getType(), index.teamIndex, index.spawnIndex, new FixedLocation(loc));
        PlayerContainerSerializer pcs = new PlayerContainerSerializer();
        pcs.setConfig( BattleArena.getSelf().getDataFolder().getPath() + "/watchers/containers.yml" );
        pcs.save();
        MessageUtil.sendMessage( sender, "&2Lobby for the " + getSpawnName( index.teamIndex ) + " &2team. Spawn #&6" +
                ( index.spawnIndex + 1 ) + "&2 set to location=&6" + Util.getLocString( loc ) );
        return true;
    }

    private static boolean changeWaitroomSpawn( Player sender, Arena arena, BattleArenaController ac, SpawnIndex index ) {
        Location loc = sender.getLocation();
        RoomContainer rc = RoomController.getOrCreateRoom( arena, PlayerHolder.LocationType.WAITROOM );
        rc.setSpawnLoc( index.teamIndex,index.spawnIndex, new FixedLocation(loc) );
        ac.updateArena(arena);
        MessageUtil.sendMessage( sender, "&2waitroom for the " + getSpawnName( index.teamIndex ) + " &2team. Spawn #&6" +
                ( index.spawnIndex + 1 ) + "&2 set to location=&6" + Util.getLocString( loc ) );
        return true;
    }

    private static boolean changeSpectateSpawn( Player sender, Arena arena, BattleArenaController ac, SpawnIndex index ) {
        Location loc = sender.getLocation();
        RoomContainer rc = RoomController.getOrCreateRoom(arena, PlayerHolder.LocationType.SPECTATE);
        rc.setSpawnLoc(index.teamIndex,index.spawnIndex, new FixedLocation(loc));
        ac.updateArena(arena);
        MessageUtil.sendMessage( sender, "&2spectator room #&6" + ( index.teamIndex + 1 ) + "&2. Spawn #&6" + 
                ( index.spawnIndex + 1 ) + "&2 set to location=&6" + Util.getLocString( loc ) );
        return true;
    }

    private static boolean changeVisitorSpawn( Player sender, Arena arena, BattleArenaController ac, SpawnIndex index ) {
        Location loc = sender.getLocation();
        RoomContainer rc = RoomController.getOrCreateRoom(arena, PlayerHolder.LocationType.VISITOR);
        rc.setSpawnLoc(index.teamIndex,index.spawnIndex, new FixedLocation(loc));
        ac.updateArena(arena);
        MessageUtil.sendMessage( sender, "&2Visitor room #&6" + ( index.teamIndex + 1 ) + "&2. Spawn #&6" + 
                ( index.spawnIndex + 1 ) + "&2 set to location=&6" + Util.getLocString( loc ));
        return true;
    }

    private static boolean changeType( CommandSender sender, Arena arena, BattleArenaController ac, String value ) {
        ArenaType t = ArenaType.fromString(value);
        if (t == null){
            MessageUtil.sendMessage(sender,"&ctype &6"+ value + "&c not found. valid types=&6" + ArenaType.getValidList() );
            return false;
        }
        arena.getParams().setType(t);
        ac.updateArena(arena);
        MessageUtil.sendMessage( sender, "&2Altered arena type to &6" + value );
        return true;
    }

    public static boolean restoreDefaultArenaOptions(Arena arena, boolean save) {
        MatchParams ap = arena.getParams();
        MatchParams p = new MatchParams(ap.getType());
        MatchParams parent = ParamController.getMatchParams( ap.getType().getName() );
        p.setRated(ap.isRated());
        p.setParent(parent);
        arena.setParams(p);
        BattleArenaController ac = BattleArena.getBAController();
        if (save)
            ArenaSerializer.saveArenas(arena.getArenaType().getPlugin());
        ac.updateArena(arena);
        return true;
    }

    public static boolean restoreDefaultArenaOptions(MatchParams params) {
        BattleArenaController ac = BattleArena.getBAController();
        for (Arena a : ac.getArenas(params)){
            restoreDefaultArenaOptions(a, false);
        }
        ArenaSerializer.saveArenas(params.getType().getPlugin());
        return true;
    }
}
