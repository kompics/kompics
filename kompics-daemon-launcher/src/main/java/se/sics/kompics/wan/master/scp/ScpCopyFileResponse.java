package se.sics.kompics.wan.master.scp;

import se.sics.kompics.Response;
import se.sics.kompics.wan.master.ssh.CommandSpec;
import ch.ethz.ssh2.SCPClient;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class ScpCopyFileResponse extends Response {

	private final FileInfo fileInfo;
	private final SCPClient scpClient;
	private final CommandSpec commandSpec;
	
	public ScpCopyFileResponse(ScpCopyFileRequest request, FileInfo fileInfo, 
			CommandSpec commandSpec, SCPClient scpClient) {
		super(request);
		this.fileInfo = fileInfo;
		this.commandSpec = commandSpec;
		this.scpClient = scpClient;
	}

	public FileInfo getFileInfo() {
		return fileInfo;
	}
	
	/**
	 * @return the commandSpec
	 */
	public CommandSpec getCommandSpec() {
		return commandSpec;
	}

	public SCPClient getScpClient() {
		return scpClient;
	}
}
