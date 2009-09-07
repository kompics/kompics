package se.sics.kompics.wan.master.events;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.sics.kompics.wan.job.ArtifactJob;
import se.sics.kompics.wan.job.Job;
import se.sics.kompics.wan.ssh.ExperimentHost;
import se.sics.kompics.wan.ssh.Host;

public class InstallJobOnHostsRequest extends Job {

    private static final long serialVersionUID = -4137521813634537953L;
    private final Set<Host> hosts;
    private final boolean isHideMavenOutput;

    public InstallJobOnHostsRequest(String groupId, String artifactId, String version, String mainClass,
            List<String> args, String repoId, String repoUrl,
            boolean isHideMavenOutput, Set<Host> hosts) {
        super(groupId, artifactId, version, mainClass, args, repoId, repoUrl);
        this.hosts = hosts;
        this.isHideMavenOutput = isHideMavenOutput;
    }

    public InstallJobOnHostsRequest(String groupId, String artifactId, String version, String mainClass,
            List<String> args, boolean isHideMavenOutput, Set<Host> hosts) {
        super(groupId, artifactId, version, mainClass, args);
        this.hosts = hosts;
        this.isHideMavenOutput = isHideMavenOutput;
    }

    public InstallJobOnHostsRequest(ArtifactJob artifact, String host) {
        super(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                artifact.getMainClass(), Arrays.asList(artifact.getArgs().split(" ")));
        this.hosts = new HashSet<Host>();
        this.hosts.add(new ExperimentHost(host));
        this.isHideMavenOutput = true;
    }

    public Set<Host> getHosts() {
        return hosts;
    }

    public boolean isHideMavenOutput() {
        return isHideMavenOutput;
    }
}
