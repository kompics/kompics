package se.sics.kompics.wan.masterdaemon.events;

import se.sics.kompics.address.Address;


/**
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class JobReadFromExecutingRequestMsg extends DaemonRequestMsg {

	private static final long serialVersionUID = 6828968658535729513L;
	
	private final int jobId;
	
	public JobReadFromExecutingRequestMsg(int jobId, Address src, DaemonAddress dest) {
		super(src, dest);
		this.jobId = jobId;
	}
	
	public int getJobId() {
		return jobId;
	}
}