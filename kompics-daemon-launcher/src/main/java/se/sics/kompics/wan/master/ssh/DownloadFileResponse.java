package se.sics.kompics.wan.master.ssh;

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
	
	public DownloadFileResponse(DownloadFileRequest request, int sessionId, File file) {
		super(request);
		this.file = file;
		this.sessionId = sessionId;
	}

	public int getSessionId() {
		return sessionId;
	}
	
	public File getFile() {
		return file;
	}

}
