/*
 * This file is part of the Kompics component model runtime.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) 
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * This program is free software; you can redistribute it and/or
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
package se.sics.kompics.network.data;

import com.larskroll.common.statistics.ExponentialMovingAverage;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import se.sics.kompics.network.Transport;

/**
 *
 * @author lkroll
 */
public class Statistics {

    public static final double NANOSEC = 1e9;
    public static final double KILOBYTE = 1024.0;
    public static final int WINDOW_SIZE = 1;
    public static final double ALPHA = 0.1;
    
    private ExponentialMovingAverage delTime = new ExponentialMovingAverage(ALPHA);
    //private DescriptiveStatistics size = new DescriptiveStatistics(WINDOW_SIZE);
    private boolean updated = false;
    private long lastPrintTS = -1;
    private long sizeAccum = 0;
    private final DescriptiveStatistics approxThroughput = new DescriptiveStatistics(WINDOW_SIZE);
    private SummaryStatistics selectionRatio = new SummaryStatistics();
    private SummaryStatistics lastWindowsRatio = new SummaryStatistics();

    void reset() {
        delTime = new ExponentialMovingAverage(ALPHA);
        //size = new DescriptiveStatistics(WINDOW_SIZE);
        lastPrintTS = -1;
        sizeAccum = 0;
        approxThroughput.clear();
        selectionRatio.clear();
        lastWindowsRatio = selectionRatio;
        updated = false;
    }

    void endWindow() {
        updated = false;

        if (lastPrintTS > 0) {
            long diffL = System.nanoTime() - lastPrintTS;
            double diffD = ((double) diffL) / NANOSEC;
            double accumD = ((double) sizeAccum) / KILOBYTE;
            double tp = accumD / diffD;
            approxThroughput.addValue(tp);
        }
        sizeAccum = 0;
        lastWindowsRatio = selectionRatio;
        selectionRatio = new SummaryStatistics();
        lastPrintTS = System.nanoTime();
    }
    
    public double avgDeliveryTime() {
        return delTime.getMean();
    }
    
    public double avgThroughputApproximation() {
        return approxThroughput.getMean();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Delivery Time: ");
        sb.append(delTime);
        sb.append('\n');
        //sb.append("Message Size: ");
        //sb.append(size);
        sb.append('\n');
        sb.append("Approximate Throughput: ");
        sb.append(approxThroughput);
        sb.append('\n');
        sb.append("Recent Selection Ratio: ");
        sb.append(lastWindowsRatio);
        sb.append('\n');
        return sb.toString();
    }

    void update(double delTD, int msgSize) {
        double sizeD = msgSize / KILOBYTE; // kb
        //size.addValue(sizeD);
        delTime.addValue(delTD);
        sizeAccum += msgSize;

        updated = true;
    }
    
    void updateSelection( Transport selection) {
        switch (selection) {
            case TCP: selectionRatio.addValue(-1.0); break;
            case UDT: selectionRatio.addValue(1.0); break;
            default: throw new RuntimeException("How the fuck?");
        }
    }
    
    public boolean isUpdated() {
        return this.updated;
    }
}
