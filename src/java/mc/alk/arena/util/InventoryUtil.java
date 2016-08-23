package mc.alk.arena.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

import lombok.AllArgsConstructor;
import mc.alk.arena.Defaults;

public class InventoryUtil {
	static final String version = "BA InventoryUtil 2.1.7";
	static final boolean DEBUG = false;

    public enum ArmorLevel { DISGUISE, WOOL, LEATHER, IRON, GOLD, CHAINMAIL, DIAMOND }
    public enum ArmorType { BOOTS, LEGGINGS, CHEST, HELM }
    
	@AllArgsConstructor
    public static class Armor {
        final public ArmorType type;
		final public ArmorLevel level;
	}

	public static class EnchantmentWithLevel{
		public EnchantmentWithLevel(){}
		public EnchantmentWithLevel( boolean _all ){ all = _all; }
		public Enchantment e;
		public Integer lvl;
		boolean all = false;
		@Override
		public String toString(){ return ( e != null ? e.getName() 
		                                             : "null" ) + ":" + lvl; }
	}

	public static class PInv {
		public ItemStack[] contents;
		public ItemStack[] armor;
		public PInv() {}
		public PInv(PlayerInventory inventory) {
			contents = inventory.getContents();
			setArmor(inventory);
		}
		public PInv(List<ItemStack> items){
			contents = items.toArray(new ItemStack[items.size()]);
			armor = new ItemStack[0];
		}
		public void setArmor(PlayerInventory inventory){
			armor = new ItemStack[4];
			armor[ArmorType.HELM.ordinal()] = inventory.getHelmet();
			armor[ArmorType.CHEST.ordinal()] = inventory.getChestplate();
			armor[ArmorType.LEGGINGS.ordinal()] = inventory.getLeggings();
			armor[ArmorType.BOOTS.ordinal()] = inventory.getBoots();
		}
	}

	public static Enchantment getEnchantmentByCommonName(String iname){
		iname = iname.toLowerCase();
		if (iname.contains("smite")) return Enchantment.DAMAGE_UNDEAD;
		if (iname.contains("sharp")) return Enchantment.DAMAGE_ALL;
		if (iname.contains("sharp")) return Enchantment.DAMAGE_ARTHROPODS;
		if (iname.contains("fire") && iname.contains("prot")) return Enchantment.PROTECTION_FIRE;
		if (iname.contains("fire")) return Enchantment.FIRE_ASPECT;
		if (iname.contains("exp") && iname.contains("prot")) return Enchantment.PROTECTION_EXPLOSIONS;
		if (iname.contains("blast") && iname.contains("prot")) return Enchantment.PROTECTION_EXPLOSIONS;
		if (iname.contains("arrow") && iname.contains("prot")) return Enchantment.PROTECTION_PROJECTILE;
		if (iname.contains("proj") && iname.contains("prot")) return Enchantment.PROTECTION_PROJECTILE;
		if (iname.contains("respiration")) return Enchantment.OXYGEN;
		if (iname.contains("fall")) return Enchantment.PROTECTION_FALL;
		if (iname.contains("prot")) return Enchantment.PROTECTION_ENVIRONMENTAL;
		if (iname.contains("respiration")) return Enchantment.OXYGEN;
		if (iname.contains("oxygen")) return Enchantment.OXYGEN;
		if (iname.contains("aqua")) return Enchantment.WATER_WORKER;
		if (iname.contains("arth")) return Enchantment.DAMAGE_ARTHROPODS;
		if (iname.contains("knockback")) return Enchantment.KNOCKBACK;
		if (iname.contains("loot")) return Enchantment.LOOT_BONUS_MOBS;
		if (iname.contains("lootmobs")) return Enchantment.LOOT_BONUS_MOBS;
		if (iname.contains("fortune")) return Enchantment.LOOT_BONUS_BLOCKS;
		if (iname.contains("lootblocks")) return Enchantment.LOOT_BONUS_BLOCKS;
		if (iname.contains("dig")) return Enchantment.DIG_SPEED;
		if (iname.contains("eff")) return Enchantment.DIG_SPEED;
		if (iname.contains("silk")) return Enchantment.SILK_TOUCH;
		if (iname.contains("flame")) return Enchantment.ARROW_FIRE;
		if (iname.contains("power")) return Enchantment.ARROW_DAMAGE;
		if (iname.contains("punch")) return Enchantment.ARROW_KNOCKBACK;
		if (iname.contains("inf")) return Enchantment.ARROW_INFINITE;
		if (iname.contains("unbreaking")) return Enchantment.DURABILITY;
		if (iname.contains("dura")) return Enchantment.DURABILITY;
		if (iname.contains("strider")) return Enchantment.DEPTH_STRIDER;
		if (iname.contains("thorn")) return Enchantment.THORNS;
        return null;
	}

