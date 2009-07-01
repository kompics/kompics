package se.sics.kompics.wan.master.scp.upload;

import se.sics.kompics.Response;
import se.sics.kompics.wan.master.scp.FileInfo;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class UploadMD5Response extends Response {

	
	private final boolean status;
	private final int commandId;
	private final FileInfo file;
	
	public UploadMD5Response(UploadMD5Request request, int commandId, 
			FileInfo file, boolean status) {
		super(request);
		this.commandId = commandId;
		this.status = status;
		this.file = file;
	}

	public int getCommandId() {
		return commandId;
	}
	
	public FileInfo getFile() {
		return file;
	}
	
	/**
	 * @return the stopOnError
	 */
	public boolean isStatus() {
		return status;
	}
}
