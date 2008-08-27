package se.sics.kompics.p2p.chord.router.events;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;
import se.sics.kompics.p2p.chord.router.FingerTableView;

/**
 * The <code>NewFingerTable</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: NewFingerTable.java 158 2008-06-16 10:42:01Z Cosmin $
 */
@EventType
public final class NewFingerTable implements Event {

	private final FingerTableView fingerTableView;

	public NewFingerTable(FingerTableView fingerTableView) {
		this.fingerTableView = fingerTableView;
	}

	public FingerTableView getFingerTableView() {
		return fingerTableView;
	}
}
