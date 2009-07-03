package se.sics.kompics.wan.master.ssh.events;

import java.io.File;

import se.sics.kompics.Response;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class UploadFileResponse extends Response {

	private final File file;
	private final int sessionId;

	public UploadFileResponse(UploadFileRequest request, int sessionId, File file) {
		super(request);
		this.sessionId = sessionId;
		this.file = file;
	}
	
	public int getSessionId() {
		return sessionId;
	}

	public File getFile() {
		return file;
	}

}
