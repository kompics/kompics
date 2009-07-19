package se.sics.kompics.wan.ssh.scp.events;

import se.sics.kompics.Request;
import se.sics.kompics.wan.ssh.CommandSpec;
import se.sics.kompics.wan.ssh.scp.FileInfo;
import ch.ethz.ssh2.SCPClient;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class ScpGetFileRequest extends Request {

	private final FileInfo fileInfo;
	private final SCPClient scpClient;
	private final CommandSpec commandSpec;

	public ScpGetFileRequest(FileInfo fileInfo, 
			CommandSpec commandSpec, SCPClient scpClient) {
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
