package se.sics.kompics.wan.plab.events;

import se.sics.kompics.Request;
import se.sics.kompics.wan.plab.PlanetLabCredentials;

public class QueryPLabSitesRequest extends Request {

	private final PlanetLabCredentials cred;

	public QueryPLabSitesRequest(PlanetLabCredentials cred) {
		this.cred = cred;
	}

	/**
	 * @return the cred
	 */
	public PlanetLabCredentials getCred() {
		return cred;
	}
}
