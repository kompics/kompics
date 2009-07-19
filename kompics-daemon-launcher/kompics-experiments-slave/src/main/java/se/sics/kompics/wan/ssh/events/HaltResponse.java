package se.sics.kompics.wan.ssh.events;

import se.sics.kompics.Response;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class HaltResponse extends Response {

	private final boolean status;

	public HaltResponse(HaltRequest request, boolean status) {
		super(request);
		this.status = status;
	}

	public boolean isStatus() {
		return status;
	}
}
