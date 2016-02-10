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

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

/**
 *
 * @author lkroll
 */
public class Statistics {

    public static final double NANOSEC = 1e9;
    public static final double KILOBYTE = 1024.0;
    
    private SummaryStatistics delTime = new SummaryStatistics();
    private SummaryStatistics size = new SummaryStatistics();
    private boolean updated = false;
    private long lastPrintTS = -1;
    private long sizeAccum = 0;
    private SummaryStatistics approxThroughput = new SummaryStatistics();

    void reset() {
        delTime = new SummaryStatistics();
        size = new SummaryStatistics();
        lastPrintTS = -1;
        sizeAccum = 0;
        approxThroughput = new SummaryStatistics();
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
        sb.append("Message Size: ");
        sb.append(size);
        sb.append('\n');
        sb.append("Approximate Throughput: ");
        sb.append(approxThroughput);
        sb.append('\n');
        return sb.toString();
    }

    void update(double delTD, int msgSize) {
        double sizeD = msgSize / KILOBYTE; // kb
        size.addValue(sizeD);
        delTime.addValue(delTD);
        sizeAccum += msgSize;

        updated = true;
    }
    
    public boolean isUpdated() {
        return this.updated;
    }
}
