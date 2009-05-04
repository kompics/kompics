package se.sics.kompics.kdld.daemon;

import se.sics.kompics.address.Address;

/**
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class ListJobsLoadedRequest extends DaemonRequestMessage {

	private static final long serialVersionUID = 2266048689015242375L;
	private final int daemonId;
	
	public ListJobsLoadedRequest(int daemonId, Address src, DaemonAddress dest) {
		super(src, dest);
		this.daemonId = daemonId;
	}
	
	public int getDaemonId() {
		return daemonId;
	}
}