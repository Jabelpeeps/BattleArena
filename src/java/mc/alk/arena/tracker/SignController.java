package mc.alk.arena.tracker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mc.alk.arena.controllers.Scheduler;
import mc.alk.arena.objects.tracker.Stat;
import mc.alk.arena.objects.tracker.StatSign;
import mc.alk.arena.objects.tracker.StatType;
import mc.alk.arena.serializers.BaseConfig;
import mc.alk.arena.util.MessageUtil;
import mc.alk.arena.util.SerializerUtil;

public class SignController {
    @Getter final Map<String,StatSign> personalSigns = new ConcurrentHashMap<>();
	@Getter final Map<String,StatSign> topSigns = new ConcurrentHashMap<>();
	final Map<String, Map<String,StatSign>> allSigns = new ConcurrentHashMap<>();
	final Map<String,Integer> prevSignCount = new ConcurrentHashMap<>();
	
	@Getter SignSerializer serialiser = new SignSerializer(); 
	boolean updating = false;
	
	public void addSign(StatSign sign){
		switch(sign.getSignType()){
		case TOP:
			topSigns.put(sign.getLocationString(), sign);
			break;
		case PERSONAL:
			personalSigns.put(sign.getLocationString(), sign);
		}
	}

	public void addSigns(Collection<StatSign> signs) {
		for (StatSign sign: signs){
			addSign(sign);}
	}

	public synchronized void updateSigns(){
		if (updating || topSigns.isEmpty())
			return;
		updating = true;
		final Map<String,StatSign> map;
		synchronized(topSigns){
			map = new HashMap<>(topSigns);
		}
		Collection<String> badSigns = new HashSet<>();
		Collection<TrackerInterface> interfaces = Tracker.getAllInterfaces();
		List<StatSign> tops = new ArrayList<>();

		for (TrackerInterface ti : interfaces){
			final String tiName = ti.getInterfaceName().toUpperCase();
			for (String loc : map.keySet()){
				StatSign ss = map.get(loc);
				if (!ss.getDBName().toUpperCase().equals(tiName)){
					continue;}
				Sign s = getSign(loc);
				if (s == null){
					badSigns.add(loc);
					continue;
				}

				switch(ss.getSignType()){
				case TOP:
					tops.add(ss);
					break;
				default:
					break;
				}
			}
			doTopSigns(ti,tops);
			tops = new ArrayList<>();
		}
		synchronized(topSigns){
			for (String s: badSigns){
				topSigns.remove(s);
			}
		}
		updating = false;
	}

	/**
	 * For all of the signs of this type
	 * @param ti TrackerInterface
	 * @param statsigns List<StatSign>
	 */
	private void doTopSigns(TrackerInterface ti, List<StatSign> statsigns){
		if (statsigns == null || statsigns.isEmpty())
			return;

		Collections.sort( statsigns, 
		        ( arg0, arg1 ) -> {
        				if (arg0.getStatType() == null && arg1.getStatType() == null) return 0;
        				else if (arg1.getStatType() == null ) return -1;
        				else if (arg0.getStatType() == null ) return 1;
        				return arg0.getStatType().compareTo(arg1.getStatType());
        		});

		StatType st = statsigns.get(0).getStatType();
		List<Stat> stats = new ArrayList<>();
		List<StatSign> update = new ArrayList<>();
		int max = 0;

		int offset = 0;
		for (StatSign ss: statsigns){
			if (ss.getStatType() != st){

				if (st != null)
					updateSigns(ti,update,max, st,offset++);

				st = ss.getStatType();
				stats.clear();
				update = new ArrayList<>();
				max =0;
			}
			update.add(ss);

			int size = this.getUpDownCount(ss);
			Integer prevSize = prevSignCount.get(ss.getLocationString());
			if (prevSize == null || prevSize != size){
				prevSignCount.put(ss.getLocationString(), size);
			}

			if (max  < size){
				max = size;}
		}
		if (!update.isEmpty() && st != null){
			updateSigns(ti,update,max,st,offset++);}
	}

	@AllArgsConstructor
	class SignResult {
		final List<Sign> signs;
		final int statSignIndex;
	}

