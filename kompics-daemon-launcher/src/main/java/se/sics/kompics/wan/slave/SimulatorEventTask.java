package se.sics.kompics.wan.slave;

import java.util.TimerTask;

import se.sics.kompics.simulator.events.SimulatorEvent;

public final class SimulatorEventTask extends TimerTask {

	private final SimulatorEvent simulatorEvent;
	private final Slave slave;

	public SimulatorEventTask(Slave slave,
			SimulatorEvent simulatorEvent) {
		super();
		this.slave = slave;
		this.simulatorEvent = simulatorEvent;
	}

	@Override
	public void run() {
		slave.handleSimulatorEvent(simulatorEvent);
	}
}
