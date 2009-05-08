package se.sics.kompics.kdld.job;

import java.io.Serializable;

import se.sics.kompics.Response;
import se.sics.kompics.kdld.daemon.maven.MavenLauncher.ProcessWrapper;


public class JobExecResponse extends Response implements Serializable {

	private static final long serialVersionUID = 2993973136500802022L;

	public enum Status {
		SUCCESS, FAIL, DUPLICATE
	};

	private final Status status;

	private final int jobId;
	
	public JobExecResponse(JobExec request, int jobId, Status status) {
		super(request);
		this.jobId = jobId;
		this.status = status;
	}

	public int getJobId() {
		return jobId;
	}

	public Status getStatus() {
		return status;
	}

}
