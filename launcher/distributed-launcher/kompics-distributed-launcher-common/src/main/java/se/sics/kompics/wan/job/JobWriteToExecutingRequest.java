package se.sics.kompics.wan.job;

import se.sics.kompics.Request;
import se.sics.kompics.wan.masterdaemon.events.JobWriteToExecutingRequestMsg;

/**
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: 
 */
public class JobWriteToExecutingRequest extends Request {


	private final int jobId;
	private final String msg;

	public JobWriteToExecutingRequest(int jobId, String msg) {
		this.jobId = jobId;
		this.msg = msg;
	}
	
	public JobWriteToExecutingRequest(JobWriteToExecutingRequestMsg msg)
	{
		this.jobId = msg.getJobId();
		this.msg = msg.getMsg();
	}

	public int getJobId() {
		return jobId;
	}
	
	public String getMsg() {
		return msg;
	}
}