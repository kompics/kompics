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

import java.util.Random;
import org.jscience.mathematics.number.LargeInteger;
import org.jscience.mathematics.number.Rational;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ujmp.core.DenseMatrix;
import org.ujmp.core.Matrix;
import org.ujmp.core.SparseMatrix;
import org.ujmp.core.calculation.Calculation;
import se.sics.kompics.config.Config;

/**
 * Uses the Sarsa algorithm from Reinforcement Learning: An Introduction, figure
 * 6.9, page 146
 * <p>
 * @author lkroll
 */
public class TDRatioLearner implements ProtocolRatioPolicy {

    private static final Logger LOG = LoggerFactory.getLogger(TDRatioLearner.class);

    private final double alpha = 0.1;
    private final double gamma = 0.1;
    private final Rational stepSize;
    private int state;
    private final int[] actions;
    private final Matrix actionStateValues; // Q(s, a)
    private final int stateMiddleIndex;  // 50/50 point
    private final int stateUpperBound; // 1 point
    private final DerivedPolicy policy;
    private int lastAction;
    private int lastState;

    public TDRatioLearner(Config config) {
        this(); // fix later^^
    }

    public TDRatioLearner() {
        policy = new EpsilonGreedy();
        actions = new int[]{-2, -1, 0, 1, 2};
        lastAction = 2; // always start off with 0 movement
        stepSize = Rational.valueOf(1, 5);
        actionStateValues = initialiseMatrix(actions, stepSize);
        int rows = (int) actionStateValues.getRowCount();
        stateMiddleIndex = (int) (rows / 2);
        stateUpperBound = rows - 1; // adjust for 0-indexing
        state = stateMiddleIndex;
        lastState = state; // because last action is 0 movement
    }

    @Override
    public Rational update(double throughput, double deliveryLatency) {
        if (Double.isNaN(throughput)) {
            return stateToRatio(state); // don't update with NaNs
        }
        // rename some variable names to make it easier to follow along the book
        int a = lastAction;
        int s = lastState;
        int sPrime = state;
        double r = throughput;
        int aPrime = policy.chooseAction();
        Matrix Q = actionStateValues;
        double Qsa = Q.getAsDouble(s, a);
        double gammaQsPrimeaPrime = gamma * Q.getAsDouble(sPrime, aPrime);
        double alphaBlock = alpha * (r + gammaQsPrimeaPrime - Qsa);
        double newQsa = Qsa + alphaBlock;
        Q.setAsDouble(newQsa, s, a);
        // update stuff
        lastState = sPrime;
        lastAction = aPrime;
        state = applyAction(aPrime, sPrime);
        Rational ratio = stateToRatio(state);
        LOG.info("Updated learner: r={}, s={}, a={}, s'={}, a'={}, s''={} ({})", new Object[]{r, s, a, sPrime, aPrime, state, ratio});
        return ratio;
    }

    @Override
    public void initialState(Rational initState) {
        state = ratioToState(initState);
    }

    private int ratioToState(Rational ratio) {

        // bound at the edges
        if (ratio.isGreaterThan(Rational.ONE)) {
            return (int) (stateUpperBound);
        }
        if (ratio.isLessThan(Rational.ONE.inverse())) {
            return 0;
        }
        // it's a number in [-1, 1]
        LargeInteger rq = ratio.getDividend().times(stepSize.getDividend());
        Rational rqs = Rational.valueOf(rq, ratio.getDivisor());
        int p = rqs.round().intValue();
        return p + stateMiddleIndex;
    }

    private Rational stateToRatio(int state) {
        long directionAdjusted = state - stateMiddleIndex;
        return stepSize.times(directionAdjusted);
    }

    private int applyAction(int action, int state) {
        int steps = actions[action];
        int newState = state + steps;
        if (newState < 0) {
            return 0;
        } else if (newState > stateUpperBound) {
            return stateUpperBound;
        } else {
            return newState;
        }
    }

    private static Matrix initialiseMatrix(int[] actions, Rational stepSize) {
        int width = actions.length;
        int height = Rational.valueOf(2, 1).divide(stepSize).intValue() + 1;
        LOG.trace("Initialising {}x{} Matrix", height, width);
        if (Math.max(width, height) < 10) { // use dense
            return DenseMatrix.Factory.zeros(height, width);
        } else { // use sparse
            return SparseMatrix.Factory.zeros(height, width);
        }
    }

    static interface DerivedPolicy {

        public int chooseAction();
    }

    class EpsilonGreedy implements DerivedPolicy {

        private double epsilon = 0.5;
        private final double epsilonDelta = 0.01;
        private final double minEpsilon = 0.05;
        private final Random RAND = new Random(1);

        @Override
        public int chooseAction() {
            if (epsilon > minEpsilon) { // decay temperature over time
                epsilon -= epsilonDelta;
            }
            if (RAND.nextDouble() < epsilon) { // select at random
                int action = RAND.nextInt(actions.length);
                LOG.trace("Selected {} randomly.", actions[action]);
                return action;
            } else { // select greedy
                Matrix stateRow = actionStateValues.getRowList().get(state);
                if (stateRow != null) {
                    LOG.trace("Selecting greedily from: {}", stateRow);
                    Matrix maxIndex = stateRow.indexOfMax(Calculation.Ret.NEW, Calculation.COLUMN);
                    LOG.trace("Max is: {}", maxIndex);
                    int action = maxIndex.getAsInt(0, 0);
                    LOG.trace("Selected {}", actions[action]);
                    return action;
                } else { // can't pick greedy if no data...pick random
                    int action = RAND.nextInt(actions.length);
                    LOG.trace("Selected {} randomly (due to lack of data).", actions[action]);
                    return action;
                }
            }
        }

    }
}
