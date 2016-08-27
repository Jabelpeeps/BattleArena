package mc.alk.arena.objects;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.objects.spawns.EntitySpawn;
import mc.alk.arena.objects.spawns.SpawnInstance;
import mc.alk.arena.util.InventoryUtil;

public class ArenaClass {
	public static final Integer DEFAULT = Integer.MAX_VALUE;
	public static final ArenaClass CHOSEN_CLASS = new ArenaClass("CHOSENCLASS","chosenClass", null, null, null);
	public static final ArenaClass SELF_CLASS = new ArenaClass("SELFCLASS","selfClass", null, null, null);

	/** Name of the Class*/
	@Getter final String name;
	/** DisplayName of the class*/
	final String displayName;
	/** Items that this class gives*/
	@Getter final List<ItemStack> items;
	/** Effects this class gives*/
	@Getter final List<PotionEffect> effects;
	/** Mobs for this class*/
	@Getter @Setter List<SpawnInstance> mobs;
	/** Name of a disguise for this class */
	@Getter @Setter String disguiseName;
	/** List of commands to run when class is given */
	@Getter @Setter List<CommandLineString> doCommands;
    /** List of permission nodes that this class has */
	@Getter List<String> permissions;
    
	@Getter boolean valid = false;

    public ArenaClass(String _name){
		this( _name, _name, new ArrayList<ItemStack>(), new ArrayList<PotionEffect>(), new ArrayList<String>() );
		valid = false;
	}

	public ArenaClass(String _name, String _displayName, List<ItemStack> _items, 
	                    List<PotionEffect> _effects, List<String> perms ) {
		name = _name;
		CopyOnWriteArrayList<ItemStack> listitems = new CopyOnWriteArrayList<>();
		ArrayList<ItemStack> armoritems = new ArrayList<>();
		if (_items != null) {
			for (ItemStack is: _items) {
				if (InventoryUtil.isArmor(is))
					armoritems.add(is);
				else
					listitems.add(is);
			}
		}
		items = listitems;
		items.addAll(armoritems);
		effects = _effects;
		displayName = _displayName;
        permissions = perms;
		valid = true;
	}

	/**
	 * Get the Display Name
	 * @return displayName or name if displayName is null
	 */
	public String getDisplayName() {
		return displayName != null ? displayName : name;
	}
	
	@Override
	public String toString(){
        return "[ArenaClass " + getName() + "]";
	}

    public List<SpawnInstance> getMobsClone() {
        List<SpawnInstance> l = new ArrayList<>();
        for (SpawnInstance si: mobs){
            if (si instanceof EntitySpawn)
                l.add(new EntitySpawn((EntitySpawn)si));
            else 
                l.add(si);
        }
        return l;
    }
}
