package mc.alk.arena.objects.teams;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.Defaults;
import mc.alk.arena.Permissions;
import mc.alk.arena.controllers.messaging.MessageHandler;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.scoreboard.ArenaObjective;
import mc.alk.arena.objects.stats.ArenaStat;
import mc.alk.arena.tracker.Tracker;
import mc.alk.arena.util.MessageUtil;

public abstract class ArenaTeam {
	static int count = 0;
	@Getter final int id = count++; 

    @Getter final protected Set<ArenaPlayer> players = new HashSet<>();
    @Getter final protected Set<ArenaPlayer> deadPlayers = new HashSet<>();
    @Getter final protected Set<ArenaPlayer> leftPlayers = new HashSet<>();

	protected boolean nameManuallySet = false;
	protected boolean nameChanged = true;
	protected String name = null; 
	protected String displayName = null; 
	@Setter protected String scoreboardDisplayName = null; 

    final HashMap<ArenaPlayer, Integer> kills = new HashMap<>();
    final HashMap<ArenaPlayer, Integer> deaths = new HashMap<>();

	/// Pickup teams are transient in nature, once the match end they disband
    @Getter @Setter protected boolean pickupTeam = false;
    @Getter @Setter int minPlayers = -1;
    @Getter @Setter int maxPlayers = -1;
    ArenaObjective objective;
    @Getter @Setter protected ChatColor teamChatColor = null;
	@Getter @Setter protected ItemStack headItem = null;
	@Setter ArenaStat arenaStat;
	@Getter @Setter MatchParams currentParams;

    @Getter @Setter int index = -1;
    @Setter String iDString = null;

	/**
	 * Default Constructor
	 */
	public ArenaTeam(){
		init();
	}
	protected ArenaTeam(ArenaPlayer p) {
		init();
		players.add(p);
		nameChanged = true;
	}
	protected ArenaTeam(Collection<ArenaPlayer> teammates) {
		init();
		players.addAll(teammates);
		nameChanged = true;
	}
	protected ArenaTeam(ArenaPlayer p, Collection<ArenaPlayer> teammates) {
		init();
		players.add(p);
		players.addAll(teammates);
		nameChanged = true;
	}
	public void init(){
		reset();
	}
    public void reset() {
        players.clear();
		deaths.clear();
		kills.clear();
        deadPlayers.clear();
        nameChanged = true;
	}
	protected String createName() {
		if (nameManuallySet || !nameChanged)
			return name;
		
		/// Sort the names and then append them together
		ArrayList<String> list = new ArrayList<>(players.size());
		
		for (ArenaPlayer p:players)
		    list.add(p.getName());
		
		for (ArenaPlayer p:leftPlayers)
		    list.add(p.getName());
		
		if (list.size() > 1)
			Collections.sort(list);
		
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		
		for (String s: list) {
			if (!first) sb.append(", ");
			sb.append(s);
			first = false;
		}
		name = sb.toString();
		nameChanged = false;
		return name;
	}

    public Set<Player> getBukkitPlayers() {
		Set<Player> ps = new HashSet<>();

		for (ArenaPlayer ap: players){
			Player p = ap.getPlayer();
			if (p != null)
				ps.add(p);
		}
		return ps;
	}

	public Set<ArenaPlayer> getLivingPlayers() {
		Set<ArenaPlayer> living = new HashSet<>();
		for (ArenaPlayer p : players){
			if (hasAliveMember(p)){
				living.add(p);}
		}
		return living;
	}

    public boolean wouldBeDeadWithout(ArenaPlayer p) {
		Set<ArenaPlayer> living = getLivingPlayers();
		living.remove(p);
		int offline = 0;
		for (ArenaPlayer ap: living){
			if (!ap.isOnline())
				offline++;
		}
		return living.isEmpty() || living.size() <= offline;
	}

    public boolean hasMember(ArenaPlayer p) { return players.contains(p); }
    public boolean hasLeft(ArenaPlayer p) { return leftPlayers.contains(p); }
    public boolean hasAliveMember(ArenaPlayer p) { return hasMember(p) && !deadPlayers.contains(p); }
	
