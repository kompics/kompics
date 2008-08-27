package se.sics.kompics.p2p.chord.router;

import java.util.LinkedList;

import se.sics.kompics.network.Address;
import se.sics.kompics.p2p.chord.events.ChordLookupRequest;

/**
 * The <code>LookupInfo</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: LookupInfo.java 158 2008-06-16 10:42:01Z Cosmin $
 */
public class LookupInfo {

	private final ChordLookupRequest request;

	private long initiated, completed;

	private int hopCount;

	private final LinkedList<Address> hops;

	public LookupInfo(ChordLookupRequest request) {
		this.request = request;
		this.hopCount = 0;
		this.initiated = 0;
		this.completed = 0;
		this.hops = new LinkedList<Address>();
	}

	public int getHopCount() {
		return hopCount;
	}

	public void appendHop(Address hop) {
		this.hopCount++;
		this.hops.add(hop);
	}

	public ChordLookupRequest getRequest() {
		return request;
	}

	public void initiatedNow() {
		initiated = System.currentTimeMillis();
	}

	public void completedNow() {
		completed = System.currentTimeMillis();
	}

	public long getduration() {
		return completed - initiated;
	}

	public LinkedList<Address> getHops() {
		return hops;
	}
}
