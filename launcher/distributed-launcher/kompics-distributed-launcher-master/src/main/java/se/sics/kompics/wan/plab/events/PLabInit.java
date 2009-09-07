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

	private final PlanetLabCredentials cred;
	
	private final String xmlRpcServerUrl;

	public PLabInit(PlanetLabCredentials cred, String xmlRpcServerUrl) {
		this.cred = cred;
		this.xmlRpcServerUrl = xmlRpcServerUrl;
	}

	/**
	 * @return the cred
	 */
	public PlanetLabCredentials getCred() {
		return cred;
	}
	
	public String getXmlRpcServerUrl() {
		return xmlRpcServerUrl;
	}
}
