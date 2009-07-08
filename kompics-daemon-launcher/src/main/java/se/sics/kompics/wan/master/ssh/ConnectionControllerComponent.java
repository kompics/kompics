package se.sics.kompics.wan.master.ssh;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.wan.master.plab.PlanetLabCredentials;
import se.sics.kompics.wan.master.plab.plc.PLControllerComponent;
import se.sics.kompics.wan.master.plab.plc.PLControllerInit;

public class ConnectionControllerComponent extends ComponentDefinition {

	Negative<ConnectionControllerPort> ccPort = negative(ConnectionControllerPort.class);
	
	private Component plController;

	private Component sshConnections;


	public ConnectionControllerComponent() {
		plController = create(PLControllerComponent.class);
		sshConnections = create(SshComponent.class);
		
		
		subscribe(handleControllerInit, control);
		subscribe(handleStart, control);
	}

	private Handler<ControllerInit> handleControllerInit = new Handler<ControllerInit>() {
		public void handle(ControllerInit event) {

			PLControllerInit pInit = new PLControllerInit(event.getCredentials());
			trigger(pInit, plController.getControl());
		}
	};

	private Handler<Start> handleStart = new Handler<Start>() {
		public void handle(Start event) {

		}
	};

}
