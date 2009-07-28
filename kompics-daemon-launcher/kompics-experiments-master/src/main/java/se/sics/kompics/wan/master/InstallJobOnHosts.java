package se.sics.kompics.wan.master;

import java.util.List;
import java.util.Set;

import se.sics.kompics.wan.job.Job;
import se.sics.kompics.wan.ssh.Host;

public class InstallJobOnHosts extends Job {

	private static final long serialVersionUID = -4137521813634537953L;
	private final Set<Host> hosts;
	
	private final boolean isHideMavenOutput;

	public InstallJobOnHosts(String groupId, String artifactId, String version, String mainClass,
			List<String> args, String repoId, String repoUrl, 
			boolean	isHideMavenOutput, Set<Host> hosts) {
		super(groupId, artifactId, version, mainClass, args, repoId, repoUrl);
		this.hosts = hosts;
		this.isHideMavenOutput = isHideMavenOutput;
	}

	public InstallJobOnHosts(String groupId, String artifactId, String version, String mainClass,
			List<String> args, boolean	isHideMavenOutput, Set<Host> hosts) {
		super(groupId, artifactId, version, mainClass, args);
		this.hosts = hosts;
		this.isHideMavenOutput = isHideMavenOutput;
	}

	public Set<Host> getHosts() {
		return hosts;
	}
	
	public boolean isHideMavenOutput() {
		return isHideMavenOutput;
	}
}