	SignResult getUpDown(StatSign ss) {
		Sign s = getSign(ss.getLocation());
		if (s == null)
			return null;
		Sign sign = null;
		World w = s.getLocation().getWorld();
		List<Sign> signList = new ArrayList<>();
		boolean foundUpStatSign = false;

		int x = s.getLocation().getBlockX();
		int y = s.getLocation().getBlockY();
		int z = s.getLocation().getBlockZ();
		LinkedList<Sign> upsignList = new LinkedList<>();
		while ((sign = getSign(w,x,++y,z)) != null){

			if (breakLine(sign.getLine(0))){
				foundUpStatSign = true;
				break;
			}
			upsignList.addFirst(sign);
		}

		if (!foundUpStatSign){
			signList.addAll(upsignList);}

		int originalSignIndex = signList.size();
		signList.add(s);

		sign = null;

		x = s.getLocation().getBlockX();
		y = s.getLocation().getBlockY();
		z = s.getLocation().getBlockZ();
		while ((sign = getSign(w,x,--y,z)) != null){
			String line = sign.getLine(0);

			if (breakLine(line)) break;
			signList.add(sign);
		}
		return new SignResult(signList,originalSignIndex);
	}

	private int getUpDownCount(StatSign ss) {
	    
		Sign s = getSign( ss.getLocation() );
		
		if ( s == null ) return 0;
		
		int count = 1;
		
		World w = s.getLocation().getWorld();
		int x = s.getLocation().getBlockX();
		int y = s.getLocation().getBlockY();
		int z = s.getLocation().getBlockZ();
		int oldY = y;
		
		while ( getSign( w, x, ++y, z ) != null) {
			count++;
		}
		y = oldY;
		
		while ( getSign( w, x, --y, z ) != null) {
			count++;
		}
		return count;
	}

	private void updateSigns(final TrackerInterface ti, final List<StatSign> update, final int max, 
	                                                        final StatType type, final int offset) {	    
		Scheduler.scheduleAsynchronousTask(  
		        () -> { List<Stat> toplist= ti.getTopX(type, max * 4);
		        
        				if ( toplist != null && !toplist.isEmpty() && Tracker.isEnabled() ) {
        				    
        					Scheduler.scheduleSynchronousTask( 
        					        new UpdateSigns( ti.getInterfaceName(), update,toplist) );
        				}
		        }, 2L * offset );
	}

	class UpdateSigns implements Runnable{
		final String dbName;
		final List<StatSign> statSignList;
		final List<Stat> statList;

		public UpdateSigns(String _dbName, List<StatSign> update, List<Stat> toplist) {
			dbName = StringUtils.capitalize(_dbName);
			statSignList = update;
			statList = toplist;
		}

		@Override
		public void run() {
			for (StatSign ss: statSignList){
				Sign s = getSign(ss.getLocation());
				if (s == null)
					continue;
				SignResult sr = getUpDown(ss);
				if (sr == null || sr.signs.isEmpty())
					continue;
				List<Sign> signList = sr.signs;

				boolean quit = false;
				int curTop = 0;
				for (int i =0;i< signList.size() && !quit;i++){
					int startIndex = 0;
					s = signList.get(i);
					if (i == sr.statSignIndex){
						s.setLine(0, MessageUtil.colorChat("[&e"+dbName+"&0]"));
						s.setLine(1, MessageUtil.colorChat("["+ss.getStatType().color()+ss.getStatType()+"&0]"));
						s.setLine(2, MessageUtil.colorChat("&cNo Records"));
						startIndex = 2;
					}
					for (int j=startIndex;j< 4;j++){
						if (curTop >= statList.size()){
							quit = true;
							break;
						}
						int val = (int) statList.get(curTop).getStat(ss.getStatType());
						String statLine = formatStatLine(statList.get(curTop).getName(), val, curTop);
						if (!s.getLine(j).equals(statLine))
							s.setLine(j, statLine+"         ");
						curTop++;
					}
					s.update(true);
				}
			}
		}

        private String formatStatLine(String name, int val, int curTop) {
            StringBuilder sb = new StringBuilder(15);
            sb.append(curTop + 1).append(".");
            int length = sb.length() + intStringLength(val) + 1;
            if (name.length() + length > 15){
                sb.append(name.substring(0, 15-length )).
                        append("~").append(String.valueOf(val));
            } else {
                sb.append(name).append(StringUtils.repeat(" ", 16 - length - name.length())).
                        append(String.valueOf(val));
            }
            return sb.toString();
        }
	}

	public static int intStringLength(int i) {
		return i==0 ? (1) : (i<0) ? (int)Math.log10(Math.abs(i))+2 : (int)Math.log10(i)+1;
	}

