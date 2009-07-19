package se.sics.kompics.wan.plab.events;

import se.sics.kompics.Response;

public class GetProgressResponse extends Response {

	private final double progress;

	public GetProgressResponse(GetProgressRequest request, double progress) {
		super(request);
		this.progress = progress;
	}

	public double getProgress() {
		return progress;
	}
}
