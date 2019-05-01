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
package se.sics.kompics.network.data.policies;

import org.jscience.mathematics.number.Rational;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import se.sics.kompics.network.Transport;

/**
 *
 * @author lkroll
 */
@RunWith(JUnit4.class)
public class PatternTest {

    private static final int N = 1000000;

    @Test
    public void constantTest() {
        AlternatingSelection as = new AlternatingSelection();
        as.updateRatio(Rational.ONE);
        long start, end;
        start = System.nanoTime();
        for (int i = 0; i < N; i++) {
            assertEquals(Transport.UDT, as.select(null));
        }
        end = System.nanoTime();
        long time = (end - start) / N;
        System.out.println("UDT Constant Selection took ~" + time + "ns");
        as.updateRatio(Rational.ONE.opposite());
        start = System.nanoTime();
        for (int i = 0; i < N; i++) {
            assertEquals(Transport.TCP, as.select(null));
        }
        end = System.nanoTime();
        time = (end - start) / N;
        System.out.println("TCP Constant Selection took ~" + time + "ns");
    }

    @Test
    public void pTest() {
        // even
        runPatternWith(Rational.valueOf(0, 1), "QP"); // will be 1/1
        // with rest
        runPatternWith(Rational.valueOf(1, 5), "QPQPQ"); // will be 2/3
    }

    @Test
    public void p1Test() {
        // even
        runPatternWith(Rational.valueOf(3, 5), "QQPQQ"); // will be 1/4
        // with rest
        runPatternWith(Rational.valueOf(1, 2), "QPQQ"); // will be 1/3
    }

    @Test
    public void otherTest() {
        runPatternWith(Rational.valueOf(1, 3), "QPQ"); // will be 1/2
        runPatternWith(Rational.valueOf(1, 4), "QPQPQPQQ"); // will be 3/5
    }

    public void runPatternWith(Rational ratio, String expectedPattern) {
        AlternatingSelection as = new AlternatingSelection();
        as.updateRatio(ratio);
        Rational posRatio = ratio.abs(); // fold into [0, 1]
        long n = posRatio.getDividend().longValue();
        long m = posRatio.getDivisor().longValue();
        Rational r = Rational.valueOf(m - n, m + n);
        long pq = r.getDivisor().longValue() + r.getDividend().longValue();
        // performance test
        long start, end;
        start = System.nanoTime();
        for (int i = 0; i < N; i++) {
            as.select(null);
        }
        end = System.nanoTime();
        long time = (end - start) / N;
        System.out.println("Selection with " + r + " took ~" + time + "ns");
        // correctness test
        as = new AlternatingSelection(); // reset
        as.updateRatio(ratio);
        long np = 0;
        long nq = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < N; i++) {
            switch (as.select(null)) {
            case TCP:
                np++;
                sb.append("P");
                break;
            case UDT:
                nq++;
                sb.append("Q");
                break;
            }

            if ((np + nq) % pq == 0) {
                // System.out.println("Checking string: " + sb.toString());
                assertEquals(expectedPattern, sb.toString());
                sb = new StringBuilder();
                assertEquals(r, Rational.valueOf(np, nq));
            }
        }
    }
}
