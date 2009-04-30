package se.sics.kompics.kdld.slave;

import se.sics.kompics.Init;
import se.sics.kompics.p2p.simulator.NetworkModel;
import se.sics.kompics.simulator.SimulationScenario;

public final class SlaveInit extends Init {

	private final SimulationScenario scenario;
	private final NetworkModel networkModel;

	public SlaveInit(SimulationScenario scenario,
			NetworkModel networkModel) {
		this.scenario = scenario;
		this.networkModel = networkModel;
	}

	public final SimulationScenario getScenario() {
		return scenario;
	}

	public final NetworkModel getNetworkModel() {
		return networkModel;
	}
}
