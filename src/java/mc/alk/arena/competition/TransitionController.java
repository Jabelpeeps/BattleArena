package mc.alk.arena.competition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import mc.alk.arena.Defaults;
import mc.alk.arena.controllers.ArenaClassController;
import mc.alk.arena.controllers.MoneyController;
import mc.alk.arena.controllers.PlayerStoreController;
import mc.alk.arena.controllers.TeleportLocationController;
import mc.alk.arena.listeners.PlayerHolder;
import mc.alk.arena.objects.ArenaClass;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.CompetitionState;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.MatchState;
import mc.alk.arena.objects.StateGraph;
import mc.alk.arena.objects.options.StateOptions;
import mc.alk.arena.objects.options.TransitionOption;
import mc.alk.arena.objects.regions.WorldGuardRegion;
import mc.alk.arena.objects.teams.ArenaTeam;
import mc.alk.arena.plugins.DisguiseController;
import mc.alk.arena.plugins.HeroesController;
import mc.alk.arena.plugins.WorldGuardController;
import mc.alk.arena.util.EffectUtil;
import mc.alk.arena.util.ExpUtil;
import mc.alk.arena.util.InventoryUtil;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.MessageUtil;
import mc.alk.arena.util.PlayerUtil;
import mc.alk.arena.util.TeamUtil;


public class TransitionController {

    /**
     * Perform a transition
     * @param am Match, which match to perform the transition on
     * @param transition: which transition are we doing
     * @param teams: which teams to affect
     * @param onlyInMatch: only perform the actions on people still in the arena match
     */
    public static void transition( PlayerHolder am, 
                                   CompetitionState transition, 
                                   Collection<ArenaTeam> teams, 
                                   boolean onlyInMatch ) {
        if ( teams == null ) return;
        
        boolean first = true;
        for ( ArenaTeam team : teams ) {
            transition( am, transition, team, onlyInMatch, first );
            first = false;
        }
    }

    public static boolean transition( PlayerHolder am, 
                                      CompetitionState transition, 
                                      ArenaTeam team, 
                                      boolean onlyInMatch ) {
        return transition( am, transition, team, onlyInMatch, true );
    }

    static boolean transition( PlayerHolder am, 
                               CompetitionState transition, 
                               ArenaTeam team, 
                               boolean onlyInMatch,
                               boolean performOncePerTransitionOptions ) {
        
        StateOptions mo = am.getParams().getStateOptions( transition );
        if ( mo == null ) return true;
        
        if ( performOncePerTransitionOptions && am instanceof Match ) {
            Match match = (Match) am;
            
            /// Options that don't affect players first
            if ( WorldGuardController.hasWorldGuard() && match.getArena() != null && match.getArena().hasRegion() ) {
                WorldGuardRegion region = match.getArena().getWorldGuardRegion();
                /// Clear the area
                if ( mo.hasOption( TransitionOption.WGCLEARREGION ) ) 
                    WorldGuardController.clearRegion( region );

                if ( mo.hasOption( TransitionOption.WGRESETREGION ) ) 
                    WorldGuardController.pasteSchematic( region );
            }
        }
        for ( ArenaPlayer p : team.getPlayers() ) {
            transition( am, transition, p, team, onlyInMatch );
        }
        return true;
    }

    public static boolean transition( PlayerHolder am,  
                                      CompetitionState transition,
                                      ArenaPlayer player, 
                                      ArenaTeam team, 
                                      boolean onlyInMatch ) {
        
        if ( team != null && team.getIndex() != -1 ) {
            MatchParams mp = am.getParams().getTeamParams( team.getIndex() );
            if ( mp != null )
                return transition( am, transition, player, team, onlyInMatch, mp.getStateGraph() );
        }
        return transition( am, transition, player, team, onlyInMatch, am.getParams().getStateGraph() );
    }

