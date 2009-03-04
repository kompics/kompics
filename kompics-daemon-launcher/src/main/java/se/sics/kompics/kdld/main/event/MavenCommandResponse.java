
package se.sics.kompics.kdld.main.event;

import se.sics.kompics.Event;

/**
 * @author jdowling
 *
 */
public class MavenCommandResponse extends Event {

	private final String msg;
	
	public enum Status { SUCCESS, FAIL };
	
	private Status status;
	
	public MavenCommandResponse(Status status, String msg) {
		this.status = status;
		this.msg = msg;
	}

	public String getMsg() {
		return msg;
	}
	
	public Status getStatus() {
		return status;
	}
}