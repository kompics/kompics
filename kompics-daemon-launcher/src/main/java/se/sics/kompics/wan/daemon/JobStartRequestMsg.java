package se.sics.kompics.wan.daemon;

import se.sics.kompics.address.Address;
import se.sics.kompics.simulator.SimulationScenario;

/**
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class JobStartRequestMsg extends DaemonRequestMessage {

	private static final long serialVersionUID = 17131156452L;

	private final SimulationScenario simulationScenario;
	private final int jobId;
	private final int slaveId;
	private final int numDaemons;
	
	public JobStartRequestMsg(int jobId, int slaveId, int numDaemons, SimulationScenario scenario, Address src, DaemonAddress dest) {
		super(src, dest);
		this.simulationScenario = scenario;
		this.jobId = jobId;
		this.slaveId = slaveId;
		this.numDaemons = numDaemons;
	}
	
	public SimulationScenario getSimulationScenario() {
		return simulationScenario;
	}
	
	public int getJobId() {
		return jobId;
	}
	
	public int getSlaveId() {
		return slaveId;
	}
	
	public int getNumDaemons() {
		return numDaemons;
	}
}