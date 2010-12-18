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
package se.sics.kompics.p2p.fd.ping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.p2p.fdstatus.ProbedPeerData;
import se.sics.kompics.p2p.overlay.OverlayAddress;

/**
 * The <code>PeerResponseTime</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class PeerResponseTime {

	private static Logger log = LoggerFactory.getLogger(PeerResponseTime.class);

	private static int SAMPLES = 100;
	
	private double avgRTT;
	private double varRTT;
	private double stdRTT;
	private double lastRTT;
	private double RTO;
	private double showedRTO;

	private double alpha = 0.125;
	private double beta = 0.25;
	private long K = 4;

	private long minRTO;
	
	private double[] rtt;
	private double[] sqerr;
	private int samples;
	private int lastRttIdx;

	public PeerResponseTime(long rtoMin) {
		avgRTT = 0.0;
		varRTT = 0.0;
		stdRTT = 0.0;
		RTO = -1.0;
		showedRTO = 0.0;
		minRTO = rtoMin;
		rtt = new double[SAMPLES];
		sqerr = new double[SAMPLES];
		lastRttIdx = 0;
		samples = 0;
	}

	public void updateRTO(long RTT) {
		// RTT is in nanoseconds
		double newRtt = ((double) RTT) / 1000000.0;  // in milliseconds
		if (samples == 0) {
			avgRTT = newRtt;
			varRTT = 0.0;
			stdRTT = 0.0;
			RTO = newRtt;
			
			rtt[lastRttIdx] = newRtt;
			sqerr[lastRttIdx] = 0.0;
			samples = 1;
			lastRttIdx = 1;
			
			log.debug("Initial RTO " + RTO);
		} else if (samples == SAMPLES) {
			double oldRtt = rtt[lastRttIdx];
			double oldSqerr = sqerr[lastRttIdx];
			
			avgRTT = ((samples * avgRTT) - oldRtt + newRtt) / samples;
			double newSqerr = (avgRTT - newRtt) * (avgRTT - newRtt);
			
			varRTT = ((samples * varRTT) - oldSqerr + newSqerr) / samples;
			stdRTT = Math.sqrt(varRTT);

			RTO = avgRTT + K * varRTT;
		
			// replace oldRtt with newRtt
			rtt[lastRttIdx] = newRtt;
			sqerr[lastRttIdx] = newSqerr;
			lastRttIdx++;
			if (lastRttIdx == SAMPLES) lastRttIdx = 0;
		} else {  //   0 < samples < SAMPLES
			avgRTT = ((samples * avgRTT) + newRtt) / (samples + 1);
			
			double sqErr = (avgRTT - newRtt) * (avgRTT - newRtt);
			varRTT = ((samples * varRTT) + sqErr) / (samples + 1);
			stdRTT = Math.sqrt(varRTT);

			RTO = avgRTT + K * varRTT;

			// add new RTT and square error to our samples
			rtt[lastRttIdx] = newRtt;
			sqerr[lastRttIdx] = sqErr;
			samples++;
			lastRttIdx++;
			if (lastRttIdx == SAMPLES) lastRttIdx = 0;
		}
		lastRTT = newRtt;
		showedRTO = (RTO < minRTO ? minRTO : RTO);
	}
	
	/**
	 * Updates the average RTO, we use a TCP-style calculation of the RTO
	 * 
	 * @param rtt
	 *            The RTT of the packet
	 */
	public void updateRTOAllTime(long rtt) {

		if (RTO == -1) {
			// Set RTO to RTT if it's the first time it's updated
			// this.count = 1;

			/*
			 * SRTT <- R, RTTVAR <- R/2, RTO <- SRTT + max (G, KRTTVAR)
			 */
			this.avgRTT = rtt;
			this.varRTT = 0;

			this.RTO = avgRTT + K * varRTT;

			log.debug("Initial RTO " + RTO);
		} else {

			// log.debug("Changing RTO " + RTO);
			// log.debug("VAR " + varRTT);
			// log.debug("AVG " + avgRTT);
			// log.debug("Beta "+beta);
			// log.debug("Alpha "+alpha);

			// RTTVAR <- (1 - beta) * RTTVAR + beta * |SRTT - R'|
			this.varRTT = (1 - beta) * varRTT + beta * Math.abs((avgRTT - rtt));

			// log.debug("Variance " + varRTT);
			// SRTT <- (1 - alpha) * SRTT + alpha * R'
			this.avgRTT = (1 - alpha) * avgRTT + alpha * rtt;

			// log.debug("Average " + avgRTT);

			// RTO = AVG + K x VAR;
			this.RTO = avgRTT + K * varRTT;

			// log.debug("Result RTO " + RTO);

			// // AVG = (((AVG * CNT) + RTT) / (CNT + 1));
			// this.avgRTT = (((avgRTT * count) + RTT) / (count + 1));

			// log.debug("Average RTT " + avgRTT);
			//
			// // DIFF = (AVG - RTT)^2;
			// this.diff = pow((avgRTT - RTT), 2);
			//
			// log.debug(" DIFF " + diff);
			//			
			// log.debug("Var RTT before "+ varRTT);
			//
			// // VAR = (((VAR * CNT) + DIFF) / (CNT + 1)); // variance of RTT
			// this.varRTT = (((varRTT * count) + diff) / (count + 1));
			//
			// log.debug("Variance " + varRTT);
			// // CNT++;
			// this.count++;
			//
			// // RTO = AVG + 4 x VAR;
			// this.RTO = avgRTT + K * varRTT;

		}
		// log.debug("RTO before check if between max and min value " + RTO);
		if (this.RTO < minRTO) {
			this.showedRTO = minRTO;

			// maximum does not make sense
			// } else if (this.RTO > FD_MAXIMUM_RTO) {
			// this.showedRTO = FD_MAXIMUM_RTO;
		} else {
			this.showedRTO = RTO;
		}
	}

	public void timedOut() {
		
	}
	
	public long getRTO() {  // in milliseconds
		long r = (showedRTO == 0 ? (long) minRTO : (long) showedRTO);

		if (r < minRTO)
			System.err.println("r=" + r + " min=" + minRTO);

		return r;
	}

	ProbedPeerData getProbedPeerData(OverlayAddress overlayAddress) {
		return new ProbedPeerData(avgRTT, varRTT, stdRTT, lastRTT, RTO,
				showedRTO, minRTO, overlayAddress);
	}
}
