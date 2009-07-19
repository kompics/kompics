package se.sics.kompics.wan.plab.events;


import se.sics.kompics.Request;
import se.sics.kompics.wan.plab.PlanetLabCredentials;

public class GetHostsNotInSliceRequest extends Request {
	
	private final PlanetLabCredentials cred;
	
	private final boolean running;
	
	public GetHostsNotInSliceRequest(PlanetLabCredentials cred, boolean running) {
		this.cred = cred;
		this.running = running;
	}
	
	/**
	 * @return the cred
	 */
	public PlanetLabCredentials getCred() {
		return cred;
	}
	
	public boolean isRunning() {
		return running;
	}
}
