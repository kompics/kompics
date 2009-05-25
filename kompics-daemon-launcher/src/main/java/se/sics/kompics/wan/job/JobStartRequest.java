package se.sics.kompics.wan.job;

import java.util.List;

import se.sics.kompics.simulator.SimulationScenario;

public class JobStartRequest extends Job {

	private static final long serialVersionUID = 8638765443105335182L;

	private final SimulationScenario scenario;
	
	private final int slaveId;
	private final int numSlaves;
	
	public JobStartRequest(int slaveId, int numDaemons, String groupId, String artifactId, String version, String mainClass, 
			List<String> args, SimulationScenario scenario, 
			String repoId, String repoUrl) {
		super(groupId, artifactId, version, mainClass, args, repoId, repoUrl);
		this.scenario = scenario;
		this.slaveId = slaveId;
		this.numSlaves = numDaemons;
	}
	
	public JobStartRequest(int slaveId, int numDaemons, String groupId, String artifactId, String version, String mainClass, 
			List<String> args, SimulationScenario scenario) {
		super(groupId, artifactId, version, mainClass, args);
		this.scenario = scenario;
		this.slaveId = slaveId;
		this.numSlaves = numDaemons;
	}
	
	public JobStartRequest(int slaveId, int numDaemons, Job job, SimulationScenario scenario) {
		this(slaveId, numDaemons, job.getGroupId(), job.getArtifactId(), job.getVersion(), 
				job.getMainClass(),	job.getArgs(), scenario, job.getRepoId(), job.getRepoUrl());
	}

	public SimulationScenario getScenario() {
		return scenario;
	}

	public int getNumSlaves() {
		return numSlaves;
	}
	
	public int getSlaveId() {
		return slaveId;
	}
	
}
