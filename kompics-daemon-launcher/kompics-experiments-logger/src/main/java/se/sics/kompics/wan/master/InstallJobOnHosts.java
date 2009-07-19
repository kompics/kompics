package se.sics.kompics.wan.master;

import java.util.List;
import java.util.TreeSet;

import se.sics.kompics.address.Address;
import se.sics.kompics.wan.job.Job;

public class InstallJobOnHosts extends Job {

	private static final long serialVersionUID = -4137521813634537953L;
	private final TreeSet<Address> hosts;
	
	private final boolean isHideMavenOutput;

	public InstallJobOnHosts(String groupId, String artifactId, String version, String mainClass,
			List<String> args, String repoId, String repoUrl, 
			boolean	isHideMavenOutput, TreeSet<Address> hosts) {
		super(groupId, artifactId, version, mainClass, args, repoId, repoUrl);
		this.hosts = hosts;
		this.isHideMavenOutput = isHideMavenOutput;
	}

	public InstallJobOnHosts(String groupId, String artifactId, String version, String mainClass,
			List<String> args, boolean	isHideMavenOutput, TreeSet<Address> hosts) {
		super(groupId, artifactId, version, mainClass, args);
		this.hosts = hosts;
		this.isHideMavenOutput = isHideMavenOutput;
	}

	public TreeSet<Address> getHosts() {
		return hosts;
	}
	
	public boolean isHideMavenOutput() {
		return isHideMavenOutput;
	}
}
