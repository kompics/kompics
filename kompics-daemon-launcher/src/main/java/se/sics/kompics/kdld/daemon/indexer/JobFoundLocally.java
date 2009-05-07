package se.sics.kompics.kdld.daemon.indexer;

import java.util.List;

import se.sics.kompics.kdld.job.DummyPomConstructionException;
import se.sics.kompics.kdld.job.Job;

public class JobFoundLocally extends Job {
	
private static final long serialVersionUID = -9018586643681094901L;

	public JobFoundLocally(int id, String groupId, String artifactId, String version, 
			String mainClass, List<String> args,
			String repoId, String repoUrl)
			throws DummyPomConstructionException {
		super(id, groupId, artifactId, version, mainClass, args, repoId, repoUrl);
	}

	public JobFoundLocally(int id, String groupId, String artifactId, String version, 
			String mainClass, List<String> args)
			throws DummyPomConstructionException {
		this(id, groupId, artifactId, version, mainClass, args, "", "");
	}
}
