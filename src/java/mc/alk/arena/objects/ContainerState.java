package mc.alk.arena.objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@RequiredArgsConstructor
public class ContainerState {
    public enum AreaContainerState { isCLOSED, isOPEN }
    
	public static final ContainerState OPEN = new ContainerState( AreaContainerState.isOPEN );
	public static final ContainerState CLOSED = new ContainerState( AreaContainerState.isCLOSED );

	@Getter final AreaContainerState state;
	@Getter @Setter String msg;

	public static ContainerState toState( AreaContainerState state ) {
		switch( state ) {
    		case isCLOSED: return ContainerState.CLOSED;
    		case isOPEN: return ContainerState.OPEN;
    		default: return null;
		}
	}
	public boolean isOpen() { return state == AreaContainerState.isOPEN; }
	public boolean isClosed() { return state == AreaContainerState.isCLOSED; }
}
