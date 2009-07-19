package se.sics.kompics.wan.ssh.scp.events;

import se.sics.kompics.Response;
import se.sics.kompics.wan.ssh.CommandSpec;
import se.sics.kompics.wan.ssh.scp.FileInfo;
import ch.ethz.ssh2.SCPClient;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class ScpPutFileResponse extends Response {

//	private final int sessionId;
	private final FileInfo fileInfo;
	private final CommandSpec commandSpec;
	private final SCPClient scpClient;

	
	public ScpPutFileResponse(ScpPutFileRequest request, FileInfo fileInfo, 
			CommandSpec commandSpec, SCPClient scpClient) {
		super(request);
//		this.sessionId = sessionId;
		this.fileInfo = fileInfo;
		this.commandSpec = commandSpec;
		this.scpClient = scpClient;
	}

	public FileInfo getFileInfo() {
		return fileInfo;
	}
	
//	public int getSessionId() {
//		return sessionId;
//	}

	public CommandSpec getCommandSpec() {
		return commandSpec;
	}
	public SCPClient getScpClient() {
		return scpClient;
	}
}
