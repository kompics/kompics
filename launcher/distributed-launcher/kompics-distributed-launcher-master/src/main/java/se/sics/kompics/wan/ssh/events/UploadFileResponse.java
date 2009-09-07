package se.sics.kompics.wan.ssh.events;

import java.io.File;
import java.util.UUID;

import se.sics.kompics.Response;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class UploadFileResponse extends Response {

	private final File file;
	private final UUID requestId;
	private final int sessionId;
	private final UploadFileRequest request;
	private final boolean success;
	
	public UploadFileResponse(UploadFileRequest request, int sessionId, File file, boolean success) {
		super(request);
		this.requestId = request.getRequestId();
		this.request = request;
		this.sessionId = sessionId;
		this.file = file;
		this.success = success;
	}

	public UUID getRequestId() {
		return requestId;
	}
	
	public int getSessionId() {
		return sessionId;
	}

	public File getFile() {
		return file;
	}

	public UploadFileRequest getRequest() {
		return request;
	}
	
	public boolean isSuccess() {
		return success;
	}
}
