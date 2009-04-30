package se.sics.kompics.kdld.daemon;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

/**
 * 
 *  Can include either the jobId or
 *  the jobId and the job description as a maven artifact.
 *  
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class JobStopRequest extends DaemonRequestMessage {

	private static final long serialVersionUID = 1710717688555956452L;

	private String groupId="";
	private String artifactId="";
	private String version="";
	
	private final int jobId;

	public JobStopRequest(int jobId, Address src, DaemonAddress dest) {
		super(src,dest);
		this.jobId = jobId;
	}
	
	public JobStopRequest(int jobId, String groupId, String artifactId, String version, 
			Address src, DaemonAddress dest) {
		this(jobId,src,dest);
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
	}

	public int getJobId() {
		return jobId;
	}
	
	public String getGroupId() {
		return groupId;
	}
	
	public String getArtifactId() {
		return artifactId;
	}
	
	public String getVersion() {
		return version;
	}
}