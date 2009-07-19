package se.sics.kompics.wan.ssh.scp.events;

import java.util.List;

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
public class UploadMD5Request extends Request {

	private final SCPClient scpClient;
	private final List<FileInfo> fileMD5Hashes;
	private final CommandSpec commandSpec;
	private final int sessionId;
	
	
	public UploadMD5Request(int sessionId, SCPClient scpClient,
			List<FileInfo> fileMD5Hashes, CommandSpec commandSpec) {
		super();
		this.sessionId = sessionId;
		this.scpClient = scpClient;
		this.fileMD5Hashes = fileMD5Hashes;
		this.commandSpec = commandSpec;
	}

	public CommandSpec getCommandSpec() {
		return commandSpec;
	}

	public List<FileInfo> getFileMD5Hashes() {
		return fileMD5Hashes;
	}

	public SCPClient getScpClient() {
		return scpClient;
	}

	/**
	 * @return the sessionId
	 */
	public int getSessionId() {
		return sessionId;
	}
}
