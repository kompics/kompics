package se.sics.kompics.wan.master.ssh;

import java.io.File;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class UploadFileRequest extends SshCommandRequest {

	private final File file;
	
	public UploadFileRequest(int sessionId, File file, 
			double timeout, boolean stopOnError) {
		super(sessionId, "#upload " + file.getPath(), timeout, stopOnError);
		this.file = file;
	}

	public File getFile() {
		return file;
	}
}
