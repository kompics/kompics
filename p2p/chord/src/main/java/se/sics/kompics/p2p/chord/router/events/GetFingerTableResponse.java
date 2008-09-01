package se.sics.kompics.p2p.chord.router.events;

import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.events.Message;
import se.sics.kompics.p2p.chord.router.FingerTableView;

/**
 * The <code>GetFingerTableResponse</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: GetFingerTableResponse.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class GetFingerTableResponse extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5902098190107627207L;

	private final FingerTableView fingerTable;

	public GetFingerTableResponse(FingerTableView fingerTable,
			Address destination) {
		super(destination);
		this.fingerTable = fingerTable;
	}

	public FingerTableView getFingerTable() {
		return fingerTable;
	}
}
