package se.sics.kompics.wan.daemon.masterclient;

import se.sics.kompics.PortType;
import se.sics.kompics.wan.master.ConnectMasterRequest;
import se.sics.kompics.wan.master.ConnectMasterResponse;
import se.sics.kompics.wan.master.DisconnectMasterRequest;
import se.sics.kompics.wan.master.ShutdownDaemonRequest;

public class MasterClientPort extends PortType {
		{
			positive(ConnectMasterResponse.class);
			positive(ShutdownDaemonRequest.class);
			negative(ConnectMasterRequest.class);
			negative(DisconnectMasterRequest.class);
		}
}
