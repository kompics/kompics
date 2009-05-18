package se.sics.kompics.kdld.master;

import se.sics.kompics.Init;
import se.sics.kompics.kdld.daemon.DaemonAddress;
import se.sics.kompics.p2p.bootstrap.BootstrapConfiguration;

/**
 * The <code>MasterClientInit</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author jdowling
 */
	public final class MasterClientInit extends Init {

		private final DaemonAddress self;

		private final MasterConfiguration masterConfiguration;

		public MasterClientInit(DaemonAddress self,
				MasterConfiguration masterConfiguration) {
			super();
			this.self = self;
			this.masterConfiguration = masterConfiguration;
		}

		public DaemonAddress getSelf() {
			return self;
		}

		public MasterConfiguration getMasterConfiguration() {
			return masterConfiguration;
		}
	}
