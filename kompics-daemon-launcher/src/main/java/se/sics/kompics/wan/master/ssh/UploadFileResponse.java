package se.sics.kompics.wan.master.ssh;

import java.io.File;

import se.sics.kompics.Response;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class UploadFileResponse extends Response {

	private final File file;

	public UploadFileResponse(UploadFileRequest request, File file) {
		super(request);
		this.file = file;
	}

	public File getFile() {
		return file;
	}

}
