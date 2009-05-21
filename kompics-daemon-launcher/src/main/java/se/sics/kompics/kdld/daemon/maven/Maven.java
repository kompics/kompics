package se.sics.kompics.kdld.daemon.maven;

import se.sics.kompics.PortType;
import se.sics.kompics.kdld.job.JobExited;
import se.sics.kompics.kdld.job.JobLoadRequest;
import se.sics.kompics.kdld.job.JobLoadResponse;
import se.sics.kompics.kdld.job.JobReadFromExecutingRequest;
import se.sics.kompics.kdld.job.JobReadFromExecutingResponse;
import se.sics.kompics.kdld.job.JobRemoveRequest;
import se.sics.kompics.kdld.job.JobRemoveResponse;
import se.sics.kompics.kdld.job.JobStartRequest;
import se.sics.kompics.kdld.job.JobStartResponse;
import se.sics.kompics.kdld.job.JobStopRequest;
import se.sics.kompics.kdld.job.JobStopResponse;


/**
 * The <code>Maven</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class Maven extends PortType {

	{
		negative(JobLoadRequest.class);
		negative(JobStartRequest.class);
		negative(JobStopRequest.class);
		negative(JobReadFromExecutingRequest.class);
		negative(JobRemoveRequest.class);
		
		positive(JobStopResponse.class);
		positive(JobLoadResponse.class);
		positive(JobStartResponse.class);
		positive(JobExited.class);
		positive(JobReadFromExecutingResponse.class);
		positive(JobRemoveResponse.class);
	}
}
