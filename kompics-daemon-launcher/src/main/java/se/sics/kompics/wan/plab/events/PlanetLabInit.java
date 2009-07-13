package se.sics.kompics.wan.plab.events;

import se.sics.kompics.Init;
import se.sics.kompics.wan.plab.PlanetLabCredentials;

/**
 * The <code>PlanetLabInit</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class PlanetLabInit extends Init {

	private PlanetLabCredentials cred;

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
