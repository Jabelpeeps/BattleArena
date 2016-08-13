package mc.alk.arena.objects.options;

import static mc.alk.arena.objects.options.TransitionOption.ADDPERMS;
import static mc.alk.arena.objects.options.TransitionOption.DISGUISEALLAS;
import static mc.alk.arena.objects.options.TransitionOption.DOCOMMANDS;
import static mc.alk.arena.objects.options.TransitionOption.ENCHANTS;
import static mc.alk.arena.objects.options.TransitionOption.EXPERIENCE;
import static mc.alk.arena.objects.options.TransitionOption.FLIGHTSPEED;
import static mc.alk.arena.objects.options.TransitionOption.GAMEMODE;
import static mc.alk.arena.objects.options.TransitionOption.GIVECLASS;
import static mc.alk.arena.objects.options.TransitionOption.GIVEDISGUISE;
import static mc.alk.arena.objects.options.TransitionOption.GIVEITEMS;
import static mc.alk.arena.objects.options.TransitionOption.HEALTH;
import static mc.alk.arena.objects.options.TransitionOption.HEALTHP;
import static mc.alk.arena.objects.options.TransitionOption.HUNGER;
import static mc.alk.arena.objects.options.TransitionOption.INVINCIBLE;
import static mc.alk.arena.objects.options.TransitionOption.INVULNERABLE;
import static mc.alk.arena.objects.options.TransitionOption.LEVELRANGE;
import static mc.alk.arena.objects.options.TransitionOption.MAGIC;
import static mc.alk.arena.objects.options.TransitionOption.MAGICP;
import static mc.alk.arena.objects.options.TransitionOption.MONEY;
import static mc.alk.arena.objects.options.TransitionOption.NEEDARMOR;
import static mc.alk.arena.objects.options.TransitionOption.NEEDITEMS;
import static mc.alk.arena.objects.options.TransitionOption.NOINVENTORY;
import static mc.alk.arena.objects.options.TransitionOption.PVPOFF;
import static mc.alk.arena.objects.options.TransitionOption.PVPON;
import static mc.alk.arena.objects.options.TransitionOption.RANDOMRESPAWN;
import static mc.alk.arena.objects.options.TransitionOption.RANDOMSPAWN;
import static mc.alk.arena.objects.options.TransitionOption.RESPAWNTIME;
import static mc.alk.arena.objects.options.TransitionOption.SAMEWORLD;
import static mc.alk.arena.objects.options.TransitionOption.TAKEITEMS;
import static mc.alk.arena.objects.options.TransitionOption.TELEPORTTO;
import static mc.alk.arena.objects.options.TransitionOption.WITHINDISTANCE;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import mc.alk.arena.Defaults;
import mc.alk.arena.objects.ArenaClass;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.ArenaSize;
import mc.alk.arena.objects.CommandLineString;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.MatchState;
import mc.alk.arena.objects.PVPState;
import mc.alk.arena.objects.StateGraph;
import mc.alk.arena.objects.exceptions.InvalidOptionException;
import mc.alk.arena.plugins.MobArenaInterface;
import mc.alk.util.EffectUtil;
import mc.alk.util.InventoryUtil;
import mc.alk.util.InventoryUtil.ArmorLevel;
import mc.alk.util.MinMax;

public class StateOptions {

    EnumMap<TransitionOption,Object> options = new EnumMap<>(TransitionOption.class);

    public StateOptions() {}
    public StateOptions( StateOptions o ) {
        if (o == null) return;
        
        if (o.options != null) 
            options = new EnumMap<>(o.options);
    }

    public void addOptions(StateOptions stateOptions) {
        if (stateOptions.options != null) 
            addOptions(stateOptions.options);
    }

    private void addOptions(Map<TransitionOption,Object> optionSet) {
        if ( options == null )
            options = new EnumMap<>(optionSet);
        else
            options.putAll(optionSet);
    }
  
    public void setOptions(Set<String> optionSet) {
        options = new EnumMap<>(TransitionOption.class);
        
        for ( String s: optionSet ){
            TransitionOption so = TransitionOption.valueOf(s);
            if (so==null)
                continue;
            this.options.put(so,null);
        }
    }

    public void setOptions(Map<TransitionOption,Object> _options) {
        options = new EnumMap<>(_options);
    }
    
    public boolean hasOption(TransitionOption option) {
         return options != null && options.containsKey( option );      
    }

    public boolean hasAnyOption(TransitionOption... ops) {        
        if ( options != null ) {
            for (TransitionOption op: ops) {
                if (options.containsKey(op))
                    return true;
            }
        }
        return false;
    }

    @SuppressWarnings( "unchecked" )
    public List<ItemStack> getGiveItems() {
        Object o = options.get(GIVEITEMS);
        return o == null ? null : (List<ItemStack>) o;
    }

