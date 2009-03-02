package se.sics.kompics.kdld.main.event;

import se.sics.kompics.Event;

public class LaunchResponse extends Event {

	public enum Status { SUCCESS, FAIL };
	
	private final Status status;
	
	private String msg;
 
	
	public LaunchResponse(Status status) {
		this.status = status;
	}
	
	public LaunchResponse(Status status, String msg) {
		this(status);
		this.msg = msg;
	}

	public Status getStatus() {
		return status;
	}
	
	public String getMsg() {
		return msg;
	}
		
}
