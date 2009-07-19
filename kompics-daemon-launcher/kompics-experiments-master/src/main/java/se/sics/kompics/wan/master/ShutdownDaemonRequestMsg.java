package se.sics.kompics.wan.master;

import se.sics.kompics.address.Address;
import se.sics.kompics.wan.masterdaemon.DaemonAddress;
import se.sics.kompics.wan.masterdaemon.DaemonRequestMessage;

public final class ShutdownDaemonRequestMsg extends DaemonRequestMessage {

		private static final long serialVersionUID = -1404086123141879148L;

		private final int timeout;
		
		public ShutdownDaemonRequestMsg(int timeout, Address src, DaemonAddress dest) {
			super(src, dest);
			this.timeout = timeout;
		}

		public ShutdownDaemonRequestMsg(Address src, DaemonAddress dest) {
			this(0, src, dest);
		}
		
		public int getTimeout() {
			return timeout;
		}

	}