    private static boolean transition( PlayerHolder am, 
                                       CompetitionState transition, 
                                       ArenaPlayer player, 
                                       ArenaTeam team, 
                                       boolean onlyInMatch, 
                                       StateGraph tops ) {
        
        if (tops == null) return true;
        
        StateOptions mo = tops.getOptions(transition);
        if (mo == null) return true;
        
        if ( Defaults.DEBUG_TRANSITIONS ) 
            Log.info( "-- transition " + am.getClass().getSimpleName() + "  " + transition + " p= " + player.getName() +
                " ops=" + mo + " onlyInMatch=" + onlyInMatch + " inArena=" + am.isHandled( player ) + " dead=" + player.isDead()
                + ":" + player.getHealth() + " online=" + player.isOnline() + " clearInv=" +
                am.getParams().getStateGraph().hasOptionAt( transition, TransitionOption.CLEARINVENTORY ) );
        
        boolean insideArena = am.isHandled( player );
        boolean teleportIn = mo.hasOption( TransitionOption.TELEPORTIN );
        boolean teleportRoom = mo.hasAnyOption( TransitionOption.TELEPORTSPECTATE, 
                                                TransitionOption.TELEPORTLOBBY, 
                                                TransitionOption.TELEPORTMAINLOBBY, 
                                                TransitionOption.TELEPORTWAITROOM, 
                                                TransitionOption.TELEPORTMAINWAITROOM );
 
        /// If the flag onlyInMatch is set, we should leave if the player isnt inside.  disregard if we are teleporting people in
        if ( onlyInMatch 
                && (!insideArena && !(teleportIn || teleportRoom) 
                        || am instanceof Match 
                        && !((Match)am).isInMatch(player) 
                        && player.getCompetition() != null 
                        && !player.getCompetition().equals(am) ) ) {
            return true;
        }

        boolean teleportOut = mo.hasAnyOption( TransitionOption.TELEPORTOUT, TransitionOption.TELEPORTTO );
        boolean wipeInventory = mo.hasOption( TransitionOption.CLEARINVENTORY );

        List<PotionEffect> effects = mo.getEffects() != null ? new ArrayList<>( mo.getEffects() ) : null;
        Integer hunger = mo.getHunger();

        int teamIndex = team == null ? -1 : team.getIndex();
        boolean playerReady = player.isOnline();
        boolean dead = !player.isOnline() || player.isDead();
        Player p = player.getPlayer();

        /// Teleport In. only tpin, respawn tps happen elsewhere
        if ((teleportIn && transition != MatchState.ONSPAWN) || teleportRoom){
            
            if ( ( insideArena || am.checkReady(player, team, mo ) ) && !dead )
                TeleportLocationController.teleport(am, team, player,mo, teamIndex);
            else
                playerReady = false;
        }

        boolean storeAll = mo.hasOption(TransitionOption.STOREALL);
        PlayerStoreController psc = PlayerStoreController.INSTANCE;
        boolean armorTeams = tops.hasOption(TransitionOption.ARMORTEAMS);
        boolean woolTeams = tops.hasOption(TransitionOption.WOOLTEAMS);

        /// Only do if player is online options
        if (playerReady && !dead) {
            Double prizeMoney = null; /// kludge, take out when I find a better way to display messages
            if (storeAll || mo.hasOption(TransitionOption.STOREGAMEMODE)) psc.storeGamemode(player);
            if (storeAll || mo.hasOption(TransitionOption.STOREEXPERIENCE)) psc.storeExperience(player);
            if (storeAll || mo.hasOption(TransitionOption.STOREITEMS)) psc.storeItems(player);
            if (storeAll || mo.hasOption(TransitionOption.STOREHEALTH)) psc.storeHealth(player);
            if (storeAll || mo.hasOption(TransitionOption.STOREHUNGER)) psc.storeHunger(player);
            if (storeAll || mo.hasOption(TransitionOption.STOREMAGIC)) psc.storeMagic(player);
            if (storeAll || mo.hasOption(TransitionOption.STOREHEROCLASS)) psc.storeHeroClass(player);
            if (storeAll || mo.hasOption(TransitionOption.STOREGAMEMODE)) psc.storeGodmode(player);
            if (storeAll || mo.hasOption(TransitionOption.STOREFLIGHT)) psc.storeFlight(player);
            if (storeAll || mo.hasOption(TransitionOption.STOREENCHANTS)) psc.storeEffects(player);
            if (wipeInventory) InventoryUtil.clearInventory(p);
            if (mo.hasOption(TransitionOption.CLEAREXPERIENCE)) ExpUtil.clearExperience(p);
            if (mo.hasOption(TransitionOption.HEALTH)) PlayerUtil.setHealth(p, mo.getHealth());
            if (mo.hasOption(TransitionOption.HEALTHP)) PlayerUtil.setHealthPercent(p, mo.getHealthP());
            if (mo.hasOption(TransitionOption.MAGIC)) HeroesController.setMagicLevel(p, mo.getMagic()); 
            if (mo.hasOption(TransitionOption.MAGICP)) HeroesController.setMagicLevelP(p, mo.getMagicP()); 
            if (hunger != null) p.setFoodLevel( hunger ); 
            if (mo.hasOption(TransitionOption.INVULNERABLE)) PlayerUtil.setInvulnerable(p,mo.getInvulnerable()*20);
            if (mo.hasOption(TransitionOption.GAMEMODE)) PlayerUtil.setGameMode(p,mo.getGameMode()); 
            if (mo.hasOption(TransitionOption.FLIGHTOFF)) PlayerUtil.setFlight(p,false); 
            if (mo.hasOption(TransitionOption.FLIGHTON)) PlayerUtil.setFlight(p,true); 
            if (mo.hasOption(TransitionOption.FLIGHTSPEED)) p.setFlySpeed( mo.getFlightSpeed() ); 
            if (mo.hasOption(TransitionOption.DOCOMMANDS)) PlayerUtil.doCommands(p,mo.getDoCommands());
            if (mo.hasOption(TransitionOption.DEENCHANT)) psc.deEnchant(p);
            if (mo.hasOption(TransitionOption.UNDISGUISE)) DisguiseController.undisguise(p);
            if (mo.getDisguiseAllAs() != null) DisguiseController.disguisePlayer(p, mo.getDisguiseAllAs());
            if (mo.getMoney() != null) MoneyController.add( player.getPlayer(), mo.getMoney() );
            
            if (mo.hasOption(TransitionOption.POOLMONEY) && am instanceof Match) {
                prizeMoney = ((Match)am).getPrizePoolMoney() * mo.getDouble(TransitionOption.POOLMONEY) / 
                        ( team != null ? team.size() : 1 );
                
                if (prizeMoney >= 0)
                    MoneyController.add( player.getPlayer(), prizeMoney );
                else
                    MoneyController.subtract( player.getPlayer(), prizeMoney );
            }
            if (mo.getExperience() != null) ExpUtil.giveExperience(p, mo.getExperience());
            
//            if (mo.hasOption(TransitionOption.REMOVEPERMS)) removePerms(player, mo.getRemovePerms());
//            if (mo.hasOption(TransitionOption.ADDPERMS)) addPerms(player, mo.getAddPerms(), 0);
            
            if (mo.hasOption(TransitionOption.GIVECLASS) && player.getCurrentClass() == null) {
                ArenaClass ac = getArenaClass(mo,teamIndex);
                if (ac != null && ac.isValid()) { 
                    if (mo.hasAnyOption(TransitionOption.WOOLTEAMS, TransitionOption.ALWAYSWOOLTEAMS)) 
                        TeamUtil.setTeamHead(teamIndex, player); 
                    
                    if (armorTeams)
                        ArenaClassController.giveClass(player, ac, TeamUtil.getTeamColor(teamIndex));
                    else
                        ArenaClassController.giveClass(player, ac);
                }
            }
            if (mo.hasOption(TransitionOption.CLASSENCHANTS)) {
                ArenaClass ac = player.getCurrentClass();
                if (ac != null){
                    ArenaClassController.giveClassEnchants(p, ac);}
            }
            if (mo.hasOption(TransitionOption.GIVEDISGUISE) && DisguiseController.enabled()){
                String disguise = getDisguise(mo,teamIndex);
                if (disguise != null) { 
                    DisguiseController.disguisePlayer(p, disguise);}
            }
            if (mo.hasOption(TransitionOption.GIVEITEMS)){
                Color color = armorTeams ? TeamUtil.getTeamColor(teamIndex) : null;
                giveItems(transition, player, mo.getGiveItems(),teamIndex, woolTeams, insideArena,color);
            }

            try {
                if (effects != null)
                EffectUtil.enchantPlayer(p, effects);
            } 
            catch (Exception e){
                if ( !Defaults.DEBUG_VIRTUAL ) Log.warn( "BattleArena " + p.getName() + " was not enchanted" );
            }
            if (Defaults.ANNOUNCE_GIVEN_ITEMS){
                String prizeMsg = mo.getPrizeMsg(null, prizeMoney);
                if (prizeMsg != null)
                    MessageUtil.sendMessage(player,"&eYou have been given \n"+prizeMsg);
            }
            if (teleportIn){
                transition(am, MatchState.ONSPAWN, player, team, false);
            }
            /// else we have a subseT of the options we should always do regardless if they are alive or not
        }  
        else if (teleportOut) {
//            if (mo.hasOption(TransitionOption.REMOVEPERMS)) removePerms(player, mo.getRemovePerms());
            if (mo.hasOption(TransitionOption.GAMEMODE)) PlayerUtil.setGameMode(p,mo.getGameMode()); 
            if (mo.hasOption(TransitionOption.FLIGHTOFF)) PlayerUtil.setFlight(p,false);
            if (mo.hasOption(TransitionOption.DEENCHANT)) psc.deEnchant(p);
            if (wipeInventory) InventoryUtil.clearInventory(p);
        }

        /// Teleport out, need to do this at the end so that all the onCancel/onComplete options are completed first
        if (teleportOut ){ /// Lets not teleport people out who are already out(like dead ppl)
            TeleportLocationController.teleportOut(am, team, player,mo);
        }
        /// Restore their exp and items.. Has to happen AFTER teleport
        boolean restoreAll = mo.hasOption(TransitionOption.RESTOREALL);
        if (restoreAll || mo.hasOption(TransitionOption.RESTOREGAMEMODE)) psc.restoreGamemode(player);
        if (restoreAll || mo.hasOption(TransitionOption.RESTOREEXPERIENCE)) psc.restoreExperience(player);
        if (restoreAll || mo.hasOption(TransitionOption.RESTOREITEMS)){
            if (woolTeams && teamIndex != -1){
                /// Teams that have left can have a -1 teamIndex
                TeamUtil.removeTeamHead(teamIndex, p);
            }
            if (Defaults.DEBUG_TRANSITIONS)Log.info("   "+transition+" transition restoring items "+insideArena);
            psc.restoreItems(player);
        }
        if (restoreAll || mo.hasOption(TransitionOption.RESTOREENCHANTS)) psc.restoreEffects(player);
        if (restoreAll || mo.hasOption(TransitionOption.RESTOREHEALTH)) psc.restoreHealth(player);
        if (restoreAll || mo.hasOption(TransitionOption.RESTOREHUNGER)) psc.restoreHunger(player);
        if (restoreAll || mo.hasOption(TransitionOption.RESTOREMAGIC)) psc.restoreMagic(player);
        if (restoreAll || mo.hasOption(TransitionOption.RESTOREHEROCLASS)) psc.restoreHeroClass(player);
        if (restoreAll || mo.hasOption(TransitionOption.RESTOREGODMODE)) psc.restoreGodmode(player);
        if (restoreAll || mo.hasOption(TransitionOption.RESTOREFLIGHT)) psc.restoreFlight(player);
        return true;
    }

//    private static void removePerms(ArenaPlayer p, List<String> perms) {
////		if (perms == null || perms.isEmpty()) {
////        }
//        /// TODO complete
//    }

//    private static void addPerms(ArenaPlayer p, List<String> perms, int ticks) {
//        if (perms == null || perms.isEmpty()) return;
//        
//        PermissionAttachment attachment = p.getPlayer().addAttachment(BattleArena.getSelf(),ticks);
//        for (String perm: perms){
//            attachment.setPermission(perm, true);}
//    }

