package se.sics.kompics.wan.masterdaemon.events;

import se.sics.kompics.address.Address;


/**
 * 
 *  Can include either the jobId or
 *  the jobId and the job description as a maven artifact.
 *  
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class JobStopRequestMsg extends DaemonRequestMsg {

	private static final long serialVersionUID = 17107145855956452L;

	private final int jobId;

	public JobStopRequestMsg(int jobId, Address src, DaemonAddress dest) {
		super(src,dest);
		this.jobId = jobId;
	}


	public int getJobId() {
		return jobId;
	}
	
}