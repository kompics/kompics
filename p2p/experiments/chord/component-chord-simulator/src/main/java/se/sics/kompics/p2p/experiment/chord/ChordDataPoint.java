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
package se.sics.kompics.p2p.experiment.chord;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;

/**
 * The <code>ChordDataPoint</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class ChordDataPoint {

	private final int networkSize;
	private final int totalMessageCount;
	private final long totalLookupCount;
	private final long successLookupCount;
	private final long correctLookupCount;
	private final double lookupSuccessRatio;
	private final double lookupCorrectRatio;

	SummaryStatistics failedHopCount = new SummaryStatistics();
	SummaryStatistics failedLatency = new SummaryStatistics();
	SummaryStatistics successHopCount = new SummaryStatistics();
	SummaryStatistics successLatency = new SummaryStatistics();
	SummaryStatistics correctHopCount = new SummaryStatistics();
	SummaryStatistics correctLatency = new SummaryStatistics();
	DescriptiveStatistics lookupLoad = new DescriptiveStatistics();

	public ChordDataPoint(int networkSize, ChordDataSet set) {
		this.networkSize = networkSize;

		int messages = 0;
		for (ReceivedMessage rm : set.messageHistogram.values()) {
			messages += rm.getTotalCount();
		}
		this.totalMessageCount = messages;

		this.totalLookupCount = set.totalLookups;
		this.successLookupCount = set.successLookups;
		this.correctLookupCount = set.correctLookups;
		this.lookupSuccessRatio = ((double) set.successLookups)
				/ set.totalLookups;
		this.lookupCorrectRatio = ((double) set.correctLookups)
				/ set.successLookups;

		for (ChordLookupStat stat : set.lookups) {
			if (stat.isSuccessful()) {
				successHopCount.addValue(stat.getHops());
				successLatency.addValue(stat.getDuration());
				if (stat.isCorrect()) {
					correctHopCount.addValue(stat.getHops());
					correctLatency.addValue(stat.getDuration());
				}
			} else {
				failedHopCount.addValue(stat.getHops());
				failedLatency.addValue(stat.getDuration());
			}
		}

		for (Integer i : set.loadHistogram.values()) {
			lookupLoad.addValue(i);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Network size:      ").append(networkSize);
		sb.append("\nMessages:          ").append(totalMessageCount);
		sb.append("\nLookups completed: ").append(totalLookupCount);
		sb.append("\nLookups success:   ").append(successLookupCount);
		sb.append("\nLookups correct:   ").append(correctLookupCount);
		sb.append("\nSuccess ratio:     ").append(lookupSuccessRatio);
		sb.append("\nCorrect ratio:     ").append(lookupCorrectRatio);
		sb.append("\nSuccess hopcount:  ").append(successHopCount.getMean());
		sb.append("\nCorrect hopcount:  ").append(correctHopCount.getMean());
		sb.append("\nSuccess duration:  ").append(successLatency.getMean());
		sb.append("\nCorrect duration:  ").append(correctLatency.getMean());
		sb.append("\nLoad mean:         ").append(lookupLoad.getMean());
		sb.append("\nLoad stddev:       ").append(
				lookupLoad.getStandardDeviation());
		sb.append("\nLoad 1%:           ").append(lookupLoad.getPercentile(1));
		for (int i = 10; i <= 90; i += 10) {
			sb.append("\nLoad ").append(i).append("%:          ").append(
					lookupLoad.getPercentile(i));
		}
		sb.append("\nLoad 99%:          ").append(lookupLoad.getPercentile(99));
		return sb.toString();
	}
}
