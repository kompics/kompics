package se.sics.kompics.wan.plab.events;


import se.sics.kompics.Request;
import se.sics.kompics.wan.plab.PlanetLabCredentials;

public class GetHostsInSliceRequest extends Request {
	
	private final PlanetLabCredentials cred;
	private final boolean forceDownload;
	
	public GetHostsInSliceRequest(PlanetLabCredentials cred, boolean forceDownload) {
		this.cred = cred;
		this.forceDownload = forceDownload;
	}
	
	/**
	 * @return the cred
	 */
	public PlanetLabCredentials getCred() {
		return cred;
	}
	
	public boolean isForceDownload() {
		return forceDownload;
	}
}
