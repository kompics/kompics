package se.sics.kompics.kdld.job;

import java.util.List;

public class JobFoundLocally extends Job {
	
private static final long serialVersionUID = -9018586643681094901L;

	public JobFoundLocally(int id, String repoId, String repoUrl, String repoName, String groupId,
			String artifactId, String version, String mainClass, List<String> args)
			throws DummyPomConstructionException {
		super(id, repoId, repoUrl, repoName, groupId, artifactId, version, mainClass, args);
	}

}