	public static String getCommonNameByEnchantment(Enchantment enc){
		if (      enc == Enchantment.PROTECTION_ENVIRONMENTAL ) return "Protection";
		else if ( enc == Enchantment.PROTECTION_FIRE ) return "Fire Protection";
		else if ( enc == Enchantment.PROTECTION_FALL ) return "Fall Protection";
		else if ( enc == Enchantment.PROTECTION_EXPLOSIONS ) return "Blast Protection";
		else if ( enc == Enchantment.PROTECTION_PROJECTILE ) return "Projectile Protection";
		else if ( enc == Enchantment.OXYGEN ) return "Respiration";
		else if ( enc == Enchantment.WATER_WORKER ) return "Aqua Affinity";
		else if ( enc == Enchantment.DAMAGE_ALL ) return "Sharp";
		else if ( enc == Enchantment.DAMAGE_UNDEAD ) return "Smite";
		else if ( enc == Enchantment.DAMAGE_ARTHROPODS ) return "Bane of Arthropods";
		else if ( enc == Enchantment.KNOCKBACK ) return "Knockback";
		else if ( enc == Enchantment.FIRE_ASPECT ) return "Fire Aspect";
		else if ( enc == Enchantment.LOOT_BONUS_MOBS ) return "Looting";
		else if ( enc == Enchantment.DIG_SPEED ) return "Efficiency";
		else if ( enc == Enchantment.SILK_TOUCH ) return "Silk Touch";
		else if ( enc == Enchantment.DURABILITY ) return "Unbreaking";
		else if ( enc == Enchantment.LOOT_BONUS_BLOCKS ) return "Fortune";
		else if ( enc == Enchantment.ARROW_DAMAGE ) return "Power";
		else if ( enc == Enchantment.ARROW_KNOCKBACK ) return "Punch";
		else if ( enc == Enchantment.ARROW_FIRE ) return "Flame";
		else if ( enc == Enchantment.ARROW_INFINITE ) return "Infinity";
		else if ( enc == Enchantment.DEPTH_STRIDER ) return "Depth Strider"; 
		else if ( enc == Enchantment.THORNS ) return "Thorns";
        return enc.getName();
	}

	static final Map<Material,Armor> armors;
	static {
		armors = new EnumMap<>(Material.class);

	    armors.put(Material.SKULL_ITEM, new Armor(ArmorType.HELM, ArmorLevel.DISGUISE));
		armors.put(Material.WOOL, new Armor(ArmorType.HELM, ArmorLevel.WOOL));
		armors.put(Material.LEATHER_HELMET, new Armor(ArmorType.HELM, ArmorLevel.LEATHER));
		armors.put(Material.IRON_HELMET, new Armor(ArmorType.HELM, ArmorLevel.IRON));
		armors.put(Material.GOLD_HELMET, new Armor(ArmorType.HELM, ArmorLevel.GOLD));
		armors.put(Material.DIAMOND_HELMET, new Armor(ArmorType.HELM, ArmorLevel.DIAMOND));
		armors.put(Material.CHAINMAIL_HELMET, new Armor(ArmorType.HELM, ArmorLevel.CHAINMAIL));

		armors.put(Material.LEATHER_CHESTPLATE, new Armor(ArmorType.CHEST,ArmorLevel.LEATHER));
		armors.put(Material.IRON_CHESTPLATE, new Armor(ArmorType.CHEST,ArmorLevel.IRON));
		armors.put(Material.GOLD_CHESTPLATE, new Armor(ArmorType.CHEST,ArmorLevel.GOLD));
		armors.put(Material.DIAMOND_CHESTPLATE, new Armor(ArmorType.CHEST,ArmorLevel.DIAMOND));
		armors.put(Material.CHAINMAIL_CHESTPLATE, new Armor(ArmorType.CHEST,ArmorLevel.CHAINMAIL));

		armors.put(Material.LEATHER_LEGGINGS, new Armor(ArmorType.LEGGINGS,ArmorLevel.LEATHER));
		armors.put(Material.IRON_LEGGINGS, new Armor(ArmorType.LEGGINGS,ArmorLevel.IRON));
		armors.put(Material.GOLD_LEGGINGS, new Armor(ArmorType.LEGGINGS,ArmorLevel.GOLD));
		armors.put(Material.DIAMOND_LEGGINGS, new Armor(ArmorType.LEGGINGS,ArmorLevel.DIAMOND));
		armors.put(Material.CHAINMAIL_LEGGINGS, new Armor(ArmorType.LEGGINGS,ArmorLevel.CHAINMAIL));

		armors.put(Material.LEATHER_BOOTS, new Armor(ArmorType.BOOTS,ArmorLevel.LEATHER));
		armors.put(Material.IRON_BOOTS, new Armor(ArmorType.BOOTS,ArmorLevel.IRON));
		armors.put(Material.GOLD_BOOTS, new Armor(ArmorType.BOOTS,ArmorLevel.GOLD));
		armors.put(Material.DIAMOND_BOOTS, new Armor(ArmorType.BOOTS,ArmorLevel.DIAMOND));
		armors.put(Material.CHAINMAIL_BOOTS, new Armor(ArmorType.BOOTS,ArmorLevel.CHAINMAIL));
	}

	public static int arrowCount(Player p) {
		return getItemAmount(p.getInventory().getContents(), new ItemStack(Material.ARROW,1));
	}

    public static boolean isEnderChest(InventoryType type) {
        return type == InventoryType.ENDER_CHEST;
    }

	public static int getItemAmountFromInventory(Inventory inv, ItemStack is) {
		return getItemAmount(inv.getContents(), is);
	}

	public static boolean isArmor(ItemStack is) {
		return armors.get(is.getType()) != null;
	}
	public static boolean isRealArmor(ItemStack is) {
		return armors.get(is.getType()) != null && is.getType()!= Material.WOOL;
	}

	public static boolean hasArmor(Player p) {
		PlayerInventory pi= p.getInventory();
		return(	(pi.getBoots() != null && pi.getBoots().getType() != Material.AIR) &&
				(pi.getHelmet() != null && pi.getBoots().getType() != Material.AIR) &&
				(pi.getLeggings() != null && pi.getBoots().getType() != Material.AIR) &&
				(pi.getChestplate() != null && pi.getBoots().getType() != Material.AIR) );
	}

