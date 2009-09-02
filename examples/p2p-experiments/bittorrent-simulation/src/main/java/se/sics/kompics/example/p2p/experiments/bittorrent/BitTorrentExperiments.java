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
package se.sics.kompics.example.p2p.experiments.bittorrent;

import java.math.BigInteger;
import java.util.Random;

import se.sics.kompics.p2p.cdn.bittorrent.client.Bitfield;
import se.sics.kompics.p2p.experiment.bittorrent.BitTorrentPeerJoin;
import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation4;
import se.sics.kompics.p2p.experiments.bittorrent.BitTorrentSimulationMain;

/**
 * The <code>BitTorrentExperiments</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
@SuppressWarnings("serial")
public class BitTorrentExperiments {
	static long seed;
	static Configuration conf = new Configuration();
	static long KBps = 1024;

	public static void main(String[] args) throws Throwable {
		seed = Long.parseLong(args[0]);
		final Configuration c = new Configuration();

		SimulationScenario bitTorrentScenario1 = new SimulationScenario() {
			{
				StochasticProcess joinSeeds = new StochasticProcess() {
					{
						eventInterArrivalTime(constant(1000));
						raise(c.initialSeeds, bitTorrentJoin, uniform(13),
								constant(256 * KBps) /* download */,
								constant(256 * KBps) /* upload */, constant(1.0));
					}
				};
				StochasticProcess joinLeechers = new StochasticProcess() {
					{
						eventInterArrivalTime(constant(500));
						raise(c.leechers, bitTorrentJoin, uniform(13),
								constant(256 * KBps) /* download */,
								constant(256 * KBps) /* upload */, constant(0.0));
					}
				};

				joinSeeds.start();
				joinLeechers.startAfterTerminationOf(1000, joinSeeds);

				// terminateAt(3600 * 1000);
			}
		};
		
		c.set();
		
		bitTorrentScenario1.setSeed(seed);
		bitTorrentScenario1.simulate(BitTorrentSimulationMain.class);
	}

	static Operation4<BitTorrentPeerJoin, BigInteger, Long, Long, Double> bitTorrentJoin = new Operation4<BitTorrentPeerJoin, BigInteger, Long, Long, Double>() {
		Random r = new Random(seed);

		public BitTorrentPeerJoin generate(BigInteger id, Long downloadBw,
				Long uploadBw, Double downloaded) {
			return new BitTorrentPeerJoin(id, downloadBw, uploadBw,
					new Bitfield(conf.torrent.getPieceCount(), r, downloaded));
		}
	};
}
