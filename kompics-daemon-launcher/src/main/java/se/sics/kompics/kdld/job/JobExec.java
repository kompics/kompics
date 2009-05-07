package se.sics.kompics.kdld.job;

import java.util.List;

import se.sics.kompics.simulator.SimulationScenario;

public class JobExec extends Job {

	private static final long serialVersionUID = 8639095227105335182L;

	private final SimulationScenario scenario;
	
	public JobExec(int id, String groupId, String artifactId, String version, String mainClass, 
			List<String> args, SimulationScenario scenario, 
			String repoId, String repoUrl) {
		super(id, groupId, artifactId, version, mainClass, args, repoId, repoUrl);
		this.scenario = scenario;
	}
	
	public JobExec(int id, String groupId, String artifactId, String version, String mainClass, 
			List<String> args, SimulationScenario scenario) {
		super(id, groupId, artifactId, version, mainClass, args);
		this.scenario = scenario;
	}
	
	public JobExec(Job job, SimulationScenario scenario) {
		this(job.getId(), job.getGroupId(), job.getArtifactId(), job.getVersion(), 
				job.getMainClass(),	job.getArgs(),scenario, job.getRepoId(), job.getRepoUrl());
	}

	public SimulationScenario getScenario() {
		return scenario;
	}

}
