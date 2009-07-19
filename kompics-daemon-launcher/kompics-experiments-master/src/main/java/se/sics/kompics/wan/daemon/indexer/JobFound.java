package se.sics.kompics.wan.daemon.indexer;

import java.util.List;

import se.sics.kompics.wan.job.DummyPomConstructionException;
import se.sics.kompics.wan.job.Job;

public class JobFound extends Job {
	
private static final long serialVersionUID = -9018586643681094901L;

	public JobFound(String groupId, String artifactId, String version, 
			String mainClass, List<String> args,
			String repoId, String repoUrl)
			throws DummyPomConstructionException {
		super(groupId, artifactId, version, mainClass, args, repoId, repoUrl);
	}

	public JobFound(String groupId, String artifactId, String version, 
			String mainClass, List<String> args)
			throws DummyPomConstructionException {
		this(groupId, artifactId, version, mainClass, args, "", "");
	}
}
