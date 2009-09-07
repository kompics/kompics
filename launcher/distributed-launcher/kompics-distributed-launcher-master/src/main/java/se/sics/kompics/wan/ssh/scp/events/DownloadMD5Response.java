package se.sics.kompics.wan.ssh.scp.events;

import java.util.UUID;

import se.sics.kompics.Response;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class DownloadMD5Response extends Response {

	private final UUID requestId;
	private final boolean status;
	private final int commandId;

	public DownloadMD5Response(DownloadMD5Request request, int commandId, 
			boolean status) {
		super(request);
		this.requestId = request.getRequestId();
		this.commandId = commandId;
		this.status = status;
	}
	
	public UUID getRequestId() {
		return requestId;
	}
	
	public int getCommandId() {
		return commandId;
	}

	/**
	 * @return the stopOnError
	 */
	public boolean isStatus() {
		return status;
	}

}