    private static void giveItems( CompetitionState ms, ArenaPlayer p, List<ItemStack> items,
                                   int teamIndex, boolean woolTeams, boolean insideArena, Color color ) {
        
        if (woolTeams && insideArena) TeamUtil.setTeamHead(teamIndex, p);
        if (Defaults.DEBUG_TRANSITIONS) Log.info("   " + ms + " transition giving items to " + p.getName() );      
        if (items != null && !items.isEmpty()) InventoryUtil.addItemsToInventory( p.getPlayer(), items, woolTeams, color);
    }

    private static ArenaClass getArenaClass( StateOptions mo, int teamIndex ) {
        Map<Integer,ArenaClass> classes = mo.getClasses();
        if ( classes == null ) return null;
        
        if (classes.containsKey(teamIndex))
            return classes.get(teamIndex);
        else if (classes.containsKey(ArenaClass.DEFAULT))
            return classes.get(ArenaClass.DEFAULT);
        
        return null;
    }

    private static String getDisguise( StateOptions mo, int teamIndex ) {
        Map<Integer,String> disguises = mo.getDisguises();
        if ( disguises == null ) return null;
        
        if (disguises.containsKey(teamIndex))
            return disguises.get(teamIndex);
        else if (disguises.containsKey(Integer.MAX_VALUE))
            return disguises.get(Integer.MAX_VALUE);
        
        return null;
    }
}
