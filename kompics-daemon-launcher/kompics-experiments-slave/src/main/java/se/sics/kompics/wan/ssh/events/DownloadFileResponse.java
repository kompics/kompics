package se.sics.kompics.wan.ssh.events;

import java.io.File;

import se.sics.kompics.Response;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class DownloadFileResponse extends Response {

	private final File file;
	private final int sessionId;
	private final boolean status;
	
	public DownloadFileResponse(DownloadFileRequest request, int sessionId, File file,
			boolean status) {
		super(request);
		this.file = file;
		this.sessionId = sessionId;
		this.status = status;
	}

	public int getSessionId() {
		return sessionId;
	}
	
	public File getFile() {
		return file;
	}

	public boolean isStatus() {
		return status;
	}
}
