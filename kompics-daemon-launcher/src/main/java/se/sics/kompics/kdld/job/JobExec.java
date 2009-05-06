package se.sics.kompics.kdld.job;

import java.util.List;

import se.sics.kompics.simulator.SimulationScenario;

public class JobExec extends Job {

	private static final long serialVersionUID = 8639095227105335182L;

	private final SimulationScenario scenario;
	
	public JobExec(int id, String repoId, String repoUrl, String repoName, String groupId,
			String artifactId, String version, String mainClass, List<String> args,
			SimulationScenario scenario) {
		super(id, repoId, repoUrl, repoName, groupId, artifactId, version, mainClass, args);
		this.scenario = scenario;
	}
	
	public JobExec(Job job, SimulationScenario scenario) {
		this(job.getId(), job.getRepoId(), job.getRepoUrl(), job.getRepoName(), 
				job.getGroupId(), job.getArtifactId(), job.getVersion(), job.getMainClass(), 
				job.getArgs(),scenario);
	}

	public SimulationScenario getScenario() {
		return scenario;
	}

}
