package se.sics.kompics.wan.master.scp.upload;

import se.sics.kompics.Response;
import se.sics.kompics.wan.master.scp.download.DownloadMD5Request;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class UploadMD5Response extends Response {

	
	private final boolean status;
	
	public UploadMD5Response(DownloadMD5Request request, boolean status) {
		super(request);
		this.status = status;
	}
	
	/**
	 * @return the stopOnError
	 */
	public boolean isStatus() {
		return status;
	}
}
