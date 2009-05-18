package se.sics.kompics.kdld.master;

import se.sics.kompics.PortType;

public class MasterClientP extends PortType {
		{
			positive(ConnectMasterResponse.class);
			negative(ConnectMasterRequest.class);
			negative(DisconnectMasterRequest.class);
		}
}
