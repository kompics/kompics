package se.sics.kompics.wan.master.plab.plc;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Start;
import se.sics.kompics.wan.plab.PlanetLabCredentials;

public class PLControllerComponent extends ComponentDefinition {

	Negative<PLControllerPort> plPort = negative(PLControllerPort.class);
	
	private PlanetLabCredentials cred;

	public PLControllerComponent() {
		
		subscribe(handlePLControllerInit, control);
		subscribe(handleStart, control);
	}

	private Handler<PLControllerInit> handlePLControllerInit = new Handler<PLControllerInit>() {
		public void handle(PLControllerInit event) {
			cred = event.getCredentials();
		}
	};

	private Handler<Start> handleStart = new Handler<Start>() {
		public void handle(Start event) {

		}
	};

}