	private boolean breakLine(String line) {
		return line != null && (!line.isEmpty() && line.startsWith("["));
	}

	private Sign getSign(World w, int x, int y, int z) {
		Block b = w.getBlockAt(x, y, z);
		Material t = b.getType();
		return t == Material.SIGN || t == Material.SIGN_POST || t==Material.WALL_SIGN ? (Sign)b.getState(): null;
	}

	Sign getSign(Location l) {
		if (l == null)
			return null;
		Material t = l.getBlock().getType();
		return t == Material.SIGN || t == Material.SIGN_POST || t==Material.WALL_SIGN ? (Sign)l.getBlock().getState(): null;
	}

	private Sign getSign(String loc) {
		Location l = SerializerUtil.getLocation(loc);
		if (l == null)
			return null;
		Material t = l.getBlock().getType();
		return t == Material.SIGN || t == Material.SIGN_POST || t==Material.WALL_SIGN ? (Sign)l.getBlock().getState(): null;
	}

	public StatSign getStatSign(Location location) {
		String key = StatSign.getLocationString(location);
		
		if (personalSigns.containsKey(key))
			return personalSigns.get(key);
		
        return topSigns.get(key);
	}

	public void clickedSign(Player player, Sign s, StatSign ss) {
		switch(ss.getSignType()){
		case TOP:
			updateTopSign(player, s, ss);
			break;
		case PERSONAL:
			updatePersonalSign(player, s, ss);
			break;
		}
	}
	private void updatePersonalSign(Player player, Sign s, StatSign ss) {
		updateSign(player,s,ss);
	}

	private void updateTopSign(Player player, Sign s, StatSign ss) {
		updateSign(player,s,ss);
	}
	private void updateSign(Player player, Sign s, StatSign ss){
		String[] lines = s.getLines();
		TrackerInterface ti = Tracker.getInterface(ss.getDBName());
		Stat stat = ti.getRecord(player);
		if (stat == null)
			return;

		lines[0] = "&0Your Stats";
		lines[1] = "&0[&9"+stat.getRating() +"&0]";
		int len = (stat.getWins() +"/" + stat.getLosses()).length();
		lines[2] = (len <= 10) ? "&2"+stat.getWins() +"&0/&4" + stat.getLosses() :
			stat.getWins() +"/" + stat.getLosses();
		if (lines[2].length() <= 12)
			lines[2] = "&0W/L " + lines[2];
		lines[3] = "&0Streak: &9" + stat.getStreak() +"";
		for (int i=0;i<lines.length;i++){
			lines[i] = MessageUtil.colorChat(lines[i]);
		}
        player.sendSignChange( s.getLocation(), lines );
	}

	public void clearSigns() {
		topSigns.clear();
		personalSigns.clear();
	}
	public void removeSignAt(Location location) {
		String l = StatSign.getLocationString(location);
		topSigns.remove(l);
		personalSigns.remove(l);
	}
	
	public class SignSerializer extends BaseConfig {

	    public void saveAll() {
	        Map<String,StatSign> map = getTopSigns();
	        
	        if ( map != null ) {
	            List<StatSign> l = new ArrayList<>(map.values());
	            config.set("topSigns", l);
	        }
	        map = getPersonalSigns();
	        
	        if ( map != null ) {
	            List<StatSign> l = new ArrayList<>(map.values());
	            config.set("personalSigns", l);
	        }
	        save();
	    }

	    public void loadAll(){
	        String[] types = new String[]{"topSigns","personalSigns"};
	        clearSigns();
	        
	        for ( String type : types ) {
	            List<?> signs = config.getList(type);
	            
	            if (signs == null) continue;
	            
	            for (Object o : signs) {
	                if (o == null || !(o instanceof StatSign)) continue;
	                
	                if (!stillSign((StatSign)o)) continue;
	                
	                addSign((StatSign) o);
	            }
	        }
	    }

	    private boolean stillSign(StatSign o) {
	        String l = o.getLocationString();
	        if (l == null)
	            return false;
	        try {
	            Location loc = SerializerUtil.getLocation(l);
	            if (loc == null) return false;
	            
	            Material mat = loc.getWorld().getBlockAt(loc).getType();
	            if ( mat != Material.SIGN && mat != Material.SIGN_POST && mat != Material.WALL_SIGN)
	                return false;
	        } 
	        catch( Exception e){
	            return false;
	        }
	        return true;
	    }
	}
}
