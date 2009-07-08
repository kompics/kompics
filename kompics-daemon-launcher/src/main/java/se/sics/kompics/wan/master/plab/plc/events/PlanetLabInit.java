package se.sics.kompics.wan.master.plab.plc.events;

import se.sics.kompics.Init;
import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.bootstrap.BootstrapConfiguration;
import se.sics.kompics.p2p.monitor.P2pMonitorConfiguration;
import se.sics.kompics.wan.master.plab.PlanetLabCredentials;

/**
 * The <code>PlanetLabInit</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class PlanetLabInit extends Init {

	private PlanetLabCredentials cred;

//	private final BootstrapConfiguration bootConfig;
//	private final P2pMonitorConfiguration monitorConfig;
//	private final Address master;

	public PlanetLabInit(PlanetLabCredentials cred) {
		this.cred = cred;
	}

	/**
	 * @return the cred
	 */
	public PlanetLabCredentials getCred() {
		return cred;
	}
	
}
