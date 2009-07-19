package se.sics.kompics.wan.job;

import java.util.List;

public class JobLoadRequest extends JobToDummyPom {

	private static final long serialVersionUID = -7970521871667880552L;

	private final boolean hideMavenOutput;
	
	public JobLoadRequest(String groupId, String artifactId, String version, 
			String mainClass, List<String> args, String repoId, String repoUrl, boolean hideMavenOutput)
			throws DummyPomConstructionException {
		super(groupId, artifactId, version, mainClass, args, repoId, repoUrl);
		
		this.hideMavenOutput = hideMavenOutput;
	}
	
	public JobLoadRequest(String groupId, String artifactId, String version, 
			String mainClass, List<String> args)
			throws DummyPomConstructionException {
		this(groupId, artifactId, version, mainClass, args, "", "", true);
	}
	
	public JobLoadRequest(Job job, boolean hideMavenOutput)
			throws DummyPomConstructionException {
		super(job);
		this.hideMavenOutput = hideMavenOutput;
	}
	
	public boolean isHideMavenOutput() {
		return hideMavenOutput;
	}
}
