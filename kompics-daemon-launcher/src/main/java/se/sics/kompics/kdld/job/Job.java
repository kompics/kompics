package se.sics.kompics.kdld.job;

import java.io.Serializable;
import java.util.List;

import se.sics.kompics.Request;

public abstract class Job extends Request implements Serializable {

	private static final long serialVersionUID = 3831799496529156008L;

	protected final int id;

	protected final String repoId;
	protected final String repoUrl;
	protected final String repoName;

	protected final String groupId;
	protected final String artifactId;
	protected final String version;

	protected final String mainClass;
	protected final List<String> args;


	public Job(int id, String repoId, String repoUrl, String repoName, String groupId,
			String artifactId, String version, String mainClass, List<String> args)
			throws DummyPomConstructionException {
		this.id = id;
		this.repoId = repoId;
		this.repoUrl = repoUrl;
		this.repoName = repoName;
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.mainClass = mainClass;
		this.args = args;

	}

	public int getId() {
		return id;
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

	public String getRepoId() {
		return repoId;
	}

	public String getRepoUrl() {
		return repoUrl;
	}

	public String getRepoName() {
		return repoName;
	}

	public String getMainClass() {
		return mainClass;
	}

	public List<String> getArgs() {
		return args;
	}

	public String[] getArgsAsArray() {
		return args.toArray(new String[args.size()]);
	}
}
