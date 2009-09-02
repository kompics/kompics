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
package se.sics.kompics.example.p2p.experiments.chord;

import java.math.BigInteger;

import se.sics.kompics.p2p.experiment.chord.ChordLookup;
import se.sics.kompics.p2p.experiment.chord.ChordPeerFail;
import se.sics.kompics.p2p.experiment.chord.ChordPeerJoin;
import se.sics.kompics.p2p.experiment.chord.CollectData;
import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation1;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation2;
import se.sics.kompics.p2p.experiments.chord.ChordSimulationMain;
import se.sics.kompics.p2p.overlay.key.NumericRingKey;

/**
 * The <code>ChordExperiments</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
@SuppressWarnings("serial")
public class ChordExperiments {
	public static void main(String[] args) throws Throwable {
		SimulationScenario chordScenario1 = new SimulationScenario() {
			{
				StochasticProcess dataBegin = new StochasticProcess() {
					{
						eventInterArrivalTime(constant(500));
						raise(1, chordData);
					}
				};
				StochasticProcess dataEnd = new StochasticProcess() {
					{
						eventInterArrivalTime(constant(500));
						raise(1, chordData);
					}
				};
				StochasticProcess process1 = new StochasticProcess() {
					{
						eventInterArrivalTime(constant(1000));
						raise(4, chordJoin, uniform(13));
					}
				};
				StochasticProcess process2 = new StochasticProcess() {
					{
						eventInterArrivalTime(constant(500));
						raise(60, chordJoin, uniform(13));
					}
				};
				StochasticProcess process3 = new StochasticProcess() {
					{
						eventInterArrivalTime(exponential(500));
						raise(500, chordJoin, uniform(13));
						// raise(1, chordLookup, uniform(13), uniform(13));
						// raise(20, chordFail, uniform(13));
					}
				};
				StochasticProcess process4 = new StochasticProcess() {
					{
						eventInterArrivalTime(exponential(5));
						raise(5000, chordLookup, uniform(13), uniform(13));
					}
				};
				// StochasticProcess lowChurn = new StochasticProcess() {
				// {
				// eventInterArrivalTime(exponential(500));
				// raise(500, chordJoin, uniform(13));
				// raise(500, chordFail, uniform(13));
				// }
				// };
				// StochasticProcess highChurn = new StochasticProcess() {
				// {
				// eventInterArrivalTime(exponential(50));
				// raise(500, chordJoin, uniform(13));
				// raise(500, chordFail, uniform(13));
				// }
				// };

				process1.start();
				process2.startAfterTerminationOf(2000, process1);
				process3.startAfterTerminationOf(3000, process2);

				dataBegin.startAfterTerminationOf(1000, process3);
				// highChurn.startAfterTerminationOf(0, dataBegin);
				process4.startAfterTerminationOf(0, dataBegin);
				dataEnd.startAfterTerminationOf(5000, process4);

				terminateAfterTerminationOf(1000, dataEnd);
			}
		};

		long seed = Long.parseLong(args[0]);

		Configuration configuration = new Configuration();
		configuration.set();

		chordScenario1.setSeed(seed + 2);
		chordScenario1.simulate(ChordSimulationMain.class);
		// chordScenario1.execute(ChordExecutionMain.class);
	}

	// operations

	static Operation<CollectData> chordData = new Operation<CollectData>() {
		public CollectData generate() {
			return new CollectData();
		}
	};

	static Operation1<ChordPeerJoin, BigInteger> chordJoin = new Operation1<ChordPeerJoin, BigInteger>() {
		public ChordPeerJoin generate(BigInteger id) {
			return new ChordPeerJoin(new NumericRingKey(id));
		}
	};

	static Operation1<ChordPeerFail, BigInteger> chordFail = new Operation1<ChordPeerFail, BigInteger>() {
		public ChordPeerFail generate(BigInteger id) {
			return new ChordPeerFail(new NumericRingKey(id));
		}
	};

	static Operation2<ChordLookup, BigInteger, BigInteger> chordLookup = new Operation2<ChordLookup, BigInteger, BigInteger>() {
		public ChordLookup generate(BigInteger node, BigInteger key) {
			return new ChordLookup(new NumericRingKey(node),
					new NumericRingKey(key));
		}
	};
}