	public void setHealth( int health ) {
	    for ( ArenaPlayer p: players ) 
	        p.getPlayer().setHealth(health); 
    }
	public void setHunger( int hunger ) {
	    for ( ArenaPlayer p: players ) 
	        p.getPlayer().setFoodLevel(hunger);
	}

    public String getName() { return createName(); }

    public void setName(String _name) {
		name = _name;
		nameManuallySet = true;
	}

    public void setAlive() { deadPlayers.clear(); }
	public void setAlive(ArenaPlayer player){ deadPlayers.remove(player); }

    public boolean isDead() {
		if (deadPlayers.size() >= players.size())
			return true;
		Set<ArenaPlayer> living = getLivingPlayers();
		if (living.isEmpty())
			return true;
		int offline = 0;
		for (ArenaPlayer ap: living){
			if (!ap.isOnline()){
				offline++;}
		}
		return living.size() <= offline;
	}

	public boolean isReady() {
		for (ArenaPlayer ap: getLivingPlayers()){
			if (!ap.isReady())
				return false;
		}
		return true;
	}

    public int size() {return players.size();}

    public int addDeath(ArenaPlayer teamMemberWhoDied) {
		Integer d = deaths.get(teamMemberWhoDied);
		if (d == null){
			d = 0;}
		deaths.put(teamMemberWhoDied, ++d);
		return d;
	}

    public int addKill(ArenaPlayer teamMemberWhoKilled){
		Integer d = kills.get(teamMemberWhoKilled);
		if (d == null){
			d = 0;}
		kills.put(teamMemberWhoKilled, ++d);
		if (objective != null){
			objective.setPoints(teamMemberWhoKilled, d);
			objective.setPoints(this, d);
		}
		return d;
	}

    public int getNKills() {
		int nkills = 0;
		for (Integer i: kills.values()) nkills+=i;
		return nkills;
	}

    public int getNDeaths() {
		int nkills = 0;
		for (Integer i: deaths.values()) nkills+=i;
		return nkills;
	}

    public Integer getNDeaths(ArenaPlayer p) {
		return deaths.get(p);
	}

    public Integer getNKills(ArenaPlayer p) {
		return kills.get(p);
	}

	/**
	 *
	 * @param p ArenaPlayer
	 * @return whether all players are dead
	 */
    public boolean killMember(ArenaPlayer p) {
		if (!hasMember(p))
			return false;
		deadPlayers.add(p);
		return deadPlayers.size() == players.size();
	}

    public boolean allPlayersOffline() {
		for (ArenaPlayer p: players){
			if (p.isOnline())
				return false;
		}
		return true;
	}
    public void sendSystemMessage( String node, Object... args ) {
        sendMessage( MessageHandler.getSystemMessage( node, args) );
    }
    public void sendMessage(String message) {
		for ( ArenaPlayer p : players )
			MessageUtil.sendMessage( p, message );
	}
    public void sendToOtherMembers(ArenaPlayer player, String message) {
		for ( ArenaPlayer p : players ) 
			if ( !p.equals(player) )
				MessageUtil.sendMessage( p, message );
	}

    public String getDisplayName(){ return displayName == null ? getName() : displayName; }

    public void setDisplayName(String teamName){
        displayName = teamName;
        nameManuallySet = true;
    }

	@Override
    public boolean equals(Object other) {
		if (this == other) return true;
		if (!(other instanceof ArenaTeam)) return false;
		return this.hashCode() == other.hashCode();
	}

	@Override
    public int hashCode() { return id;}

	@Override
    public String toString(){ return "[" + getDisplayName() + "]"; }
	
    public boolean hasTeam(ArenaTeam team){
		if (team instanceof CompositeTeam){
			for (ArenaTeam t: ((CompositeTeam)team).getOldTeams()){
				if (this.hasTeam(t))
					return true;
			}
			return false;
		}
        return this.equals(team);
	}

