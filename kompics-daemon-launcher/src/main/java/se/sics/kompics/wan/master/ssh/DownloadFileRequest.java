package se.sics.kompics.wan.master.ssh;


/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class DownloadFileRequest extends SshCommandRequest {

	public DownloadFileRequest(int sessionId, String file, double timeout, boolean stopOnError) {
		super(sessionId, "#download " + file, timeout, stopOnError);
	}

}
