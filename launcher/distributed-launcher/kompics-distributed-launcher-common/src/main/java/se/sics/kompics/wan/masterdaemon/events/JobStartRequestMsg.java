package se.sics.kompics.wan.masterdaemon.events;

import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;


/**
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class JobStartRequestMsg extends DaemonRequestMsg {

	private static final long serialVersionUID = 17131156452L;

	private final SimulationScenario simulationScenario;
	private final int jobId;
	private final int numPeers;
	
	public JobStartRequestMsg(int jobId, int numPeers, SimulationScenario scenario, Address src, DaemonAddress dest) {
		super(src, dest);
		this.simulationScenario = scenario;
		this.jobId = jobId;
		this.numPeers = numPeers;
	}
	
	public SimulationScenario getSimulationScenario() {
		return simulationScenario;
	}
	
	public int getJobId() {
		return jobId;
	}
	
	public int getNumPeers() {
		return numPeers;
	}
}