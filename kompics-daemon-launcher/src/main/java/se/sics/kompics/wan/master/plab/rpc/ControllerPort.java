package se.sics.kompics.wan.master.plab.rpc;

import se.sics.kompics.PortType;
import se.sics.kompics.wan.master.plab.plc.events.GetAllHostsRequest;
import se.sics.kompics.wan.master.plab.plc.events.GetAllHostsResponse;
import se.sics.kompics.wan.master.plab.plc.events.InstallDaemonOnHostsRequest;
import se.sics.kompics.wan.master.plab.plc.events.InstallDaemonOnHostsResponse;
import se.sics.kompics.wan.master.plab.plc.events.QueryPLabSitesRequest;
import se.sics.kompics.wan.master.plab.plc.events.QueryPLabSitesResponse;
import se.sics.kompics.wan.master.ssh.events.CommandRequest;
import se.sics.kompics.wan.master.ssh.events.CommandResponse;
import se.sics.kompics.wan.master.ssh.events.DownloadFileRequest;
import se.sics.kompics.wan.master.ssh.events.DownloadFileResponse;
import se.sics.kompics.wan.master.ssh.events.HaltRequest;
import se.sics.kompics.wan.master.ssh.events.HaltResponse;
import se.sics.kompics.wan.master.ssh.events.SshConnectRequest;
import se.sics.kompics.wan.master.ssh.events.SshConnectResponse;
import se.sics.kompics.wan.master.ssh.events.UploadFileRequest;
import se.sics.kompics.wan.master.ssh.events.UploadFileResponse;



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
