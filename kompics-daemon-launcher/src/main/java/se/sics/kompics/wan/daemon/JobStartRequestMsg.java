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
	private final int id;
	
	public JobStartRequestMsg(int id, SimulationScenario scenario, Address src, DaemonAddress dest) {
		super(src, dest);
		this.simulationScenario = scenario;
		this.id = id;
	}
	
	public SimulationScenario getSimulationScenario() {
		return simulationScenario;
	}
	
	public int getId() {
		return id;
	}
}