	public static ArmorLevel hasArmorSet(List<ItemStack> inv) {
		ArmorLevel armorSet[] = new ArmorLevel[4];
		for (ItemStack is: inv){
			Armor a = armors.get(is.getType());
			if (a == null)
				continue;
			switch (a.type){
			case BOOTS: armorSet[0] = a.level; break;
			case LEGGINGS: armorSet[1] = a.level; break;
			case CHEST: armorSet[2] = a.level; break;
			case HELM: armorSet[3] = a.level; break;
			}
		}
		ArmorLevel lvl = null;
		for (ArmorLevel a: armorSet){
			if (lvl == null)
				lvl = a;
			else if (lvl != a)
				return null;
		}
		return lvl;
	}

	public static int getItemAmount(ItemStack[] items, ItemStack is){
		int count = 0;
		for (ItemStack item : items) {
			if (item == null) {
				continue;}
			if (item.getType() == is.getType() && ((item.getDurability() == is.getDurability() || item.getDurability() == -1) )) {
				count += item.getAmount();
			}
		}
		return count;
	}

	/**
	 * Return a item stack from a given string
	 * @param itemStr ItemStack
	 * @return ItemStack
	 */
	public static ItemStack getItemStack(String itemStr) {
		if (itemStr == null || itemStr.isEmpty())
			return null;
		itemStr = itemStr.replace(" ", "_");
		itemStr = itemStr.replace(";", ":");
		itemStr = itemStr.toLowerCase();

		String split[] = itemStr.split(":");
		short dataValue = 0;
		if (split.length > 1){
			if (isInt(split[1])){
				int i = Integer.valueOf(split[1]);
				dataValue = (short) i;
				itemStr = split[0];
			}
		}
		Material mat = Material.matchMaterial(itemStr);
		if (DEBUG) Log.info(mat +"   " + itemStr +"   " + dataValue);
		if (mat != null && mat != Material.AIR) {
			return new ItemStack(mat, 1, dataValue);
		}
        if (itemStr.equalsIgnoreCase("steak")){
        	return new ItemStack(Material.COOKED_BEEF, 1);
        } else if (itemStr.equalsIgnoreCase("chicken")){
        	return new ItemStack(Material.COOKED_CHICKEN, 1);
        }
		itemStr = itemStr.toUpperCase();
		for (Material m : Material.values()){
			String itemName = m.name();
			int index = itemName.indexOf(itemStr,0);
			if (index != -1 && index == 0){
				if (DEBUG) Log.info(m +"   " + itemStr +"   " + dataValue);
				return new ItemStack(m, 1, dataValue);
			}
		}
		return null;
	}

    public static boolean isInt(String i) {try {Integer.parseInt(i);return true;} catch (Exception e) {return false;}}

	public static boolean isFloat(String i){try{Float.parseFloat(i);return true;} catch (Exception e){return false;}}

//	/// This allows for abbreviations to work, useful for sign etc
//	public static int getMaterialID(String name) {
//		name = name.toUpperCase();
//		/// First try just getting it from the Material Name
//		Material mat = Material.getMaterial(name);
//		if (mat != null)
//			return mat.getId();
//		/// Might be an abbreviation, or a more complicated
//		int temp = Integer.MAX_VALUE;
//		name = name.replaceAll("\\s+", "").replaceAll("_", "");
//		for (Material m : Material.values()) {
//			if (m.name().replaceAll("_", "").startsWith(name)) {
//				if (m.name().length() < temp) {
//					mat = m;
//					temp = m.name().length();
//				}
//			}
//		}
//		return mat != null ? mat.getId() : -1;
//	}

	public static boolean hasItem(Player p, ItemStack item) {
		PlayerInventory inv = p.getInventory();
		for (ItemStack is : inv.getContents()){
			if (is != null && is.getType() == item.getType()){
				return true;}
		}
		for (ItemStack is : inv.getArmorContents()){
			if (is != null && is.getType() == item.getType()){
				return true;}
		}
		return false;
	}

    public static boolean hasAllItems(Player p, List<ItemStack> items) {
        for (ItemStack is : items){
            if (!hasItem(p,is))
                return false;
        }
        return true;
    }

	public static boolean hasAnyItem(Player p) {
		PlayerInventory inv = p.getInventory();
		for (ItemStack is : inv.getContents()){
			if (is != null && is.getType() != Material.AIR){
				return true;}
		}
		for (ItemStack is : inv.getArmorContents()){
			if (is != null && is.getType() != Material.AIR){
				return true;}
		}
		return false;
	}

	public static void addItemToInventory(Player player, ItemStack itemStack) {
		addItemToInventory(player,itemStack,itemStack.getAmount(),true,false,null);
	}

	public static void addItemToInventory(Player player, ItemStack itemStack, int stockAmount, boolean update) {
		addItemToInventory(player,itemStack,stockAmount,update,false,null);
	}

	public static void addItemsToInventory(Player p, List<ItemStack> items, boolean ignoreCustomHelmet) {
		addItemsToInventory(p,items, ignoreCustomHelmet,null);
	}

	public static void addItemsToInventory(Player p, List<ItemStack> items, boolean ignoreCustomHelmet, Color color) {
		if (items == null)
			return;
		for (ItemStack is : items){
			InventoryUtil.addItemToInventory(p, is.clone(), is.getAmount(), false, ignoreCustomHelmet, color);
		}
		try { p.updateInventory(); } catch (Exception e){
            if (!Defaults.DEBUG_VIRTUAL)
                Log.printStackTrace(e);
        }
	}

	public static void addItemToInventory(Player player, ItemStack itemStack, int stockAmount,
			                                                        boolean update, boolean ignoreCustomHelmet) {
		addItemToInventory(player,itemStack,stockAmount,update,ignoreCustomHelmet,null);
	}

