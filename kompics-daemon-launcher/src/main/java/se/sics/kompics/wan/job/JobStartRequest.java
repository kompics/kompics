package se.sics.kompics.wan.job;

import java.util.List;

import se.sics.kompics.simulator.SimulationScenario;

public class JobStartRequest extends Job {

	private static final long serialVersionUID = 8638765443105335182L;

	private final SimulationScenario scenario;
	
	private final int numPeers;
	
	public JobStartRequest(int numDaemons, String groupId, String artifactId, String version, String mainClass, 
			List<String> args, SimulationScenario scenario, 
			String repoId, String repoUrl) {
		super(groupId, artifactId, version, mainClass, args, repoId, repoUrl);
		this.scenario = scenario;
		this.numPeers = numDaemons;
	}
	
	public JobStartRequest(int numDaemons, String groupId, String artifactId, String version, String mainClass, 
			List<String> args, SimulationScenario scenario) {
		super(groupId, artifactId, version, mainClass, args);
		this.scenario = scenario;
		this.numPeers = numDaemons;
	}
	
	public JobStartRequest(int numPeers, Job job, SimulationScenario scenario) {
		this(numPeers, job.getGroupId(), job.getArtifactId(), job.getVersion(), 
				job.getMainClass(),	job.getArgs(), scenario, job.getRepoId(), job.getRepoUrl());
	}

	public SimulationScenario getScenario() {
		return scenario;
	}

	public int getNumPeers() {
		return numPeers;
	}
	
	
}
