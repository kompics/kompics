package se.sics.kompics.wan.daemon.masterclient;

import se.sics.kompics.PortType;
import se.sics.kompics.wan.daemonmaster.ConnectMasterRequest;
import se.sics.kompics.wan.daemonmaster.ConnectMasterResponse;
import se.sics.kompics.wan.daemonmaster.DisconnectMasterRequest;
import se.sics.kompics.wan.daemonmaster.ShutdownDaemonRequest;

public class MasterClientPort extends PortType {
		{
			positive(ConnectMasterResponse.class);
			positive(ShutdownDaemonRequest.class);
			negative(ConnectMasterRequest.class);
			negative(DisconnectMasterRequest.class);
		}
}