	public static void addItemToInventory(Player player, ItemStack itemStack, int stockAmount,
			boolean update, boolean ignoreCustomHelmet, Color color) {
		PlayerInventory inv = player.getInventory();
		Material itemType =itemStack.getType();
		if (armors.containsKey(itemType)){
			addArmorToInventory(inv,itemStack,stockAmount,ignoreCustomHelmet, color);
		} else {
			addItemToInventory(inv, itemStack,stockAmount);
		}
		if (update)
			try { player.updateInventory(); } catch (Exception e){
                if (!Defaults.DEBUG_VIRTUAL)
                    Log.printStackTrace(e);
            }
	}


	private static void addArmorToInventory(PlayerInventory inv,
			ItemStack itemStack, int stockAmount, boolean ignoreCustomHelmet, Color color) {
		Material itemType =itemStack.getType();
		boolean isHelmet = false; // armor.get(itemType).type == ArmorType.HELM;
                try {
                    isHelmet = armors.get(itemType).type == ArmorType.HELM;
                } catch (NullPointerException handled) {
                    isHelmet = false;
                }

		Armor a = armors.get(itemType);
		final ItemStack oldArmor = getArmorSlot(inv,a.type);
		boolean empty = (oldArmor == null || oldArmor.getType() == Material.AIR);
		boolean better = empty || armorSlotBetter(armors.get(oldArmor.getType()), a);

		if (color != null && a.level == ArmorLevel.LEATHER){
			setColor(itemStack,color);
		}
		if (empty || better){
			switch (armors.get(itemType).type){
			case HELM:
				if (empty || !ignoreCustomHelmet)
					inv.setHelmet(itemStack);
				break;
			case CHEST: inv.setChestplate(itemStack); break;
			case LEGGINGS: inv.setLeggings(itemStack); break;
			case BOOTS: inv.setBoots(itemStack); break;
			}
		}
		if (!empty){
			if (better && !(isHelmet && ignoreCustomHelmet)){
				addItemToInventory(inv, oldArmor,oldArmor.getAmount());
			} else {
				addItemToInventory(inv, itemStack,stockAmount);
			}
		}
	}

