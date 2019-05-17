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

import org.javatuples.Pair;
import org.jscience.mathematics.number.Rational;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Transport;

/**
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public class AlternatingSelection implements ProtocolSelectionPolicy<Msg<?, ?>> {

    private static final Logger LOG = LoggerFactory.getLogger(AlternatingSelection.class);

    private Pattern currentPattern = new PPattern(0, 0);
    private final Transport[] emissionTranslation = new Transport[] { Transport.TCP, Transport.UDT };

    @Override
    public void updateRatio(Rational ratio) {
        if (ratio.isLargerThan(Rational.ONE)) { // |ratio|>|1|
            throw new RuntimeException("Invalid ratio (" + ratio + ")! Must be in [-1,1]");
        }
        if (ratio.isNegative()) {
            emissionTranslation[0] = Transport.UDT;
            emissionTranslation[1] = Transport.TCP;
        } else if (ratio.isPositive()) {
            emissionTranslation[0] = Transport.TCP;
            emissionTranslation[1] = Transport.UDT;
        } // if it's exactly zero the assignment doesn't matter, since it's 50/50 anyway
        Rational posRatio = ratio.abs(); // fold into [0, 1]
        long n = posRatio.getDividend().longValue();
        long m = posRatio.getDivisor().longValue();
        Rational r = Rational.valueOf(m - n, m + n);
        if (r.equals(Rational.ZERO)) {
            currentPattern = new ConstantPattern(Emission.Q);
            LOG.trace("Updating to {} ({}), using ConstantPattern of {}",
                    new Object[] { ratio, r, emissionTranslation[Emission.Q.ordinal()] });
            return;
        }
        long p = r.getDividend().longValue(); // it's fine if it round some precision away here
        long q = r.getDivisor().longValue();
        long pRest = PPattern.estimateQuality(p, q);
        long p1Rest = Pplus1Pattern.estimateQuality(p, q);
        if (p1Rest < pRest) { // pick the pattern with smaller rest, but prefer the simpler PPattern
            if (!currentPattern.getClass().equals(Pplus1Pattern.class)) {
                Pair<Long, Long> counters = currentPattern.getCounters();
                currentPattern = new Pplus1Pattern(counters.getValue0(), counters.getValue1());
            }
        } else {
            if (!currentPattern.getClass().equals(PPattern.class)) {
                Pair<Long, Long> counters = currentPattern.getCounters();
                currentPattern = new PPattern(counters.getValue0(), counters.getValue1());
            }
        }
        currentPattern.updateRatio(p, q);
        LOG.trace("Updating to {} ({}), using {} with p={}, q={}", new Object[] { ratio, r, currentPattern, p, q });
    }

    @Override
    public Transport select(Msg<?, ?> msg) {
        Emission e = currentPattern.select();
        int ePos = e.ordinal();
        return emissionTranslation[ePos];
    }

    public static enum Emission {

        P, Q;
    }

    public static interface Pattern {

        public void updateRatio(long p, long q);

        public Emission select();

        public Pair<Long, Long> getCounters();
    }

    public static class ConstantPattern implements Pattern {

        private final Emission val;

        ConstantPattern(Emission val) {
            this.val = val;
        }

        @Override
        public void updateRatio(long p, long q) {
            throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods,
                                                                           // choose Tools | Templates.
        }

        @Override
        public Emission select() {
            return val;
        }

        @Override
        public Pair<Long, Long> getCounters() {
            return Pair.with(0l, 0l);
        }

    }

    public static class PPattern implements Pattern {

        private long p = 1;
        private long q = 1;
        private long b = 1;
        private long np;
        private long nq;

        PPattern(long np, long nq) {
            this.np = np;
            this.nq = nq;
        }

        @Override
        public void updateRatio(long p, long q) {
            this.p = p;
            this.q = q;
            this.b = q / p; // automatically rounded down
            np = np % p;
            nq = nq % q;
            // System.out.println("P: p="+p+", q="+q+", b="+b);
        }

        @Override
        public Emission select() {
            if ((np == 0 && nq == 0) || (np == p && nq == q)) { // collapsed initial and reset cases
                // System.out.println("P: np="+np+", nq="+nq+" selecting Q (reset rule)");
                np = 0;
                nq = 1;
                return Emission.Q;
            } else if ((nq == (b * (np + 1))) && (np < p)) {
                // System.out.println("P: np="+np+", nq="+nq+" selecting P");
                np++;
                return Emission.P;
            } else {
                // System.out.println("P: np="+np+", nq="+nq+" selecting Q");
                nq++;
                return Emission.Q;
            }
        }

        @Override
        public Pair<Long, Long> getCounters() {
            return Pair.with(np, nq);
        }

        public static long estimateQuality(long p, long q) {
            long b = q / p; // I hope the compiler doesn't optimise this away
            long rest = q - (p * b);
            return Math.abs(b - rest); // p is good if the difference between the block size and the rest is small
        }

    }

    public static class Pplus1Pattern implements Pattern {

        private long p = 1;
        private long q = 1;
        private long b = 1;
        private long np;
        private long nq;

        Pplus1Pattern(long np, long nq) {
            this.np = np;
            this.nq = nq;
        }

        @Override
        public void updateRatio(long p, long q) {
            this.p = p;
            this.q = q;
            this.b = q / (p + 1); // automatically rounded down
            np = np % p;
            nq = nq % q;
            // System.out.println("P1: p="+p+", q="+q+", b="+b);
        }

        @Override
        public Emission select() {
            if ((np == 0 && nq == 0) || (np == p && nq == q)) { // collapsed initial and reset cases
                // System.out.println("P1: np="+np+", nq="+nq+" selecting Q (reset rule)");
                np = 0;
                nq = 1;
                return Emission.Q;
            } else if ((nq == (b * (np + 1))) && (np < p)) {
                // System.out.println("P1: np="+np+", nq="+nq+" selecting P");
                np++;
                return Emission.P;
            } else {
                // System.out.println("P1: np="+np+", nq="+nq+" selecting Q");
                nq++;
                return Emission.Q;
            }
        }

        @Override
        public Pair<Long, Long> getCounters() {
            return Pair.with(np, nq);
        }

        public static long estimateQuality(long p, long q) {
            long b = q / (p + 1); // I hope the compiler doesn't optimise this away
            return q - ((p + 1) * b); // p+1 is good if the rest is small
        }

    }
}
