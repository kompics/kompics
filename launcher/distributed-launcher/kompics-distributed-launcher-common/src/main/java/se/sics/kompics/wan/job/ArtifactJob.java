/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics.wan.job;

import java.io.Serializable;

/**
 *
 * @author jdowling
 */
public class ArtifactJob implements Serializable {

    private static final long serialVersionUID = 74444488L;

    private final String artifactId;
    private final String groupId;
    private final String version;
    private final String repoId;
    private final String repoUrl;
    private final String mainClass;
    private final String args;
    private String port;
    private String webPort;

    public ArtifactJob(String groupId, String artifactId, String version, String repoId, String repoUrl, String mainClass, String args,
            String port, String webPort) {
        this.artifactId = artifactId;
        this.groupId = groupId;
        this.version = version;
        this.repoId = repoId;
        this.repoUrl = repoUrl;
        this.mainClass = mainClass;
        this.args = args;
        this.port = port;
        this.webPort = webPort;
    }

    public ArtifactJob(ArtifactJob artifact) {
        this(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getRepoId(),
                artifact.getRepoUrl(), artifact.getMainClass(), artifact.getArgs(),
                artifact.getPort(), artifact.getWebPort());
    }

    public String getArgs() {
        return args;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getGroupId() {
        return groupId;
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

    public String getVersion() {
        return version;
    }

    public String getPort() {
        return port;
    }

    public String getWebPort() {
        return webPort;
    }

    public void setPort(String port) {
        this.port = port;
        int p = Integer.parseInt(port);
        p += 1000;
        this.webPort = Integer.toString(p);
    }


}