	public static void setColor(ItemStack itemStack, Color color) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && itemStack.getItemMeta() instanceof LeatherArmorMeta){
            Color bukkitColor = Color.fromRGB(color.getRed(), color.getGreen(), color.getBlue());
            LeatherArmorMeta lam = (LeatherArmorMeta) itemStack.getItemMeta();
            lam.setColor(bukkitColor);
            itemStack.setItemMeta(lam);
        }
    }
	
	public static int first(Inventory inv, ItemStack is1) {
		if (is1 == null) {
			return -1;
		}
		ItemStack[] inventory = inv.getContents();
		for (int i = 0; i < inventory.length; i++) {
			ItemStack is2 = inventory[i];
			if (is2 == null) continue;
			if (is1.getType() == is2.getType() && is1.getDurability() == is2.getDurability()) {
				return i;
			}
		}
		return -1;
	}

	public static HashMap<Integer, ItemStack> removeItems(Inventory inv, List<ItemStack> items) {
		return removeItem(inv, items.toArray(new ItemStack[items.size()]));
	}

	public static HashMap<Integer, ItemStack> removeItems(PlayerInventory inv, ItemStack... items) {
		HashMap<Integer,ItemStack> leftover = removeItem(inv,items);
		if (leftover.isEmpty())
			return leftover;
		for (ItemStack is1: items){
			ItemStack is2 = inv.getBoots();
			if (is2 != null && is1.getType() == is2.getType() && is1.getDurability() == is2.getDurability()){
				inv.setBoots(null);
				continue;
			}
			is2 = inv.getLeggings();
			if (is2 != null && is1.getType() == is2.getType() && is1.getDurability() == is2.getDurability()){
				inv.setLeggings(null);
				continue;
			}
			is2 = inv.getChestplate();
			if (is2 != null && is1.getType() == is2.getType() && is1.getDurability() == is2.getDurability()){
				inv.setChestplate(null);
				continue;
			}
			is2 = inv.getHelmet();
			if (is2 != null && is1.getType() == is2.getType() && is1.getDurability() == is2.getDurability()){
				inv.setHelmet(null);
			}
		}
		/// TODO technically this is not correct as removing the armour slots should also decrease the leftover
		return leftover;
	}

	/**
	 * This is nearly a direct copy of the removeItem from CraftBukkit
	 * The difference is my ItemStack == ItemStack comparison (found in first())
	 * there I change it to go by itemid and datavalue
	 * as opposed to itemid and quantity
	 * @param inv the inventory
	 * @param items array of items
	 * @return HashMap Integer ItemStack
	 */
	public static HashMap<Integer, ItemStack> removeItem(Inventory inv, ItemStack... items) {
		HashMap<Integer, ItemStack> leftover = new HashMap<>();

		for (int i = 0; i < items.length; i++) {
			ItemStack item = items[i];
			int toDelete = item.getAmount();

			while (true) {
				int first = first(inv, item);
				
				if (first == -1) {
					item.setAmount(toDelete);
					leftover.put(i, item);
					break;
				}
                ItemStack itemStack = inv.getItem(first);
                int amount = itemStack.getAmount();

                if (amount <= toDelete) {
                	toDelete -= amount;
                	
                	inv.setItem(first, null);
                } 
                else {
                	itemStack.setAmount(amount - toDelete);
                	inv.setItem(first, itemStack);
                	toDelete = 0;
                }
                
				if (toDelete <= 0) {
					break;
				}
			}
		}
		return leftover;
	}
	private static boolean armorSlotBetter(Armor oldArmor, Armor newArmor) {
        return !(oldArmor == null || newArmor == null) && oldArmor.level.ordinal() < newArmor.level.ordinal();
    }

	private static ItemStack getArmorSlot(PlayerInventory inv, ArmorType armorType) {
		switch (armorType){
		case HELM: return inv.getHelmet();
		case CHEST: return inv.getChestplate();
		case LEGGINGS: return inv.getLeggings();
		case BOOTS:return inv.getBoots();
		}
		return null;
	}

	public static void addItemToInventory(Inventory inv, ItemStack is, int left){
	    
		if (is == null || is.getType() == Material.AIR)
			return;
		if (Defaults.ITEMS_IGNORE_STACKSIZE){
			inv.addItem(is);
			return;
		}
		int maxStackSize = is.getType().getMaxStackSize();
		ItemStack is2 = is.clone(); /// we are modifying the amounts, so lets clone
		if(left <= maxStackSize){
			is2.setAmount(left);
			inv.addItem(is2);
			return;
		}
		if(maxStackSize != 64){
			ArrayList<ItemStack> items = new ArrayList<>();
			for (int i = 0; i < Math.ceil((double)left / maxStackSize); i++) {
				if (left < maxStackSize) {
					is2.setAmount(left);
					items.add(is2);
					return;
				}
                is2.setAmount(maxStackSize);
                items.add(is2);
			}
			Object[] iArray = items.toArray();
			for(Object o : iArray){
				inv.addItem((ItemStack) o);
			}
		}
		else inv.addItem(is);
	}


	public static void clearInventory(Player p) {
		if(Defaults.DEBUG_STORAGE) Log.info("Clearing inventory of " + p.getName() +" o=" +
				p.isOnline() +", d="+ p.isDead() +"   inv=" + p.getInventory());

		PlayerInventory inv = p.getInventory();
		p.closeInventory();
		if (inv != null){
			inv.clear();
			inv.setArmorContents(null);
			inv.setItemInMainHand(null);
			inv.setItemInOffHand( null );
		}
		p.updateInventory();
	}

	public static void clearInventory(Player p, boolean skipHead) {
		if (!skipHead){
			clearInventory(p);
			return;
		}
		if(Defaults.DEBUG_STORAGE) Log.info("Clearing inventory of " + p.getName());
		
		PlayerInventory inv = p.getInventory();
		p.closeInventory();
		
		if (inv != null){
			inv.clear();
			if ( inv.getHelmet() != null && isRealArmor(inv.getHelmet()) ) 
			    inv.setHelmet(null);
			
			inv.setBoots(null);
			inv.setChestplate(null);
			inv.setLeggings(null);
            inv.setItemInMainHand(null);
            inv.setItemInOffHand( null );
		}
		p.updateInventory();

	}

	public static String getCommonName(ItemStack is) {
		Material mat = is.getType();
		int datavalue = is.getDurability();
		if (datavalue > 0){
			return mat.toString() + ":" + datavalue;
		}
		return mat.toString();
	}


	public static boolean isItem(String str){
		return parseItem(str) != null;	
	}

	private static final Pattern PATTERN_LORE =
			Pattern.compile("lore= ?\"([^\"]*)\"",Pattern.CASE_INSENSITIVE); //The pattern for matching lore
	private static final Pattern PATTERN_OWNER =
			Pattern.compile("owner= ?\"([^\"]*)\"",Pattern.CASE_INSENSITIVE); //The pattern for matching lore
	private static final Pattern PATTERN_DISPLAY_NAME =
			Pattern.compile("displayName= ?\"([^\"]*)\"",Pattern.CASE_INSENSITIVE); //The pattern for Display name
	private static final Pattern PATTERN_COLOR =
			Pattern.compile("color= ?([0-9]+),([0-9]+),([0-9]+)",Pattern.CASE_INSENSITIVE); //The pattern for matching lore
	private static final Pattern PATTERN_POSITION =
			Pattern.compile("position= ?\"([^\"]*)\"",Pattern.CASE_INSENSITIVE); //The pattern for matching position

	public static ItemStack parseItem(String str)  {
	    
		/// items in yaml get stored like this {leather_chest=fireprot:5 1}
		/// so need to remove the {} and the first '='
		if (str.contains("{"))
			str = str.replaceFirst("=", " ");
		str = str.replaceAll("[}{]", "");

		if (DEBUG) Log.info("item=" + str);

		/// Parse Lore (thanks to Netherfoam)
		List<String> lore = parseLore(str);
		if(lore != null){ //We have lore, so strip it.
			str = PATTERN_LORE.matcher(str).replaceFirst("");}
		String ownerName = parseOwner(str);
		if(ownerName != null){ //We have lore, so strip it.
			str = PATTERN_OWNER.matcher(str).replaceFirst("");}
		String displayName = parseDisplayName(str);
		if(displayName != null){ //We have lore, so strip it.
			str = PATTERN_DISPLAY_NAME.matcher(str).replaceFirst("");}
		Color c = parseColor(str);
		if (c != null){ /// we have color, so strip it
			str = PATTERN_COLOR.matcher(str).replaceFirst("");}
		Integer pos = parsePosition(str);
		if (pos != null){ /// we have position, so strip it
			str = PATTERN_POSITION.matcher(str).replaceFirst("");}
		ItemStack is;
		String split[] = str.split(" +");
		is = InventoryUtil.getItemStack(split[0].trim());
		if (is == null)
			return null;
		int amt;
		if (split.length > 1){
			try {
				amt = Integer.valueOf(split[split.length-1]);
			} catch (Exception e){
				amt = 1;
			}
		} else {
			amt = 1;
		}
		is.setAmount(amt);
		if (lore != null && !lore.isEmpty())
			setLore(is,lore);
		if (c!=null)
			setColor(is, c);
		if (displayName != null)
			setDisplayName(is,displayName);
		if (ownerName != null)
			setOwnerName(is,ownerName);

		for (int i = 1; i < split.length-1;i++){
            if (Defaults.TESTSERVER) continue;
			EnchantmentWithLevel ewl = getEnchantment(split[i].trim());
            if (ewl == null){
				throw new IllegalArgumentException(" enchantment " + split[i].trim() +" does not exist");
			}
			try {
				is.addUnsafeEnchantment(ewl.e, ewl.lvl);
			} catch (IllegalArgumentException iae){
				Log.warn(ewl+" can not be applied to the item " + str);
			}
		}
		return is;
	}
	public static void setLore(ItemStack itemStack, List<String> lore) {
        ItemMeta meta = itemStack.getItemMeta();
        if(meta != null){
            meta.setLore(lore);
            itemStack.setItemMeta(meta);
        }
    }
	public static void setDisplayName(ItemStack itemStack, String displayName) {
        ItemMeta meta = itemStack.getItemMeta();
        if(meta != null){
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',displayName));
            itemStack.setItemMeta(meta);
        }
    }
	public static void setOwnerName(ItemStack itemStack, String ownerName) {
        ItemMeta im = itemStack.getItemMeta();
        if (im != null && im instanceof SkullMeta){
            SkullMeta sm = (SkullMeta) im;
            sm.setOwner(ownerName);
            itemStack.setItemMeta(sm);
        }
    }
	public static Integer parsePosition(String str){
		Matcher m = PATTERN_POSITION.matcher(str);
		if (!m.find())
			return null;
		return Integer.valueOf(m.group(1));
	}

	public static Color parseColor(String str){
		Matcher m = PATTERN_COLOR.matcher(str);
		if (!m.find())
			return null;
		return Color.fromRGB( Integer.valueOf(m.group(1)),Integer.valueOf(m.group(2)),Integer.valueOf(m.group(3)));
	}

	public static String parseOwner(String str){
		Matcher matcher = PATTERN_OWNER.matcher(str);
		if(!matcher.find()){
			return null;}
		return matcher.group(1);
	}

	public static String parseDisplayName(String str){
		Matcher matcher = PATTERN_DISPLAY_NAME.matcher(str);
		if(!matcher.find()){
			return null;}
		return MessageUtil.colorChat(matcher.group(1));
	}

	public static LinkedList<String> parseLore(String str){
		try{
			Matcher matcher = PATTERN_LORE.matcher(str);
			if(matcher.find()){
                String part = ChatColor.translateAlternateColorCodes('&', matcher.group(1));
				String[] lines = part.split("([;\\n]|\\\\n)");
				if(DEBUG) Log.info(Arrays.toString(lines));
				LinkedList<String> lore = new LinkedList<>();
                Collections.addAll(lore, lines);
				return lore;
			}
		}
		catch(Exception e){
			Log.printStackTrace(e); 
		}
		return null;
	}

	public static EnchantmentWithLevel getEnchantment(String str) {
        if (str.equalsIgnoreCase("all")){
			return new EnchantmentWithLevel(true);
		}
		Enchantment e = null;
		str = str.replaceAll(",", ":");
		int index = str.indexOf(':');
		index = (index != -1 ? index : -1);
		int lvl = -1;
		if (index != -1){
			try {lvl = Integer.parseInt(str.substring(index + 1)); } catch (Exception err){/*do nothing*/}
			str = str.substring(0,index);
		}

		try {e = Enchantment.getById(Integer.valueOf(str));} catch (Exception err){/*do nothing*/}
		if (e == null)
			e = Enchantment.getByName(str);
		if (e == null)
			e = getEnchantmentByCommonName(str);
		if (e == null)
			return null;
		EnchantmentWithLevel ewl = new EnchantmentWithLevel();
		ewl.e = e;
		if (lvl < e.getStartLevel()){lvl = e.getStartLevel();}
		if (!Defaults.ITEMS_UNSAFE_ENCHANTMENTS &&
				lvl > e.getMaxLevel()){lvl = e.getMaxLevel();}
		ewl.lvl = lvl;
		return ewl;
	}

	public boolean addEnchantments(ItemStack is, String[] args) {
		Map<Enchantment,Integer> encs = new HashMap<>();
		for (String s : args){
			EnchantmentWithLevel ewl = getEnchantment(s);
			if (ewl != null){
				if (ewl.all){
					return addAllEnchantments(is);}
				encs.put(ewl.e, ewl.lvl);
			}
		}
		addEnchantments(is,encs);
		return true;
	}

	public void addEnchantments(ItemStack is,Map<Enchantment, Integer> enchantments) {
		for (Enchantment e: enchantments.keySet()){
			is.addUnsafeEnchantment(e, enchantments.get(e));
		}
	}

	public boolean addAllEnchantments(ItemStack is) {
		for (Enchantment enc : Enchantment.values()){
			if (enc.canEnchantItem(is)){
				is.addUnsafeEnchantment(enc, enc.getMaxLevel());
			}
		}
		return true;
	}

	/**
	 * For Serializing an item or printing
	 * @param is ItemStack
	 * @return String
	 */
	public static String getItemString(ItemStack is) {
		StringBuilder sb = new StringBuilder();
		if (is.getDurability() > 0){
			sb.append(is.getType().toString()).append(":").append(is.getDurability()).append(" ");
		} else {
			sb.append(is.getType().toString()).append(" ");
		}

		Map<Enchantment,Integer> encs = is.getEnchantments();
		for (Enchantment enc : encs.keySet()){
			sb.append(enc.getName()).append(":").append(encs.get(enc)).append(" ");
		}
		ItemMeta meta = is.getItemMeta();
		if ( meta != null ) {
    		List<String> lore = meta.getLore();
    		if (lore != null && !lore.isEmpty()){
                sb.append("lore=\"").append(StringUtils.join(lore, "\\n")).append("\" ");
    		}
    		if (meta instanceof LeatherArmorMeta){
                LeatherArmorMeta lam = (LeatherArmorMeta) meta;
                Color color = Color.fromRGB(lam.getColor().getRed(), lam.getColor().getGreen(), lam.getColor().getBlue());
                if (color!=null)
                    sb.append("color=\"").append(color.getRed()).append(",").append(color.getGreen()).append(",").append(color.getBlue()).append("\" ");
    		}

    		String op = meta.getDisplayName();
    		if (op != null && !op.isEmpty())
    			sb.append("displayName=\"").append(op).append("\" ");
    		
    		if ( meta instanceof SkullMeta ) {
    	        op = ((SkullMeta)meta).getOwner();
    	        if (op != null && !op.isEmpty())
    	            sb.append("ownerName=\"").append(op).append("\" ");
    		}
		}
		sb.append(is.getAmount());
		return sb.toString();
	}

	public static boolean isColorable(ItemStack item){
		Armor a = armors.get(item.getType());
		return a != null && a.level == ArmorLevel.LEATHER;
	}

	public static boolean hasEnchantedItem(Player p) {
		PlayerInventory inv = p.getInventory();
		for (ItemStack is : inv.getContents()){
			if (is != null && is.getType() != Material.AIR ){
				Map<Enchantment,Integer> enc = is.getEnchantments();
				if (enc != null && !enc.isEmpty())
					return true;
			}
		}
		for (ItemStack is : inv.getArmorContents()){
			if (is != null && is.getType() != Material.AIR){
				Map<Enchantment,Integer> enc = is.getEnchantments();
				if (enc != null && !enc.isEmpty())
					return true;
			}
		}
		return false;
	}

	public static boolean sameMaterial(ArmorLevel lvl, ItemStack is) {
		Armor a = armors.get(is.getType());
        return a != null && a.level == lvl;
    }

	public static ItemStack getWool(int color) {
		return new ItemStack(Material.WOOL,1,(short) color);
	}

	public static void printInventory(Player p) {
		PlayerInventory pi = p.getInventory();
		Log.info("Inventory of "+p.getName());
		for (ItemStack is: pi.getContents()){
			if (is != null && is.getType() != Material.AIR){
				Log.info(getCommonName(is) +"  " + is.getAmount());}
		}
		for (ItemStack is: pi.getArmorContents()){
			if (is != null && is.getType() != Material.AIR){
				Log.info(getCommonName(is) +"  " + is.getAmount());}
		}
	}

    public static boolean sameItem(ItemStack item1, ItemStack item2) {

        if ( item1 == null && item2 == null ) return true;
        if ( item1 == null || item2 == null ) return false;
             
        Material i1 = item1.getType();
        Material i2 = item2.getType();
        
        if ( i1 != i2 ) return false;
        if ( i1 == Material.AIR && i2 == Material.AIR ) return true;
        
        if ( item1.getDurability() != item2.getDurability() ) return false;
        if ( item1.getAmount() != item2.getAmount() ) return false;

        Map<Enchantment, Integer> e1 = item1.getEnchantments();
        Map<Enchantment, Integer> e2 = item1.getEnchantments();
        
        if ( e1.isEmpty() && e2.isEmpty() ) return true;
        if ( e1.equals( e2 ) ) return true;
        
        return false;
    }

    public static boolean sameItems(List<ItemStack> items, PlayerInventory inv, boolean woolTeams) {
		ItemStack[] contents = inv.getContents();
		ItemStack[] armor = inv.getArmorContents();
		/// This is a basic check to make sure we have the same number of items, and same total durability
		/// Even with the 3 loops b/c there is no creation or sorting this is orders of magnitude faster
		/// and takes almost no time.
		int nitems1 = 0, nitems2 = 0;
		int dura1 = 0, dura2 = 0;

		for (ItemStack is: items){
			if (is == null || is.getType() == Material.AIR)
				continue;
			nitems1 += is.getAmount();
			dura1 += is.getDurability()*is.getAmount();
		}
		for (ItemStack is: contents){
			if (is == null || is.getType() == Material.AIR)
				continue;
			nitems2 += is.getAmount();
			dura2 += is.getDurability()*is.getAmount();
		}
		if (!woolTeams){
			for (ItemStack is: armor){
				if (is == null || is.getType() == Material.AIR)
					continue;
				nitems2 += is.getAmount();
				dura2 += is.getDurability()*is.getAmount();
			}
		} else {
			ItemStack is = inv.getHelmet();
			if (is != null && is.getType() != Material.AIR && InventoryUtil.isRealArmor(is)){
				nitems2 += is.getAmount();
				dura2 += is.getDurability()*is.getAmount();
			}
			is = inv.getBoots();
			if (is != null && is.getType() != Material.AIR){
				nitems2 += is.getAmount();
				dura2 += is.getDurability()*is.getAmount();
			}
			is = inv.getLeggings();
			if (is != null && is.getType() != Material.AIR){
				nitems2 += is.getAmount();
				dura2 += is.getDurability()*is.getAmount();
			}
			is = inv.getChestplate();
			if (is != null && is.getType() != Material.AIR){
				nitems2 += is.getAmount();
				dura2 += is.getDurability()*is.getAmount();
			}
		}
		if (DEBUG) Log.info("nitems1  " + nitems1 +":" + nitems2+"      " + dura1 +"  : " + dura2);
		if (nitems1 != nitems2 || dura1 != dura2)
			return false;
//        List<ItemStack> pitems = new ArrayList<ItemStack>();
//
//		/// Now that the basic check is over, the more intensive one starts
//		//// I could check size right now if it werent for "air" and null blocks in inventories
//		List<ItemStack> pitems = new ArrayList<ItemStack>();
//		pitems.addAll(Arrays.asList(contents));
//		if (woolTeams){ /// ignore helmet maybe
//			if (inv.getHelmet() != null && InventoryUtil.isRealArmor(inv.getHelmet())) pitems.add(inv.getHelmet());
//
//			if (inv.getBoots() != null) pitems.add(inv.getBoots());
//			if (inv.getLeggings() != null) pitems.add(inv.getLeggings());
//			if (inv.getChestplate() != null) pitems.add(inv.getChestplate());
//		} else {
//			pitems.addAll(Arrays.asList(armor));
//		}
//		List<ItemStack> stacks = new ArrayList<ItemStack>();
//		for (ItemStack is: items){
//			if (is == null || is.getType() == Material.AIR)
//				continue;
//            final int max = 64;
//			if (DEBUG)Log.info(" iss   " + is.getAmount() +"   " + is.getMaxStackSize() +"    " + is +" max="+max);
//            if (is.getAmount() > max){
//				is = is.clone();
//				while (is.getAmount() > max){
//					is.setAmount(is.getAmount() - max);
//					ItemStack is2 = new ItemStack(is);
//					is2.setAmount(max);
//					stacks.add(is2);
//				}
//			}
//			stacks.add(is);
//		}
//		items = stacks;
//		Collections.sort(items, new ItemComparator());
//		Collections.sort(pitems, new ItemComparator());
//		int idx = 0;
//		ItemStack is1, is2;
//
//		while (idx< items.size() && idx<pitems.size()){
//			is1 = items.get(idx);
//			is2 = pitems.get(idx);
//			if (DEBUG) Log.info(idx  +" : " + is1 +"  " + is2);
//			if ((is1==null || is1.getType() == Material.AIR) && (is2 == null || is2.getType() == Material.AIR))
//				return true;
//			if ((is1==null || is1.getType() == Material.AIR) || (is2 == null || is2.getType() == Material.AIR))
//				return false;
//			/// Alright, now that we dont have to worry about null or air
//			if (!is1.equals(is2))
//				return false;
//			idx++;
//		}
//		/// Arrays are similar up until the smallest array
//		/// If any array has more elements that are not null, then they are not equal
//		for (int i=idx;i<items.size();i++){
//			is1 = items.get(i);
//			if (is1 != null && is1.getType() != Material.AIR)
//				return false;
//		}
//		for (int i=idx;i<pitems.size();i++){
//			is2 = pitems.get(i);
//			if (is2 != null && is2.getType() != Material.AIR)
//				return false;
//		}
		return true;
	}

	public static void addToInventory(Player p, PInv pinv) {
		try{
			PlayerInventory inv = p.getPlayer().getInventory();
			for (ArmorType armor : ArmorType.values()){
				final int ord = armor.ordinal();
				if (ord >= pinv.armor.length)
					break;
				final ItemStack newArmor = pinv.armor[ord];
				if (newArmor == null || newArmor.getType()== Material.AIR)
					continue;
				final ItemStack oldArmor = getArmorSlot(inv,armor);
				boolean empty = (oldArmor == null || oldArmor.getType() == Material.AIR);
				if (empty) {
					switch(armor){
					case HELM: inv.setHelmet(newArmor); break;
					case CHEST: inv.setChestplate(newArmor); break;
					case LEGGINGS: inv.setLeggings(newArmor); break;
					case BOOTS: inv.setBoots(newArmor); break;
					}
				} else {
					addItemToInventory(p, newArmor, newArmor.getAmount(),false,false);
				}
			}
			inv.setContents(pinv.contents);
		} catch(Exception e){
			Log.printStackTrace(e);
		}
		try {p.getPlayer().updateInventory(); } catch (Exception e){
            Log.printStackTrace(e);
        }
	}


	public static List<ItemStack> getItemList(Player player){
		List<ItemStack> items = new ArrayList<>();
		for (ItemStack item: player.getInventory().getArmorContents()){
			if (item != null && item.getType() != Material.AIR){
				items.add(item);}
		}
		for (ItemStack item: player.getInventory().getContents()){
			if (item != null && item.getType() != Material.AIR){
				items.add(item);}
		}
		return items;
	}

	public static void dropItems(Player player) {
		Location loc = player.getLocation();
		World w = loc.getWorld();
		PlayerInventory inv = player.getInventory();
		for (ItemStack is: inv.getArmorContents()){
			if (is == null || is.getType() == Material.AIR)
				continue;
			w.dropItemNaturally(loc, is);
		}
		for (ItemStack is: inv.getContents()){
			if (is == null || is.getType() == Material.AIR)
				continue;
			w.dropItemNaturally(loc, is);
		}
	}

	public static ArrayList<ItemStack> getItemList(ConfigurationSection cs, String nodeString) {
		if (cs == null || cs.getList(nodeString) == null)
			return null;
		ArrayList<ItemStack> items = new ArrayList<>();
		try {
			String str = null;
			for (Object o : cs.getList(nodeString)){
				try {
					str = o.toString();
					ItemStack is = InventoryUtil.parseItem(str);
					if (is != null){
						items.add(is);
					} else {
						Log.err(cs.getCurrentPath() +"."+nodeString + " couldnt parse item " + str);
					}
				} catch (IllegalArgumentException e) {
					Log.err(cs.getCurrentPath() +"."+nodeString + " couldnt parse item " + str);
				} 
			}
		} catch (Exception e){
			Log.err(cs.getCurrentPath() +"."+nodeString + " could not be parsed in config.yml");
			Log.printStackTrace(e);
		}
		return items;
	}	
}
