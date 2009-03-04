package se.sics.kompics.kdld.main.event;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

public class LaunchResponse extends Message {

	public enum Status { SUCCESS, FAIL };
	
	private final Status status;
	
	private String msg;
 
	
	public LaunchResponse(Status status, Address src, Address dest) {
		super(src, dest);
		this.status = status;
	}
	
	public LaunchResponse(Status status, String msg, Address src, Address dest) {
		this(status, src, dest);
		this.msg = msg;
	}

	public Status getStatus() {
		return status;
	}
	
	public String getMsg() {
		return msg;
	}
		
}
