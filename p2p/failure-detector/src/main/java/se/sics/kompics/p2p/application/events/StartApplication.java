package se.sics.kompics.p2p.application.events;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;

/**
 * The <code>StartApplication</code> class.
 * 
 * @author Cosmin Arad
 * @version $Id: StartApplication.java 65 2008-05-13 09:01:01Z cosmin $
 */
@EventType
public final class StartApplication implements Event {

	private final String[] operations;

	public StartApplication(String operations) {
		this.operations = operations.split(":");
	}

	public final String[] getOperations() {
		return operations;
	}
}
