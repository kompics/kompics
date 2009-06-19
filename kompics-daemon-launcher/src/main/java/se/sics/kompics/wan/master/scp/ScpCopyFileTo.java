package se.sics.kompics.wan.master.scp;

import se.sics.kompics.Event;
import ch.ethz.ssh2.SCPClient;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class ScpCopyFileTo extends Event {

	private final String hostname;
	private final FileInfo fileInfo;
	private final SCPClient scpClient;

	public ScpCopyFileTo(String hostname, FileInfo fileInfo, SCPClient scpClient) {
		this.hostname = hostname;
		this.fileInfo = fileInfo;
		this.scpClient = scpClient;
	}

	public String getHostname() {
		return hostname;
	}

	public FileInfo getFileInfo() {
		return fileInfo;
	}

	public SCPClient getScpClient() {
		return scpClient;
	}
}
