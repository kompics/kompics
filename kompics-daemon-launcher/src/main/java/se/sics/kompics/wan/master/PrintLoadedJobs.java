package se.sics.kompics.wan.master;

import se.sics.kompics.Request;

public class PrintLoadedJobs extends Request {

	private final String host;

	public PrintLoadedJobs(String host) {
		this.host = host;
	}

	public String getHost() {
		return host;
	}
}
