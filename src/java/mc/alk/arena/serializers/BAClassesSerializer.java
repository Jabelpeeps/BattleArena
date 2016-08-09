package mc.alk.arena.serializers;

import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import mc.alk.arena.BattleArena;
import mc.alk.arena.controllers.ArenaClassController;
import mc.alk.arena.objects.ArenaClass;
import mc.alk.arena.objects.CommandLineString;
import mc.alk.arena.objects.exceptions.InvalidOptionException;
import mc.alk.arena.objects.spawns.SpawnInstance;
import mc.alk.util.InventoryUtil;
import mc.alk.util.Log;

public class BAClassesSerializer extends BaseConfig {

    public void loadAll() {
        try {
            config.load(file);
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
        loadClasses(config.getConfigurationSection("classes"));
    }

    public void loadClasses(ConfigurationSection cs) {
        if (cs == null) {
            Log.info(BattleArena.getNameAndVersion() + " has no classes");
            return;
        }
        StringBuilder sb = new StringBuilder();
        Set<String> keys = cs.getKeys(false);
        boolean first = true;
        for (String className : keys) {
            try {
                ArenaClass ac = parseArenaClass(cs.getConfigurationSection(className));
                if (ac == null) {
                    continue;
                }
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(ac.getName());
                ArenaClassController.addClass(ac);
            } catch (Exception e) {
                Log.err("There was an error loading the class " + className + ". It will be disabled");
                Log.printStackTrace(e);
            }
        }
        if (first) {
            Log.info(BattleArena.getNameAndVersion() + " no predefined classes found. inside of " + cs.getCurrentPath());
        } else {
            Log.info(BattleArena.getNameAndVersion() + " registering classes: " + sb.toString());
        }
    }

    public ArenaClass parseArenaClass(ConfigurationSection cs) {
        List<ItemStack> items = null;
        List<PotionEffect> effects = null;
        List<SpawnInstance> mobs = null;
        List<CommandLineString> commands = null;
        List<String> permissions = null;

        if (cs.contains("items")) {
            items = InventoryUtil.getItemList(cs, "items");
        }
        if (cs.contains("enchants")) {
            effects = ConfigSerializer.getEffectList(cs, "enchants");
        }
        if (cs.contains("mobs")) {
            mobs = SpawnSerializer.getSpawnList(cs.getConfigurationSection("mobs"));
        }
        if (cs.contains("permissions")) {
            permissions = cs.getStringList("permissions");
        }
        if (cs.contains("doCommands")) {
            try {
                commands = ConfigSerializer.getDoCommands(cs.getStringList("doCommands"));
            } catch (InvalidOptionException e) {
                Log.printStackTrace(e);
            }
        }
        String displayName = cs.getString("displayName", null);
        displayName = displayName == null || displayName.isEmpty() ? cs.getName() : displayName;
        ArenaClass ac = new ArenaClass(cs.getName(), displayName, items, effects, permissions);
        if (mobs != null && !mobs.isEmpty()) {
            ac.setMobs(mobs);
        }
        if (cs.contains("disguise")) {
            ac.setDisguiseName(cs.getString("disguise"));
        }
        if (commands != null && !commands.isEmpty()) {
            ac.setDoCommands(commands);
        }
        return ac;
    }

}
