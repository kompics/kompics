package se.sics.kompics.wan.master.scp.download;

import se.sics.kompics.Response;
import se.sics.kompics.wan.master.scp.FileInfo;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class DownloadMD5Response extends Response {

	
	private final boolean status;
	private final FileInfo fileInfo;
	
	public DownloadMD5Response(DownloadMD5Request request, FileInfo file, boolean status) {
		super(request);
		this.status = status;
		this.fileInfo = file;
	}
	
	/**
	 * @return the stopOnError
	 */
	public boolean isStatus() {
		return status;
	}
	
	public FileInfo getFileInfo() {
		return fileInfo;
	}
}
