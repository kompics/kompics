package se.sics.kompics.wan.plab.events;

import se.sics.kompics.Init;
import se.sics.kompics.wan.plab.PlanetLabCredentials;

/**
 * The <code>PlanetLabInit</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class PLabInit extends Init {

	private PlanetLabCredentials cred;

	public PLabInit(PlanetLabCredentials cred) {
		this.cred = cred;
	}

	/**
	 * @return the cred
	 */
	public PlanetLabCredentials getCred() {
		return cred;
	}
	
}
