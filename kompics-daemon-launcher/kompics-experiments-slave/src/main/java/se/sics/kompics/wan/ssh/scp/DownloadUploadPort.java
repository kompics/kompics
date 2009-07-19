package se.sics.kompics.wan.ssh.scp;

import se.sics.kompics.PortType;
import se.sics.kompics.wan.ssh.scp.events.DownloadMD5Request;
import se.sics.kompics.wan.ssh.scp.events.DownloadMD5Response;
import se.sics.kompics.wan.ssh.scp.events.UploadMD5Request;
import se.sics.kompics.wan.ssh.scp.events.UploadMD5Response;



/**
 * The <code>SshPort</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class DownloadUploadPort extends PortType {

	{
		negative(DownloadMD5Request.class);
		negative(UploadMD5Request.class);
		
		positive(DownloadMD5Response.class);
		positive(UploadMD5Response.class);
	}
}
