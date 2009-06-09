package se.sics.kompics.wan.daemon;

import se.sics.kompics.address.Address;


/**
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class ListJobsLoadedRequestMsg extends DaemonRequestMessage {

	private static final long serialVersionUID = 2266048689015242375L;
	
	public ListJobsLoadedRequestMsg(Address src, DaemonAddress dest) {
		super(src, dest);
	}
	
}