package mc.alk.arena.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.ChatColor;

import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.controllers.RoomController;
import mc.alk.arena.objects.arenas.ArenaType;
import mc.alk.arena.objects.messaging.AnnouncementOptions;
import mc.alk.arena.objects.modules.ArenaModule;
import mc.alk.arena.objects.options.TransitionOption;
import mc.alk.arena.objects.victoryconditions.VictoryType;
import mc.alk.arena.util.MessageUtil;


public class MatchParams extends ArenaParams implements Comparable<MatchParams>{

    @Setter String prefix;
    @Setter String signDisplayName;
    @Setter VictoryType victoryType;
    @Setter Integer intervalTime;
    @Setter @Getter AnnouncementOptions announcementOptions;

    @Setter Integer numConcurrentCompetitions;
    Set<ArenaModule> modules;
    @Setter Boolean useTrackerPvP;
    @Setter Boolean useTrackerMessages;
    @Setter Boolean useTeamRating;

    MatchParams mparent;


    public MatchParams(){
        super();
    }

    public MatchParams(ArenaType at) {
        super(at);
    }

    public MatchParams(MatchParams mp) {
        super(mp);
    }

    @Override
    public void copy(ArenaParams ap){
        if ( this == ap ) return;
        
        super.copy(ap);
        
        if (ap instanceof MatchParams){
            MatchParams mp = (MatchParams)ap;
            prefix = mp.prefix;
            victoryType = mp.victoryType;

            intervalTime = mp.intervalTime;
            announcementOptions = mp.announcementOptions;
            numConcurrentCompetitions = mp.numConcurrentCompetitions;
            mparent = mp.mparent;
            useTrackerMessages = mp.useTrackerMessages;
            useTrackerPvP = mp.useTrackerPvP;
            useTeamRating  = mp.useTeamRating;
            signDisplayName = mp.signDisplayName;
            if (mp.modules != null)
                modules = new HashSet<>(mp.modules);
        }

    }
    @Override
    public void flatten() {
        if (mparent != null){
            if ( prefix == null) prefix = mparent.getPrefix();
            if ( victoryType == null) victoryType = mparent.getVictoryType();
            if ( intervalTime == null) intervalTime = mparent.getIntervalTime();
            if ( announcementOptions == null ) announcementOptions = mparent.getAnnouncementOptions();
            if ( numConcurrentCompetitions == null) numConcurrentCompetitions = mparent.getNConcurrentCompetitions();
            if ( useTrackerMessages == null) useTrackerMessages = mparent.getUseTrackerMessages();
            if ( useTrackerPvP == null) useTrackerPvP = mparent.getUseTrackerPvP();
            if ( useTeamRating == null) useTeamRating = mparent.isTeamRating();
            if ( signDisplayName== null) signDisplayName = mparent.getSignDisplayName();
            modules = getModules();
            mparent = null;
        }
        super.flatten();
    }

    public VictoryType getVictoryType() {
        return victoryType == null && mparent != null ? mparent.getVictoryType() : victoryType;
    }
    public String getPrefix(){
        return prefix == null && mparent != null ? mparent.getPrefix() : prefix;
    }
    public String getSignDisplayName(){
        return signDisplayName == null && mparent != null ? mparent.getSignDisplayName() : signDisplayName;
    }

    @Override
    public int compareTo(MatchParams other) {
        Integer hash = this.hashCode();
        return hash.compareTo(other.hashCode());
    }

    public Integer getIntervalTime() {
        return intervalTime == null && mparent != null ? mparent.getIntervalTime() : intervalTime;
    }
    @Override
    public int hashCode() {
        return type == null ? super.hashCode() : ( type.ordinal() << 25 );
    }
    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof MatchParams && this.hashCode() == other.hashCode());
    }
    @Override
    public String toString(){
        return super.toString() + ",vc=" + victoryType;
    }

    public ChatColor getColor() {
        return MessageUtil.getFirstColor( getPrefix() );
    }

    public Integer getNConcurrentCompetitions(){
        return numConcurrentCompetitions != null ? numConcurrentCompetitions 
                                                 : (mparent != null ? mparent.getNConcurrentCompetitions() 
                                                                    : null);
    }

    public JoinType getJoinType() {
        return JoinType.QUEUE;
    }

    public void addModule(ArenaModule am) {
        if (modules == null)
            modules = new HashSet<>();
        modules.add(am);
    }

    public Set<ArenaModule> getModules() {
        Set<ArenaModule> ms  = modules == null ? new HashSet<>() : new HashSet<>(modules);

        if (mparent != null) {
            ms.addAll(mparent.getModules());
        }
        return ms;
    }

    public Boolean getUseTrackerPvP() {
        return useTrackerPvP != null ? useTrackerPvP : (mparent!= null ? mparent.getUseTrackerPvP() : null);
    }
    public Boolean getUseTrackerMessages() {
        return useTrackerMessages != null ? useTrackerMessages : (mparent!= null ? mparent.getUseTrackerMessages() : null);
    }

    @Override
    public boolean valid() {
        return super.valid() && (!getStateGraph().hasAnyOption(TransitionOption.TELEPORTLOBBY) ||
                                RoomController.hasLobby(getType()));
    }

    @Override
    public Collection<String> getInvalidReasons() {
        List<String> reasons = new ArrayList<>();
        if (getStateGraph().hasAnyOption(TransitionOption.TELEPORTLOBBY) && !RoomController.hasLobby(getType()))
            reasons.add("Needs a Lobby");
        reasons.addAll(super.getInvalidReasons());
        return reasons;
    }

    @Override
    public void setParent(ArenaParams _parent) {
        super.setParent(_parent);
        mparent = (_parent instanceof MatchParams) ? (MatchParams) _parent : null;
    }

    public Boolean isTeamRating(){
        return useTeamRating != null ? useTeamRating : (mparent!= null ? mparent.isTeamRating() : null);
    }
}
