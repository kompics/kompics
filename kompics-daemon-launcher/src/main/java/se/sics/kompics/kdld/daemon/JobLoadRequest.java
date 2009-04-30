package se.sics.kompics.kdld.daemon;

import java.util.List;

import se.sics.kompics.address.Address;

/**
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: DeployRequest.java
 */
public class JobLoadRequest extends DaemonRequestMessage {

	private static final long serialVersionUID = 1710436546452L;

	private final String repoId;
	private final String repoUrl;
	private final String repoName;
	
	private final String groupId;
	private final String artifactId;
	private final String version;
	private final String mainClass;
	
	private final List<String> args;
	
	private final int jobId;
	

	public JobLoadRequest(int jobId, 
			String repoId, String repoUrl, String repoName,
			String groupId, String artifactId, String version,
			String mainClass, List<String> args, Address src, DaemonAddress dest) {
		super(src,dest);
		this.repoId =  repoId;
		this.repoUrl = repoUrl;
		this.repoName = repoName;
		this.jobId = jobId;
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.mainClass = mainClass;
		this.args = args;
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

	public String getMainClass() {
		return mainClass;
	}
	
	public String getRepoId() {
		return repoId;
	}
	public String getRepoUrl() {
		return repoUrl;
	}
	public String getRepoName() {
		return repoName;
	}
	public List<String> getArgs() {
		return args;
	}
}