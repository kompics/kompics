package se.sics.kompics.kdld.job;

import java.util.List;

public class JobAssembly extends JobToDummyPom {

	private static final long serialVersionUID = -7970521871667880552L;

	public JobAssembly(int id, String groupId, String artifactId, String version, 
			String mainClass, List<String> args, String repoId, String repoUrl)
			throws DummyPomConstructionException {
		super(id, groupId, artifactId, version, mainClass, args, repoId, repoUrl);
	}
	
	public JobAssembly(int id, String groupId, String artifactId, String version, 
			String mainClass, List<String> args)
			throws DummyPomConstructionException {
		this(id, groupId, artifactId, version, mainClass, args, "", "");
	}
	
	public JobAssembly(Job job)
			throws DummyPomConstructionException {
		super(job);
	}
}
