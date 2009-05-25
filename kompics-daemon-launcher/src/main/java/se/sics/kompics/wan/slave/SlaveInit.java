package se.sics.kompics.wan.slave;

import se.sics.kompics.Init;
import se.sics.kompics.p2p.simulator.NetworkModel;
import se.sics.kompics.simulator.SimulationScenario;

public final class SlaveInit extends Init {

	private final SimulationScenario scenario;
	private final NetworkModel networkModel;
	private final int daemonId;
	
	public SlaveInit(int daemonId, SimulationScenario scenario,
			NetworkModel networkModel) {
		this.daemonId = daemonId;
		this.scenario = scenario;
		this.networkModel = networkModel;
	}
	
	public int getDaemonId() {
		return daemonId;
	}

	public final SimulationScenario getScenario() {
		return scenario;
	}

	public final NetworkModel getNetworkModel() {
		return networkModel;
	}
}
