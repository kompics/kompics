package se.sics.kompics.wan.job;

import java.util.List;

import se.sics.kompics.simulator.SimulationScenario;

public class JobStartRequest extends Job {

	private static final long serialVersionUID = 8639095227105335182L;

	private final SimulationScenario scenario;
	
	public JobStartRequest(String groupId, String artifactId, String version, String mainClass, 
			List<String> args, SimulationScenario scenario, 
			String repoId, String repoUrl) {
		super(groupId, artifactId, version, mainClass, args, repoId, repoUrl);
		this.scenario = scenario;
	}
	
	public JobStartRequest(String groupId, String artifactId, String version, String mainClass, 
			List<String> args, SimulationScenario scenario) {
		super(groupId, artifactId, version, mainClass, args);
		this.scenario = scenario;
	}
	
	public JobStartRequest(Job job, SimulationScenario scenario) {
		this(job.getGroupId(), job.getArtifactId(), job.getVersion(), 
				job.getMainClass(),	job.getArgs(), scenario, job.getRepoId(), job.getRepoUrl());
	}

	public SimulationScenario getScenario() {
		return scenario;
	}

}
