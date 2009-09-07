package se.sics.kompics.wan.daemon.masterclient;

import se.sics.kompics.PortType;
import se.sics.kompics.wan.daemonmaster.events.ConnectMasterRequest;
import se.sics.kompics.wan.daemonmaster.events.ConnectMasterResponse;
import se.sics.kompics.wan.daemonmaster.events.DisconnectMasterRequest;
import se.sics.kompics.wan.daemonmaster.events.ShutdownDaemonRequest;

public class MasterClientPort extends PortType {
    {
        positive(ConnectMasterResponse.class);
        positive(ShutdownDaemonRequest.class);
        negative(ConnectMasterRequest.class);
        negative(DisconnectMasterRequest.class);
    }
}
