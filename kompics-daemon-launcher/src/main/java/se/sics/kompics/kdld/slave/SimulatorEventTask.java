package se.sics.kompics.kdld.slave;

import java.util.TimerTask;

import se.sics.kompics.simulator.events.SimulatorEvent;

public final class SimulatorEventTask extends TimerTask {

	private final SimulatorEvent simulatorEvent;
	private final Slave master;

	public SimulatorEventTask(Slave master,
			SimulatorEvent simulatorEvent) {
		super();
		this.master = master;
		this.simulatorEvent = simulatorEvent;
	}

	@Override
	public void run() {
		master.handleSimulatorEvent(simulatorEvent);
	}
}
