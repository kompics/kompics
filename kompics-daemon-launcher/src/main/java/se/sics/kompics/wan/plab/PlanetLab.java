package se.sics.kompics.wan.plab;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

public class PlanetLab extends ComponentDefinition {
	private Negative<Network> net = negative(Network.class);
	Positive<Timer> timer = positive(Timer.class);

	public PlanetLab() {

		subscribe(handleGetRunningPlanetLabHosts, net);
	}
	
	
	private Handler<GetRunningPlanetLabHosts> handleGetRunningPlanetLabHosts = 
		new Handler<GetRunningPlanetLabHosts>() {
		public void handle(GetRunningPlanetLabHosts event) {

			// Use XML-RPC interface to Co-Mon to get the status of executing hosts.
		}
	};
}
