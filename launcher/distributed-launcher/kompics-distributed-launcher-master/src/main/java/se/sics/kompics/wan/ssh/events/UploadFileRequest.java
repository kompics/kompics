package se.sics.kompics.wan.ssh.events;

import java.io.File;
import java.util.UUID;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class UploadFileRequest extends SshCommandRequest {

	private final File localFileOrDir;

	private final String remotePath;

	private final int sessionId;

	private final double timeout;

	private final boolean recursive;

	private final boolean stopOnError;

	public UploadFileRequest(UUID requestId, int sessionId, File localFileOrDir, String remotePath,
			boolean recursive, double timeout, boolean stopOnError) {
		super(requestId, sessionId, "#upload " + localFileOrDir + " " + remotePath, timeout, stopOnError);

		this.localFileOrDir = localFileOrDir;
		this.remotePath = remotePath;
		this.recursive = recursive;
		this.sessionId = sessionId;
		this.timeout = timeout;
		this.stopOnError = stopOnError;
	}
	
	public UUID getRequestId() {
		return requestId;
	}

	public File getLocalFileOrDir() {
		return localFileOrDir;
	}

	public String getRemotePath() {
		return remotePath;
	}

	public boolean isRecursive() {
		return recursive;
	}

	/**
	 * @return the sessionId
	 */
	public int getSessionId() {
		return sessionId;
	}

	/**
	 * @return the timeout
	 */
	public double getTimeout() {
		return timeout;
	}

	/**
	 * @return the stopOnError
	 */
	public boolean isStopOnError() {
		return stopOnError;
	}

}
