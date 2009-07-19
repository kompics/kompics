package se.sics.kompics.wan.daemonmaster;

import se.sics.kompics.Response;


public final class ConnectMasterResponse extends Response {

	private final boolean succeeded;

	public ConnectMasterResponse(ConnectMasterRequest request, boolean succeeded) {
		super(request);
		this.succeeded = succeeded;
	}

	public boolean isSucceeded() {
		return succeeded;
	}
}