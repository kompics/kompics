package se.sics.kompics.wan.job;

import java.util.List;

public class JobLoadRequest extends JobToDummyPom {

	private static final long serialVersionUID = -7970521871667880552L;

	public JobLoadRequest(String groupId, String artifactId, String version, 
			String mainClass, List<String> args, String repoId, String repoUrl)
			throws DummyPomConstructionException {
		super(groupId, artifactId, version, mainClass, args, repoId, repoUrl);
	}
	
	public JobLoadRequest(String groupId, String artifactId, String version, 
			String mainClass, List<String> args)
			throws DummyPomConstructionException {
		this(groupId, artifactId, version, mainClass, args, "", "");
	}
	
	public JobLoadRequest(Job job)
			throws DummyPomConstructionException {
		super(job);
	}
}
