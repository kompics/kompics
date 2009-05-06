package se.sics.kompics.kdld.daemon.indexer;

import se.sics.kompics.Response;

public class IndexStop extends Response {

	public IndexStop(IndexStart request) {
		super(request);
	}

}
