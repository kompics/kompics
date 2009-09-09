/**
 * This file is part of the Kompics P2P Framework.
 * 
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.kompics.example.p2p.experiments.cyclon;

import java.math.BigInteger;

import se.sics.kompics.p2p.experiment.cyclon.CollectCyclonData;
import se.sics.kompics.p2p.experiment.cyclon.CyclonPeerFail;
import se.sics.kompics.p2p.experiment.cyclon.CyclonPeerGetPeer;
import se.sics.kompics.p2p.experiment.cyclon.CyclonPeerJoin;
import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation1;
import se.sics.kompics.p2p.experiments.cyclon.CyclonSimulationMain;

/**
 * The <code>CyclonExperiments</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
@SuppressWarnings("serial")
public class CyclonSimulationExperiments {
	public static void main(String[] args) throws Throwable {
		SimulationScenario cyclonScenario1 = new SimulationScenario() {
			{
				StochasticProcess data = new StochasticProcess() {
					{
						eventInterArrivalTime(constant(1000));
						raise(10, cyclonData);
					}
				};
				StochasticProcess process1 = new StochasticProcess() {
					{
						eventInterArrivalTime(constant(1000));
						raise(2, cyclonJoin, uniform(13));
					}
				};
				StochasticProcess process2 = new StochasticProcess() {
					{
						eventInterArrivalTime(constant(500));
						raise(10, cyclonJoin, uniform(13));
					}
				};
				StochasticProcess process3 = new StochasticProcess() {
					{
						eventInterArrivalTime(exponential(10));
						raise(2500, cyclonJoin, uniform(13));
					}
				};
				// StochasticProcess churn = new StochasticProcess() {
				// {
				// eventInterArrivalTime(exponential(50));
				// raise(500, cyclonJoin, uniform(13));
				// raise(500, cyclonFail, uniform(13));
				// }
				// };

				process1.start();
				process2.startAfterTerminationOf(2000, process1);
				process3.startAfterTerminationOf(3000, process2);
				// churn.startAfterTerminationOf(15000, process3);
				// data.startAfterTerminationOf(10000, churn);
				data.startAfterTerminationOf(15000, process3);

				terminateAfterTerminationOf(1000, data);
			}
		};

		long seed = Long.parseLong(args[0]);

		Configuration configuration = new Configuration();
		configuration.set();

		cyclonScenario1.setSeed(seed);
		cyclonScenario1.simulate(CyclonSimulationMain.class);
		// cyclonScenario1.execute(CyclonExecutionMain.class);
	}

	// operations

	static Operation<CollectCyclonData> cyclonData = new Operation<CollectCyclonData>() {
		public CollectCyclonData generate() {
			return new CollectCyclonData();
		}
	};

	static Operation1<CyclonPeerJoin, BigInteger> cyclonJoin = new Operation1<CyclonPeerJoin, BigInteger>() {
		public CyclonPeerJoin generate(BigInteger id) {
			return new CyclonPeerJoin(id);
		}
	};

	static Operation1<CyclonPeerFail, BigInteger> cyclonFail = new Operation1<CyclonPeerFail, BigInteger>() {
		public CyclonPeerFail generate(BigInteger id) {
			return new CyclonPeerFail(id);
		}
	};

	static Operation1<CyclonPeerGetPeer, BigInteger> cyclonGetPeer = new Operation1<CyclonPeerGetPeer, BigInteger>() {
		public CyclonPeerGetPeer generate(BigInteger id) {
			return new CyclonPeerGetPeer(id);
		}
	};
}