    @SuppressWarnings( "unchecked" )
    public List<ItemStack> getNeedItems() {
        Object o = options.get(NEEDITEMS);
        return o == null ? null : (List<ItemStack>) o;
    }

    @SuppressWarnings( "unchecked" )
    public List<ItemStack> getTakeItems() {
        Object o = options.get(TAKEITEMS);
        return o == null ? null : (List<ItemStack>) o;
    }

    @SuppressWarnings( "unchecked" )
    public List<PotionEffect> getEffects(){
        Object o = options.get(ENCHANTS);
        return o == null ? null : (List<PotionEffect>) o;
    }

    public Double getHealth() {return getDouble(HEALTH);}
    public Double getHealthP() {return getDouble(HEALTHP);}
    public Integer getHunger() {return getInt(HUNGER);}
    public Integer getMagic() { return getInt(MAGIC);}
    public Integer getMagicP() { return getInt(MAGICP);}
    public Double getWithinDistance() {return getDouble(WITHINDISTANCE);}
    public GameMode getGameMode() {return getGameMode(GAMEMODE);}
    
    @SuppressWarnings( "unchecked" )
    public List<CommandLineString> getDoCommands() {
        final Object o = options.get(DOCOMMANDS);
        return o == null ? null : (List<CommandLineString>) o;
    }

    public Integer getInt(TransitionOption experience){
        final Object o = options.get(experience);
        return o == null ? null : (Integer) o;
    }

    public Double getDouble(TransitionOption money){
        final Object o = options.get(money);
        return o == null ? null : (Double) o;
    }

    public Float getFloat(TransitionOption flightspeed){
        final Object o = options.get(flightspeed);
        return o == null ? null : (Float) o;
    }

    public String getString(TransitionOption disguiseallas){
        final Object o = options.get(disguiseallas);
        return o == null ? null : (String) o;
    }

    public GameMode getGameMode(TransitionOption gamemode){
        final Object o = options.get(gamemode);
        return o == null ? null : (GameMode) o;
    }

    public Double getMoney(){return getDouble(MONEY);}
    public boolean hasMoney(){
        Double d = getDouble(MONEY);
        return d != null && d > 0;
    }
    public Float getFlightSpeed(){return getFloat(FLIGHTSPEED);}
    public Integer getInvulnerable(){return getInt(INVULNERABLE);}
    public Integer getRespawnTime(){return getInt(RESPAWNTIME);}
    public Integer getExperience(){return getInt(EXPERIENCE);}
    public String getDisguiseAllAs() {return getString(DISGUISEALLAS);}

    public boolean playerReady(ArenaPlayer p, World w) {
        if (p==null || !p.isOnline() || p.isDead() || p.getPlayer().isSleeping())
            return false;
        if (options.containsKey(NEEDITEMS)){
            List<ItemStack> items = getNeedItems();
            Inventory inv = p.getInventory();
            for (ItemStack is : items){
                if (InventoryUtil.getItemAmountFromInventory(inv, is) < is.getAmount())
                    return false;
            }
        }
        /// Inside MobArena?
        if (MobArenaInterface.hasMobArena() && MobArenaInterface.insideMobArena(p)){
            return false;
        }
        if (options.containsKey(GAMEMODE)){
            GameMode gm = getGameMode();
            if (p.getPlayer().getGameMode() != gm){
                return false;}
        }

        if (options.containsKey(NOINVENTORY)){
            if (InventoryUtil.hasAnyItem(p.getPlayer()))
                return false;
        }
        if (options.containsKey(SAMEWORLD) && w!=null){
            if (p.getLocation().getWorld().getUID() != w.getUID())
                return false;
        }
        if (options.containsKey( NEEDARMOR )){
            if (!InventoryUtil.hasArmor(p.getPlayer()))
                return false;
        }
        if (options.containsKey(LEVELRANGE)){
            MinMax mm = (MinMax) options.get(LEVELRANGE);
            if (!mm.contains(p.getLevel()))
                return false;
        }
        return true;
    }

    public String getNotReadyMsg(String header) {
        StringBuilder sb = new StringBuilder();
        boolean hasSomething = false;
        if (header != null){
            sb.append(header);
            hasSomething = true;
        }
        if (options.containsKey(NEEDITEMS)){
            List<ItemStack> items = getNeedItems();
            hasSomething = true;
            for (ItemStack is : items){
                sb.append("&5 - &6").append(is.getAmount()).append(" ").append(is.getData());
            }
        }
        if (options.containsKey(NOINVENTORY)){
            hasSomething = true;
            sb.append("&5 - &6Clear Inventory");
        }
        if (options.containsKey(GAMEMODE)){
            hasSomething = true;
            GameMode gm = getGameMode();
            sb.append("&5 - &6GameMode=").append(gm.toString());
        }
        if ( options.containsKey( NEEDARMOR ) ){
            hasSomething = true;
            sb.append("&5 - &6Armor");
        }
        if (options.containsKey(LEVELRANGE)){
            MinMax mm = (MinMax) options.get(LEVELRANGE);
            sb.append("&a - lvl=").append(mm.toString());
        }
        return hasSomething ? sb.toString() : null;
    }

