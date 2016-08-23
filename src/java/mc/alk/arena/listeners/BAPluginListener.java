package mc.alk.arena.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.controllers.MoneyController;
import mc.alk.arena.objects.messaging.AnnouncementOptions;
import mc.alk.arena.objects.messaging.plugins.HerochatPlugin;
import mc.alk.arena.plugins.DisguiseController;
import mc.alk.arena.plugins.EssentialsUtil;
import mc.alk.arena.plugins.FactionsController;
import mc.alk.arena.plugins.HeroesController;
import mc.alk.arena.plugins.McMMOController;
import mc.alk.arena.plugins.MobArenaUtil;
import mc.alk.arena.plugins.VanishNoPacketUtil;
import mc.alk.arena.plugins.WorldEditUtil;
import mc.alk.arena.plugins.WorldGuardController;
import mc.alk.arena.util.Log;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;

/**
 *
 * @author alkarin
 *
 */
public class BAPluginListener implements Listener {

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (event.getPlugin().getName().equalsIgnoreCase("CombatTag")) {
            loadCombatTag();
        } else if (event.getPlugin().getName().equalsIgnoreCase("Essentials")) {
            loadEssentials();
        } else if (event.getPlugin().getName().equalsIgnoreCase("Factions")) {
            loadFactions();
        } else if (event.getPlugin().getName().equalsIgnoreCase("Herochat")) {
            loadHeroChat();
        } else if (event.getPlugin().getName().equalsIgnoreCase("Heroes")) {
            loadHeroes();
        } else if (event.getPlugin().getName().equalsIgnoreCase("LibsDisguises")) {
            loadLibsDisguise();
        } else if (event.getPlugin().getName().equalsIgnoreCase("mcMMO")) {
            loadMcMMO();
        } else if (event.getPlugin().getName().equalsIgnoreCase("MobArena")) {
            loadMobArena();
        } else if (event.getPlugin().getName().equalsIgnoreCase("MultiInv")) {
            loadMultiInv();
        } else if (event.getPlugin().getName().equalsIgnoreCase("Multiverse-Core")) {
            loadMultiverseCore();
        } else if (event.getPlugin().getName().equalsIgnoreCase("Multiverse-Inventories")) {
            loadMultiverseInventory();
        } else if (event.getPlugin().getName().equalsIgnoreCase("WorldEdit")) {
            loadWorldEdit();
        } else if (event.getPlugin().getName().equalsIgnoreCase("WorldGuard")) {
            loadWorldGuard();
        } else if (event.getPlugin().getName().equalsIgnoreCase("VanishNoPacket")) {
            loadVanishNoPacket();
        } else if (event.getPlugin().getName().equalsIgnoreCase("Vault")) {
            loadVault();
        } else {
            loadOthers();
        }
    }

    public void loadAll() {
        loadCombatTag();
        loadEssentials();
        loadFactions();
        loadHeroChat();
        loadHeroes();
        loadLibsDisguise();
        loadMcMMO();
        loadMobArena();
        loadMultiInv();
        loadMultiverseCore();
        loadMultiverseInventory();
        loadWorldEdit();
        loadWorldGuard();
        loadVanishNoPacket();
        loadVault();
        loadOthers();
    }

    public void loadCombatTag() {
        if ( Bukkit.getPluginManager().getPlugin("CombatTag") != null ) {
            Log.info( "[BattleArena] CombatTag detected, enabling limited tag support" );
        }
    }
    public void loadEssentials() {
        if ( EssentialsUtil.isEnabled() ) 
            Log.info( "[BattleArena] Essentials detected. God mode handling activated" );
    }

    public void loadFactions() {
        if (!FactionsController.enabled()) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("Factions");
            if (plugin != null) {
                if (FactionsController.setPlugin(true)) {
                    Log.info("[BattleArena] Factions detected. Configurable power loss enabled (default no powerloss)");
                } else {
                    Log.info("[BattleArena] Old Factions detected that does not have a PowerLossEvent");
                }
            }
        }
    }

    public void loadHeroChat() {
        if (AnnouncementOptions.chatPlugin == null) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("Herochat");
            if (plugin != null) {
                AnnouncementOptions.setChatPlugin(new HerochatPlugin());
                Log.info("[BattleArena] Herochat detected, adding channel options");
            }
        }
    }

    public void loadHeroes() {
        if (!HeroesController.enabled()) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("Heroes");
            if (plugin != null) {
                HeroesController.setPlugin(plugin);
                Log.info("[BattleArena] Heroes detected. Implementing heroes class options");
            }
        }
    }

    public void loadLibsDisguise() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("LibsDisguises");
        if (plugin != null) {
            if (!DisguiseController.enabled()) {
                DisguiseController.setLibsDisguise(plugin);
                Log.info("[BattleArena] LibsDisguises detected. Implementing disguises");
            }
        }
    }

    public void loadMcMMO() {
        if (!McMMOController.isEnabled()) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("mcMMO");
            if (plugin != null) {
                McMMOController.setEnabled(true);
                Log.info("[BattleArena] mcMMO detected. Implementing disabled skills options");
            }
        }
    }

    public void loadMobArena() {
        if ( MobArenaUtil.isEnabled() ) 
                Log.info( "[BattleArena] MobArena detected.  Implementing no add when in MobArena" );
    }

    public void loadMultiInv() {
        if (!Defaults.PLUGIN_MULTI_INV) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("MultiInv");
            if (plugin != null) {
                Defaults.PLUGIN_MULTI_INV = true;
                Log.info("[BattleArena] MultiInv detected.  Implementing teleport/gamemode workarounds");
            }
        }
    }

    public void loadMultiverseCore() {
        if (!Defaults.PLUGIN_MULITVERSE_CORE) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("Multiverse-Core");
            if (plugin != null) {
                Defaults.PLUGIN_MULITVERSE_CORE = true;
                Log.info("[BattleArena] Multiverse-Core detected. Implementing teleport/gamemode workarounds");
            }
        }
    }

    public void loadMultiverseInventory() {
        if (!Defaults.PLUGIN_MULITVERSE_INV) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("Multiverse-Inventories");
            if (plugin != null) {
                Defaults.PLUGIN_MULITVERSE_INV = true;
                Log.info("[BattleArena] Multiverse-Inventories detected. Implementing teleport/gamemode workarounds");
            }
        }
    }

    public void loadWorldEdit() {
        if ( !WorldEditUtil.hasWorldEdit() && WorldEditUtil.checkIfLoadedYet() )
            Log.info( "[BattleArena] WorldEdit detected." );               
    }

    public void loadWorldGuard() {
        if ( !WorldGuardController.hasWorldGuard() && WorldGuardController.checkIfLoadedYet() ) 
            Log.info( "[BattleArena] WorldGuard detected. WorldGuard regions can now be used" );
    }

    public void loadVanishNoPacket() {
        if ( VanishNoPacketUtil.isEnabled() ) 
            Log.info( "[BattleArena] VanishNoPacket detected. Invisibility fix is "
                        + "disabled for vanished players not in an arena" );
    }

    public void loadVault() {
        if ( Bukkit.getPluginManager().getPlugin("Vault") != null ) {
            /// Load vault economy
            if (!MoneyController.hasEconomy()) {
                try {
                    RegisteredServiceProvider<Economy> provider = 
                            Bukkit.getServer().getServicesManager().getRegistration( Economy.class );
                    
                    if (provider == null || provider.getProvider() == null) {
                        Log.warn( BattleArena.getNameAndVersion() + " found no economy plugin. "
                                    + "Attempts to use money in arenas might result in errors.");
                        return;
                    }
                    MoneyController.setEconomy(provider.getProvider());
                    Log.info( BattleArena.getNameAndVersion() + " found economy plugin Vault. [Default]"  );
                } 
                catch (Error e) {
                    Log.err( BattleArena.getNameAndVersion() + " exception loading economy through Vault"    );
                    Log.printStackTrace(e);
                }
            }
            /// Load Vault chat
            if (AnnouncementOptions.vaultChat == null) {
                try {
                    RegisteredServiceProvider<Chat> provider = 
                            Bukkit.getServer().getServicesManager().getRegistration( Chat.class );
                    
                    if (provider != null && provider.getProvider() != null)
                        AnnouncementOptions.setVaultChat(provider.getProvider());
                    else if (AnnouncementOptions.chatPlugin == null) 
                        Log.info("[BattleArena] Vault chat not detected, ignoring channel options");
                } 
                catch (Error e) {
                    Log.err(BattleArena.getNameAndVersion() + " exception loading chat through Vault");
                    Log.printStackTrace(e);
                }
            }
        }
    }

    private void loadOthers() {
        if (Bukkit.getPluginManager().getPlugin("AntiLootSteal") != null) {
            Defaults.PLUGIN_ANTILOOT = true;
        }
    }
}
