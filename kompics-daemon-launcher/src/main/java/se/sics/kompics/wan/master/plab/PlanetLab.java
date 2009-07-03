package se.sics.kompics.wan.master.plab;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.wan.master.plab.events.GetRunningPlanetLabHostsRequest;

public class PlanetLab extends ComponentDefinition {
	private Negative<Network> net = negative(Network.class);
	Positive<Timer> timer = positive(Timer.class);

	public PlanetLab() {

		subscribe(handleGetRunningPlanetLabHosts, net);
	}
	
	
	private Handler<GetRunningPlanetLabHostsRequest> handleGetRunningPlanetLabHosts = 
		new Handler<GetRunningPlanetLabHostsRequest>() {
		public void handle(GetRunningPlanetLabHostsRequest event) {

			// Use XML-RPC interface to Co-Mon to get the status of executing hosts.
		}
	};
}
