package se.sics.kompics.wan.master.plab.rpc;

import se.sics.kompics.PortType;
import se.sics.kompics.wan.plab.events.GetAllHostsRequest;
import se.sics.kompics.wan.plab.events.GetAllHostsResponse;
import se.sics.kompics.wan.plab.events.InstallDaemonOnHostsRequest;
import se.sics.kompics.wan.plab.events.InstallDaemonOnHostsResponse;
import se.sics.kompics.wan.plab.events.QueryPLabSitesRequest;
import se.sics.kompics.wan.plab.events.QueryPLabSitesResponse;
import se.sics.kompics.wan.ssh.events.CommandRequest;
import se.sics.kompics.wan.ssh.events.CommandResponse;
import se.sics.kompics.wan.ssh.events.DownloadFileRequest;
import se.sics.kompics.wan.ssh.events.DownloadFileResponse;
import se.sics.kompics.wan.ssh.events.HaltRequest;
import se.sics.kompics.wan.ssh.events.HaltResponse;
import se.sics.kompics.wan.ssh.events.SshConnectRequest;
import se.sics.kompics.wan.ssh.events.SshConnectResponse;
import se.sics.kompics.wan.ssh.events.UploadFileRequest;
import se.sics.kompics.wan.ssh.events.UploadFileResponse;



/**
 * The <code>SshPort</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class ControllerPort extends PortType {

	{
		
		negative(GetAllHostsRequest.class);
		negative(QueryPLabSitesRequest.class);
		negative(InstallDaemonOnHostsRequest.class);
		
		positive(GetAllHostsResponse.class);
		positive(QueryPLabSitesResponse.class);
		positive(InstallDaemonOnHostsResponse.class);
		
		negative(SshConnectRequest.class);
		negative(CommandRequest.class);
		negative(HaltRequest.class);		
		negative(DownloadFileRequest.class);
		negative(UploadFileRequest.class);
		
		positive(SshConnectResponse.class);
		positive(CommandResponse.class);
		positive(HaltResponse.class);
		positive(DownloadFileResponse.class);
		positive(UploadFileResponse.class);

	}
}
