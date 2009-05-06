package se.sics.kompics.kdld.job;

import java.util.List;

public class JobAssembly extends JobToDummyPom {

	private static final long serialVersionUID = -7970521871667880552L;

	public JobAssembly(int id, String repoId, String repoUrl, String repoName, String groupId,
			String artifactId, String version, String mainClass, List<String> args)
			throws DummyPomConstructionException {
		super(id, repoId, repoUrl, repoName, groupId, artifactId, version, mainClass, args);
	}
	
	public JobAssembly(Job job)
			throws DummyPomConstructionException {
		super(job);
	}
}
