package se.sics.kompics.wan.daemon.maven;

import se.sics.kompics.PortType;
import se.sics.kompics.wan.job.JobExited;
import se.sics.kompics.wan.job.JobLoadRequest;
import se.sics.kompics.wan.job.JobLoadResponse;
import se.sics.kompics.wan.job.JobReadFromExecutingRequest;
import se.sics.kompics.wan.job.JobReadFromExecutingResponse;
import se.sics.kompics.wan.job.JobRemoveRequest;
import se.sics.kompics.wan.job.JobRemoveResponse;
import se.sics.kompics.wan.job.JobStartRequest;
import se.sics.kompics.wan.job.JobStartResponse;
import se.sics.kompics.wan.job.JobStopRequest;
import se.sics.kompics.wan.job.JobStopResponse;
import se.sics.kompics.wan.job.JobWriteToExecutingRequest;


/**
 * The <code>Maven</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class MavenPort extends PortType {

	{
		negative(JobLoadRequest.class);
		negative(JobStartRequest.class);
		negative(JobStopRequest.class);
		negative(JobReadFromExecutingRequest.class);		
		negative(JobRemoveRequest.class);
		negative(JobWriteToExecutingRequest.class);
		
		positive(JobStopResponse.class);
		positive(JobLoadResponse.class);
		positive(JobStartResponse.class);
		positive(JobExited.class);
		positive(JobReadFromExecutingResponse.class);
		positive(JobRemoveResponse.class);
		positive(JobReadFromExecutingResponse.class);
	}
}
