package se.sics.kompics.wan.plab.events;


import se.sics.kompics.Request;

public class GetProgressRequest extends Request {

	private final double progress;
	public GetProgressRequest(double progress) {
		this.progress = progress;
	}
	
	public double getProgress() {
		return progress;
	}
}
