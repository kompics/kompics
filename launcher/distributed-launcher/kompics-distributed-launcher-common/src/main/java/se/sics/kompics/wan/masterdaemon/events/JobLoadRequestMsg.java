package se.sics.kompics.wan.masterdaemon.events;

import java.util.List;

import se.sics.kompics.address.Address;
import se.sics.kompics.wan.job.Job;
import se.sics.kompics.wan.util.PomUtils;

/**
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: DeployRequest.java
 */
public class JobLoadRequestMsg extends DaemonRequestMsg {

	private static final long serialVersionUID = 1710436546452L;

	private final String groupId;
	private final String artifactId;
	private final String version;

	private final String repoId;
	private final String repoUrl;

	private final String mainClass;
	
	private final List<String> args;
	
	private final int jobId;
	
	private final boolean hideMavenOutput;

	public JobLoadRequestMsg(String groupId, String artifactId, String version,
			String repoId, String repoUrl, 
			String mainClass, List<String> args, boolean hideMavenOutput, 
			Address src, DaemonAddress dest) {
		super(src,dest);
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.repoId =  repoId;
		this.repoUrl = repoUrl;
		this.mainClass = mainClass;
		this.args = args;
		this.jobId = hashCode();
		this.hideMavenOutput = hideMavenOutput;
	}

	public JobLoadRequestMsg(Job job, boolean hideMavenOutput, Address src, DaemonAddress dest) {
		super(src,dest);
		this.groupId = job.getGroupId();
		this.artifactId = job.getArtifactId();
		this.version = job.getVersion();
		this.repoId =  job.getRepoId();
		this.repoUrl = job.getRepoUrl();
		this.mainClass = job.getMainClass();
		this.args = job.getArgs();
		this.jobId = hashCode();
		this.hideMavenOutput = hideMavenOutput;
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
	public List<String> getArgs() {
		return args;
	}
	
	@Override
	public int hashCode() {
		return PomUtils.generateJobId(groupId, artifactId, version);
	}
	
	public boolean isHideMavenOutput() {
		return hideMavenOutput;
	}
}