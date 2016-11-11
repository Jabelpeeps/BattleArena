package mc.alk.arena.util;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import lombok.Getter;
import mc.alk.arena.Defaults;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.StateGraph;
import mc.alk.arena.objects.options.TransitionOption;
import mc.alk.arena.objects.teams.ArenaTeam;

public class TeamUtil {
	static final int NTEAMS = 35;
	static final List<TeamHead> teamHeads = new ArrayList<>();
//	static final HashMap<String, Integer> map = new HashMap<>();

	public static void removeTeamHead(final int color, Player p) {
		ItemStack item = getTeamHead(color);
		final PlayerInventory inv = p.getInventory();
		if (inv != null && inv.getHelmet() != null && inv.getHelmet().getType() == item.getType()){
			inv.setHelmet(new ItemStack(Material.AIR));
		}
	}
	public static String getTeamName(int index) {
		return index < teamHeads.size() ? teamHeads.get(index).getName() : "Team" + index;
	}
	public static void setTeamHead(final int color, ArenaTeam team) {
		for (ArenaPlayer p: team.getPlayers()){
			setTeamHead(color,p);
		}
	}
	public static ItemStack getTeamHead(int index){
		return index < teamHeads.size() ? teamHeads.get(index).getHeadItem() : new ItemStack(Material.DIRT);
	}
	public static ChatColor getTeamChatColor(int index){
		return index < teamHeads.size() ? teamHeads.get(index).getChatColor() : ChatColor.WHITE;
	}
	public static Color getTeamColor(Integer index){
		return index != null && index < teamHeads.size() ? teamHeads.get(index).getColor() : Color.WHITE;
	}
    public static DyeColor getDyeColor(Integer index){
        return index != null && index < teamHeads.size() ? teamHeads.get(index).getDyeColor() : DyeColor.WHITE;
    }
    public static void setTeamHead(final int index, ArenaPlayer player) {
		setTeamHead( getTeamHead(index), player );
	}
	public static void setTeamHead(final ItemStack item, ArenaPlayer player) {
		setTeamHead( item, player.getPlayer() );
	}
	public static void setTeamHead(ItemStack item, Player p) {
		if (p.isOnline() && !p.isDead()) {
			ItemStack is = p.getInventory().getHelmet();
            p.getInventory().setHelmet(item);
			
			if (is != null && is.getType() != Material.AIR && is.getType()!= Material.WOOL)
				InventoryUtil.addItemToInventory(p, is.clone(), is.getAmount(),true, true);
			p.updateInventory();
		}
	}

//	public static Integer getTeamIndex(String op) {
//		if (map.containsKey(op.toUpperCase()))
//			return map.get(op.toUpperCase());
//		try {
//			return Integer.valueOf(op);
//		} 
//		catch( NumberFormatException e ){
//			return null;
//		}
//	}

    public static Integer getFromHumanTeamIndex(String op) {
//        if (map.containsKey(op.toUpperCase()))
//            return map.get(op.toUpperCase());
        try {
            return Integer.valueOf(op) -1;
        } 
        catch( NumberFormatException e ) {
            return null;
        }
    }

    public static void addTeamHead(String name, TeamHead th) {
		teamHeads.add(th);
//		map.put( name.toUpperCase(), teamHeads.size() - 1 );
	}

	public static String formatName(ArenaTeam t){
		StringBuilder sb = new StringBuilder("&e " + t.getDisplayName());

		for ( ArenaPlayer p : t.getPlayers() ) {
			sb.append("&e(&c").append( t.getNKills(p) ).append("&e,&7").append( t.getNDeaths(p) ).append("&e)");
		}
		return sb.toString();
	}

    public static void initTeam(ArenaTeam team, MatchParams params) {
        team.reset();
        team.setCurrentParams(params);
        int index = team.getIndex();
        MatchParams teamParams = null;
        boolean isTeamParam = false;
        if (index != -1) {
            teamParams = params.getTeamParams(index);
        }
        if (teamParams == null) {
            teamParams = params;
        } else {
            isTeamParam = true;
        }

        team.setMinPlayers(teamParams.getMinTeamSize());
        team.setMaxPlayers(teamParams.getMaxTeamSize());

        boolean alwaysTeamNames = false;
        if (index != -1){
            StateGraph tops = teamParams.getStateGraph();
            team.setTeamChatColor( getTeamChatColor(index));
            if (tops != null){
                if (tops.hasOption(TransitionOption.WOOLTEAMS) && teamParams.getMaxTeamSize() > 1 ||
                        tops.hasOption(TransitionOption.ALWAYSWOOLTEAMS)){
                    team.setHeadItem( getTeamHead(index));
                }
                alwaysTeamNames = tops.hasOption(TransitionOption.ALWAYSTEAMNAMES);
            }

            String name;
            if (!isTeamParam || teamParams.getDisplayName() == null) {
                name = getTeamName(index);
                if ( alwaysTeamNames ||
                        (!team.hasSetName() && team.getDisplayName().length() > Defaults.MAX_TEAM_NAME_APPEND)){
                    team.setDisplayName(name);
                }
            } else {
                name = teamParams.getDisplayName();
                team.setDisplayName(name);
            }
            team.setScoreboardDisplayName(name.length() > Defaults.MAX_SCOREBOARD_NAME_SIZE ?
                    name.substring(0,Defaults.MAX_SCOREBOARD_NAME_SIZE) : name);
        }

    }
    public static class TeamHead {
        @Getter final String name;
        @Getter final ItemStack headItem;
        @Getter final ChatColor chatColor;
        @Getter final DyeColor dyeColor;
        @Getter final Color color;

        public TeamHead(ItemStack is, String _name, Color _color){
            headItem = is;
            name = _name;
            chatColor = MessageUtil.getFirstColor(_name);
            color = _color;
            dyeColor = findDyeColor(_color);
        }

        private DyeColor findDyeColor(Color _color) {
            
            DyeColor closest = DyeColor.WHITE;
            double min = Float.MAX_VALUE;
            
            for (DyeColor dc : DyeColor.values()) {
                Color c = dc.getColor();
                double dev = (Math.pow(Math.abs(c.getRed() - _color.getRed()),2)) +
                        (Math.pow(Math.abs(c.getGreen() - _color.getGreen()),2)) +
                                (Math.pow(Math.abs(c.getBlue() - _color.getBlue()),2));
                if (dev < min) {
                    min = dev;
                    closest = dc;
                }
            }
            return closest;
        }
    }
}
