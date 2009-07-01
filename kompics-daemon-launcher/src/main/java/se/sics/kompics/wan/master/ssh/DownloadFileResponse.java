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

	public DownloadFileResponse(DownloadFileRequest request, File file) {
		super(request);
		this.file = file;
	}

	public File getFile() {
		return file;
	}

}