    public String getNotReadyMsg(ArenaPlayer p, World w, String headerMsg) {
        StringBuilder sb = new StringBuilder(headerMsg);
        boolean isReady = true;
        if (options.containsKey(NEEDITEMS)){
            Inventory inv = p.getInventory();
            List<ItemStack> items = getNeedItems();
            for (ItemStack is : items){
                int amountInInventory =InventoryUtil.getItemAmountFromInventory(inv, is);
                if (amountInInventory < is.getAmount()){
                    int needed = amountInInventory - is.getAmount();
                    sb.append("&5 - &e").append(needed).append(" ").append(is.getType()).append("\n");
                    isReady = false;
                }
            }
        }
        if (options.containsKey(GAMEMODE)){
            GameMode gm = getGameMode();
            if (p.getPlayer().getGameMode() != gm){
                sb.append("&5 -&e a &6You need to be in &c").append(gm).append("&e mode \n");
                isReady = false;
            }
        }
        if (options.containsKey(NOINVENTORY)){
            if (InventoryUtil.hasAnyItem(p.getPlayer())){
                sb.append("&5 -&e a &6Clear Inventory\n");
                isReady = false;
            }
        }
        if (options.containsKey(SAMEWORLD) && w!=null){
            if (p.getLocation().getWorld().getUID() != w.getUID()){
                sb.append("&5 -&c Not in same world\n");
                isReady = false;
            }
        }
        /// Inside MobArena?
        if (MobArenaInterface.hasMobArena() && MobArenaInterface.insideMobArena(p)){
            isReady = false;
            sb.append("&5 - &4You are Inside Mob Arena");
        }

        if ( options.containsKey( NEEDARMOR ) ){
            if (!InventoryUtil.hasArmor(p.getPlayer())){
                sb.append("&&5 - &6Armor\n");
                isReady = false;
            }
        }

        if (options.containsKey(LEVELRANGE)){
            MinMax mm = (MinMax) options.get(LEVELRANGE);
            if (!mm.contains(p.getLevel())){
                sb.append("&a - lvl=").append(mm.toString());
                isReady = false;
            }
        }
        return isReady? null : sb.toString();
    }

    public String getPrizeMsg(String header) {
        return getPrizeMsg(header,null);
    }
    public String getPrizeMsg(String header, Double poolMoney) {
        StringBuilder sb = new StringBuilder();
        boolean hasSomething = false;
        if (header != null){
            sb.append(header);
            hasSomething = true;
        }
        if (options.containsKey(EXPERIENCE)){
            hasSomething = true;
            sb.append("&5 - &2").append(getExperience()).append(" experience");
        }
        if (hasMoney()){
            hasSomething = true;
            sb.append("&5 - &6").append(getMoney()).append(" ").append(Defaults.MONEY_STR);
        }
        if (poolMoney != null){
            hasSomething = true;
            sb.append("&5 - &6").append(poolMoney).append(" ").append(Defaults.MONEY_STR);
        }

        if (getGiveItems() != null){
            hasSomething = true;
            List<ItemStack> items = getGiveItems();
            ArmorLevel lvl = InventoryUtil.hasArmorSet(items);
            if (lvl != null){
                sb.append("&5 - &a").append(lvl.toString()).append(" ARMOR");
            }
            for (ItemStack is : items){
                if (lvl != null && InventoryUtil.sameMaterial(lvl,is))
                    continue;
                String enchanted = !is.getEnchantments().isEmpty() ? " &4Enchanted ": "";
                sb.append("&5 - &a").append(is.getAmount()).append(enchanted).append(is.getType().toString());
            }
        }
        if (options.containsKey( ENCHANTS )){
            hasSomething = true;
            for (PotionEffect ewa : getEffects()){
                if (ewa != null)
                    sb.append("&5 - &b").append(EffectUtil.getCommonName(ewa));
            }
        }

        return hasSomething? sb.toString() : null;
    }

    public PVPState getPVP() {
        if (options.containsKey(PVPON)) {
            return PVPState.ON;
        } 
        else if (options.containsKey(PVPOFF)){
            return PVPState.OFF;
        } 
        else if (options.containsKey(INVINCIBLE)){
            return PVPState.INVINCIBLE;
        }
        return null;
    }

