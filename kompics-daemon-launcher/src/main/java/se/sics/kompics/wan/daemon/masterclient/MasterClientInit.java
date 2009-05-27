package se.sics.kompics.wan.daemon.masterclient;

import se.sics.kompics.Init;
import se.sics.kompics.wan.daemon.DaemonAddress;
import se.sics.kompics.wan.master.MasterClientConfig;

/**
 * The <code>MasterClientInit</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author jdowling
 */
	public final class MasterClientInit extends Init {

		private final DaemonAddress self;

		private final MasterClientConfig masterConfiguration;

		public MasterClientInit(DaemonAddress self,
				MasterClientConfig masterConfiguration) {
			super();
			this.self = self;
			this.masterConfiguration = masterConfiguration;
		}

		public DaemonAddress getSelf() {
			return self;
		}

		public MasterClientConfig getMasterConfiguration() {
			return masterConfiguration;
		}
	}
