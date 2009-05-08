package se.sics.kompics.kdld.daemon.maven;

import se.sics.kompics.PortType;
import se.sics.kompics.kdld.daemon.JobStopRequest;
import se.sics.kompics.kdld.job.JobAssembly;
import se.sics.kompics.kdld.job.JobAssemblyResponse;
import se.sics.kompics.kdld.job.JobExec;
import se.sics.kompics.kdld.job.JobExecResponse;
import se.sics.kompics.kdld.job.JobExited;
import se.sics.kompics.kdld.job.JobReadFromExecuting;
import se.sics.kompics.kdld.job.JobReadFromExecutingResponse;


/**
 * The <code>Maven</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class Maven extends PortType {

	{
		negative(JobAssembly.class);
		negative(JobExec.class);
		negative(JobStopRequest.class);
		negative(JobReadFromExecuting.class);

		positive(JobAssemblyResponse.class);
		positive(JobExecResponse.class);
		positive(JobExited.class);
		positive(JobReadFromExecutingResponse.class);
	}
}