    public boolean randomRespawn() {
        return options.containsKey(RANDOMRESPAWN) ||
                options.containsKey(RANDOMSPAWN);
    }

    public Map<TransitionOption, Object> getOptions() {
        return options;
    }

    public void addOption(TransitionOption option) throws InvalidOptionException {
        if (option.hasValue()) throw new InvalidOptionException("TransitionOption needs a value!");
        addOption(option, null);
    }

    public void addOption(TransitionOption option, Object value) throws InvalidOptionException {
        if (option.hasValue() && value==null) throw new InvalidOptionException("TransitionOption needs a value!");
        if ( options == null )
            options = new EnumMap<>(TransitionOption.class);
        options.put(option,value);
    }
    
    public Object removeOption( TransitionOption option ) {
        if ( option != null )
            return options.remove( option );
        
        return null;
    }

    public boolean containsAll(StateOptions tops) {
        if (tops.options == null && options != null )
            return false;
        if (tops.options == null)
            return true;
        for (TransitionOption op: tops.options.keySet()){
            if (!options.containsKey(op)){
                return false;
            }
            if (op.hasValue() && !options.get(op).equals(tops.options.get(op)))
                return false;
        }
        return true;
    }

    public static String getInfo(MatchParams mp, String name) {
        StringBuilder sb = new StringBuilder();
        StateGraph sg = mp.getStateGraph();
        String required = sg.getRequiredString(null);
        String prestart = sg.getGiveString(MatchState.ONPRESTART);
        String start = sg.getGiveString(MatchState.ONSTART);
        String onspawn = sg.getGiveString(MatchState.ONSPAWN);
        String prizes = sg.getGiveString(MatchState.WINNERS);
        boolean rated = mp.isRated();
        String teamSizes = ArenaSize.rangeString(mp.getMinTeamSize(), mp.getMaxTeamSize());
        sb.append("&eThis is ").append(rated ? "a &4Rated" : "an &aUnrated").
                append("&e ").append(name).append(". ");
        sb.append("&eTeam size=&6").append(teamSizes);
        sb.append("\n&eRequirements to Join:");
        sb.append(required==null? "&aNone" : required);
        if (prestart != null || start !=null || onspawn != null){
            sb.append("\n&eYou are given:");
            if (prestart != null) sb.append(prestart);
            if (start != null) sb.append(start);
            if (onspawn != null) sb.append(onspawn);
        }
        sb.append("\n&ePrize for &5winning&e a game:");
        sb.append(prizes==null? "&aNone" : prizes);
        return sb.toString();
    }

    @SuppressWarnings( "unchecked" )
    public Map<Integer, ArenaClass> getClasses(){
        Object o = options.get(GIVECLASS);
        return o == null ? null : (Map<Integer, ArenaClass>) o;
    }

    @SuppressWarnings( "unchecked" )
    public Map<Integer, String> getDisguises(){
        Object o = options.get(GIVEDISGUISE);
        return o == null ? null : (Map<Integer, String>) o;
    }


    @SuppressWarnings( "unchecked" )
    public List<String> getAddPerms() {
        final Object o = options.get(ADDPERMS);
        return o == null ? null : (List<String>) o;
    }

    /// TODO, not sure this will work properly, I really want to remove those perms that were added in another section!!
    @SuppressWarnings( "unchecked" )
    public List<String> getRemovePerms() {
        final Object o = options.get(ADDPERMS);
        return o == null ? null : (List<String>) o;
    }

    public Location getTeleportToLoc() {return returnLoc(TELEPORTTO);}

    private Location returnLoc(TransitionOption teleportto){
        final Object o = options.get(teleportto);
        return o == null ? null : (Location) o;
    }

    private ChatColor getColor(TransitionOption transitionOption, StateOptions so) {
        return so !=null && so.options.containsKey(transitionOption) ? ChatColor.WHITE : ChatColor.GOLD;
    }
    public String getOptionString(StateOptions so) {
        if (options == null) return "[SO empty]";
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Entry<TransitionOption, Object> entry: options.entrySet()){
            if (!first) sb.append("&2, " );
            first = false;
            sb.append(getColor(entry.getKey(), so).toString());
            sb.append(entry.getKey());
            Object value = so !=null && so.options.containsKey(entry.getKey()) ?
                    so.options.get(entry.getKey()) :
                    entry.getValue();
            if (value != null){
                TransitionOption i = entry.getKey();
                if (i.equals(TransitionOption.GIVECLASS) ||
                        i.equals(TransitionOption.ENCHANTS) ||
                        i.equals(TransitionOption.GIVEDISGUISE)) {
                    continue;
                }
                sb.append(":").append(value);
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public String toString(){
        return getOptionString(null);
    }
}
