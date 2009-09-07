package se.sics.kompics.wan.masterdaemon.events;

import se.sics.kompics.address.Address;
import se.sics.kompics.wan.masterdaemon.events.DaemonAddress;
import se.sics.kompics.wan.masterdaemon.events.DaemonRequestMsg;

public final class ShutdownDaemonRequestMsg extends DaemonRequestMsg
{

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