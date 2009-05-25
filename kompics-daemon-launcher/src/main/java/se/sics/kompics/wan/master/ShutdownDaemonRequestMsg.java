package se.sics.kompics.wan.master;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.wan.daemon.DaemonAddress;

public final class ShutdownDaemonRequestMsg extends Message {

		private static final long serialVersionUID = -1404086123141879148L;
		private final DaemonAddress daemon;

		public ShutdownDaemonRequestMsg(DaemonAddress src, Address dest) {
			super(src.getPeerAddress(), dest);
			this.daemon = src;
		}


		public DaemonAddress getDaemon() {
			return daemon;
		}
	}