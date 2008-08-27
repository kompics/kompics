package se.sics.kompics.p2p.chord.router.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.p2p.chord.router.FingerTableView;
import se.sics.kompics.p2p.network.events.PerfectNetworkDeliverEvent;

/**
 * The <code>GetFingerTableResponse</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: GetFingerTableResponse.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class GetFingerTableResponse extends PerfectNetworkDeliverEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7687731796500255775L;

	private final FingerTableView fingerTable;

	public GetFingerTableResponse(FingerTableView fingerTable) {
		super();
		this.fingerTable = fingerTable;
	}

	public FingerTableView getFingerTable() {
		return fingerTable;
	}
}
