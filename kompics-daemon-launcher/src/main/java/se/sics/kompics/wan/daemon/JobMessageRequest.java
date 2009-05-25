package se.sics.kompics.wan.daemon;

import se.sics.kompics.address.Address;

/**
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: 
 */
public class JobMessageRequest extends DaemonRequestMessage {


	private static final long serialVersionUID = 1977578506786380785L;

	private final int jobId;

	private final String msg;


	public JobMessageRequest(int jobId, String msg, Address source, DaemonAddress destination) {
		super(source, destination);
		this.jobId = jobId;
		this.msg = msg;
	}

	public String getMsg() {
		return msg;
	}

	public int getJobId() {
		return jobId;
	}
}