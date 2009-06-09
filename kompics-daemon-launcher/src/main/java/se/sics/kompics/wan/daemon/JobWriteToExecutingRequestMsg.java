package se.sics.kompics.wan.daemon;

import se.sics.kompics.address.Address;


/**
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class JobWriteToExecutingRequestMsg extends DaemonRequestMessage {


	private static final long serialVersionUID = 4842615637331837248L;
	
	private final int jobId;
	private final String msg;
	
	public JobWriteToExecutingRequestMsg(int jobId, String msg, Address src, DaemonAddress dest) {
		super(src, dest);
		this.jobId = jobId;
		this.msg = msg;
	}
	
	public int getJobId() {
		return jobId;
	}
	
	public String getMsg() {
		return msg;
	}
}