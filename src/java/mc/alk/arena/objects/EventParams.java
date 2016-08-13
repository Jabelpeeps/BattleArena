package mc.alk.arena.objects;

import java.util.ArrayList;
import java.util.List;

import lombok.Setter;
import mc.alk.arena.objects.arenas.ArenaType;


public class EventParams extends MatchParams{
    @Setter Integer secondsTillStart;
	@Setter Integer announcementInterval;
	@Setter List<String> playerOpenOptions;
	EventParams eparent;

	public EventParams(MatchParams mp) {
		super(mp);
	}

    @Override
    public void copy(ArenaParams ap){
        if (this == ap)
            return;
        super.copy(ap);
        if (ap instanceof EventParams){
            EventParams ep = (EventParams) ap;
            secondsTillStart = ep.secondsTillStart;
            announcementInterval = ep.announcementInterval;
            eparent = ep.eparent;
            if (ep.playerOpenOptions != null)
                playerOpenOptions = new ArrayList<>(ep.playerOpenOptions);
        }
    }

	@Override
	public void flatten() {
		if (eparent != null){
			if ( secondsTillStart == null)  secondsTillStart = eparent.getSecondsTillStart();
			if ( announcementInterval == null)  announcementInterval = eparent.getAnnouncementInterval();
			if ( playerOpenOptions == null)  playerOpenOptions = eparent.getPlayerOpenOptions();
			 eparent = null;
		}
		super.flatten();
	}

	public EventParams(ArenaType at) {
		super(at);
	}
	public Integer getSecondsTillStart() {
        return secondsTillStart == null && eparent != null ? eparent.getSecondsTillStart() : secondsTillStart;
    }
	public Integer getAnnouncementInterval() {
        return announcementInterval == null && eparent != null ? eparent.getAnnouncementInterval() : announcementInterval;
	}
	@Override
	public JoinType getJoinType() {
		return JoinType.JOINPHASE;
	}

	public List<String> getPlayerOpenOptions(){
		return playerOpenOptions != null ? playerOpenOptions 
		                                 : (eparent != null ? eparent.getPlayerOpenOptions() 
		                                                    : null);
	}
	@Override
	public void setParent(ArenaParams parent) {
		super.setParent(parent);
        this.eparent = (parent instanceof EventParams) ? (EventParams) parent : null;
	}

}