    public String getTeamInfo(Set<UUID> insideMatch){
		StringBuilder sb = new StringBuilder("&eTeam: ");
		if (displayName != null) sb.append(displayName);
		sb.append(" ").append(isDead() ? "&4dead" : "&aalive").append("&e, ");

		for (ArenaPlayer p: players){
			sb.append("&6").append(p.getName());
			boolean isAlive = hasAliveMember(p);
			boolean online = p.isOnline();
			final String inmatch = insideMatch == null? "": ((insideMatch.contains(p.getUniqueId())) ? "&e(in)" : "&4(out)");
			final int k = kills.containsKey(p) ? kills.get(p) : 0;
			final int d = deaths.containsKey(p) ? deaths.get(p) : 0;
			sb.append("&e(&c").append(k).append("&e,&7").append(d).append("&e)");
			sb.append("&e:").append(isAlive ? "&ah=" + p.getHealth() : "&40").
                    append((!online) ? "&4(O)" : "").append(inmatch).append("&e ");
		}
		return sb.toString();
	}

    public String getTeamSummary() {
		StringBuilder sb = new StringBuilder("&6"+getDisplayName());
		for (ArenaPlayer p: players){
			final int k = kills.containsKey(p) ? kills.get(p) : 0;
			final int d = deaths.containsKey(p) ? deaths.get(p) : 0;
			sb.append("&e(&c").append(k).append("&e,&7").append(d).append("&e)");
		}
		return sb.toString();
	}

    public String getOtherNames(ArenaPlayer player) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (ArenaPlayer p: players){
			if (p.equals(player))
				continue;
			if (!first) sb.append(", ");
			sb.append(p.getName());
			first = false;
		}
		return sb.toString();
	}

    public boolean hasSetName() {
		return nameManuallySet;
	}

    public int getPriority() {
		int priority = Integer.MAX_VALUE;
		for (ArenaPlayer ap: players){
			if ( Permissions.getPriority( ap ) < priority)
				priority = Permissions.getPriority( ap );
		}
		return priority;
	}

	public void addPlayer(ArenaPlayer player) {
		players.add(player);
		leftPlayers.remove(player);
		nameChanged = true;
	}

	public boolean removePlayer(ArenaPlayer player) {
		deadPlayers.remove(player);
		leftPlayers.remove(player);
		kills.remove(player);
		deaths.remove(player);
		nameChanged = true;
        return players.remove(player);
    }

	/**
	 * Call when a player has left this team
	 */
    public void playerLeft(ArenaPlayer p) {
		if (!hasMember(p))
			return;
		deadPlayers.remove(p);
		players.remove(p);
		leftPlayers.add(p);
	}

	public void addPlayers(Collection<ArenaPlayer> _players) {
		players.addAll(_players);
		nameChanged = true;
	}

	public void removePlayers(Collection<ArenaPlayer> _players) {
		players.removeAll(_players);
		deadPlayers.removeAll(_players);
		leftPlayers.removeAll(_players);
		for (ArenaPlayer ap: _players){
			kills.remove(ap);
			deaths.remove(ap);
		}
		nameChanged = true;
	}

	public void clear(){
		players.clear();
		deadPlayers.clear();
		leftPlayers.clear();
		nameManuallySet = false;
		nameChanged = false;
		name = "Empty";
		kills.clear();
		deadPlayers.clear();
	}

    public void setArenaObjective(ArenaObjective _objective){
		objective = _objective;
		int tk = 0;
		for (ArenaPlayer player: this.getPlayers()){
			Integer _kills = getNKills(player);
			if (_kills == null) _kills = 0;
			_objective.setPoints(player, _kills);
			tk += _kills;
		}
		_objective.setPoints(this, tk);
	}

	public String getIDString(){
		return iDString == null ? String.valueOf(id) : iDString;
	}

	public String getScoreboardDisplayName(){
		if (scoreboardDisplayName != null)
			return scoreboardDisplayName;
		String _name = getDisplayName();
		return _name.length() > Defaults.MAX_SCOREBOARD_NAME_SIZE ? _name.substring(0,Defaults.MAX_SCOREBOARD_NAME_SIZE) 
		                                                          : _name;
	}

	public ArenaStat getStat() {
		return getStat( getCurrentParams() );
	}
	public ArenaStat getStat(MatchParams params){
        return Tracker.getInterface( params ).getTeamRecord( name );
	}
}
