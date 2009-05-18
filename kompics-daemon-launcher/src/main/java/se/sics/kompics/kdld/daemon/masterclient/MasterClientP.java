package se.sics.kompics.kdld.daemon.masterclient;

import se.sics.kompics.PortType;
import se.sics.kompics.kdld.master.ConnectMasterRequest;
import se.sics.kompics.kdld.master.ConnectMasterResponse;
import se.sics.kompics.kdld.master.DisconnectMasterRequest;
import se.sics.kompics.kdld.master.ShutdownDaemonRequest;

public class MasterClientP extends PortType {
		{
			positive(ConnectMasterResponse.class);
			positive(ShutdownDaemonRequest.class);
			negative(ConnectMasterRequest.class);
			negative(DisconnectMasterRequest.class);
		}
}
