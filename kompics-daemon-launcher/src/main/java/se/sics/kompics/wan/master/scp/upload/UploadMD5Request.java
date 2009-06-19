package se.sics.kompics.wan.master.scp.upload;

import java.util.List;

import se.sics.kompics.Request;
import se.sics.kompics.wan.master.scp.FileInfo;
import se.sics.kompics.wan.master.ssh.CommandSpec;
import se.sics.kompics.wan.master.ssh.SshComponent;
import se.sics.kompics.wan.master.ssh.SshComponent.SshConn;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class UploadMD5Request extends Request {

	private final SshConn sshConn;
	private final List<FileInfo> fileMD5Hashes;
	private final CommandSpec commandSpec;

	public UploadMD5Request(SshConn sshConn, List<FileInfo> fileMD5Hashes,
			CommandSpec commandSpec) {
		super();
		this.sshConn = sshConn;
		this.fileMD5Hashes = fileMD5Hashes;
		this.commandSpec = commandSpec;
	}

	public CommandSpec getCommandSpec() {
		return commandSpec;
	}

	public List<FileInfo> getFileMD5Hashes() {
		return fileMD5Hashes;
	}

	public SshConn getSshConn() {
		return sshConn;
	}

}
