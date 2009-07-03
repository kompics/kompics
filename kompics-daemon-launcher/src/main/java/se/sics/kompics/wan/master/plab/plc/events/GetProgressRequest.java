package se.sics.kompics.wan.master.plab.plc.events;


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
