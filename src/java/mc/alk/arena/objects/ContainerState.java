package mc.alk.arena.objects;

import lombok.Getter;
import lombok.Setter;

public class ContainerState {
	public static final ContainerState OPEN = new ContainerState(AreaContainerState.OPEN);

	public static final ContainerState CLOSED = new ContainerState(AreaContainerState.CLOSED);

    @SuppressWarnings( "hiding" )
	public enum AreaContainerState {
        CLOSED, OPEN
	}

	@Getter final AreaContainerState state;
	@Getter @Setter String msg;

	public ContainerState(AreaContainerState _state){
		state = _state;
	}

	public ContainerState(AreaContainerState _state, String _msg){
		state = _state;
		msg = _msg;
	}

	public static ContainerState toState(AreaContainerState state) {
		switch(state){
		case CLOSED: return ContainerState.CLOSED;
		case OPEN: return ContainerState.OPEN;
		default: return null;
		}
	}

	public boolean isOpen() {
		return state == AreaContainerState.OPEN;
	}

	public boolean isClosed() {
		return state == AreaContainerState.CLOSED;
	}
}
