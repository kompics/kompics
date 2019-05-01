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

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
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
 * Uses the Sarsa algorithm from Reinforcement Learning: An Introduction, figure 6.9, page 146
 * <p>
 * 
 * @author lkroll
 */
public class TDRatioLearner implements ProtocolRatioPolicy {

    private static final Logger LOG = LoggerFactory.getLogger(TDRatioLearner.class);

    private final Config config;
    private final double alpha;
    private final double gamma;
    private final double lambda;
    private final Rational stepSize;
    private int state;
    private final int[] actions;
    private final ActionValueEstimator Q; // reward estimation
    private final ActionValueEstimator e; // eligibility trace
    private final DerivedPolicy policy;
    private int lastAction;
    private int lastState;

    public TDRatioLearner(Config conf) {
        config = conf;
        alpha = config.getValue("kompics.net.data.td.alpha", Double.class);
        gamma = config.getValue("kompics.net.data.td.gamma", Double.class);
        lambda = config.getValue("kompics.net.data.td.lambda", Double.class);
        long iStepSize = config.getValue("kompics.net.data.td.stepSize", Long.class);
        stepSize = Rational.valueOf(1, iStepSize);
        List<Integer> basicActions = config.getValues("kompics.net.data.td.actions", Integer.class);
        actions = completeActions(basicActions);
        ActionValueEstimator.Implementation qImpl = ActionValueEstimator.Implementation
                .valueOf(config.getValue("kompics.net.data.td.actionValueEstimator", String.class));
        AVEFactory avef = new AVEFactory();
        Q = avef.getInstance(qImpl, stepSize);
        e = avef.getInstance(ActionValueEstimator.Implementation.MATRIX, stepSize);
        LOG.trace("Initialised TD with actions {}, and {} states, stepSize {}",
                new Object[] { Arrays.toString(actions), Q.numStates(), stepSize });
        policy = new EpsilonGreedy();
        lastAction = actions[actions.length / 2]; // always start off with 0 movement
        state = Q.middleState();
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
        double Qsa = Q.at(s, a);
        double delta = r + gamma * Q.at(sPrime, aPrime) - Qsa;
        // accumulating trace
        // double esa = e.at(s, a);
        // e.set(esa + 1.0, s, a); // update eligibility trace
        // replacting trace (loose)
        e.set(1.0, s, a);
        // replaceing trace (strict)
        for (int j = 0; j < actions.length; j++) {
            if (a != j) {
                e.set(0.0, s, j);
            }
        }
        for (int i = 0; i < Q.numStates(); i++) {
            for (int j = 0; j < actions.length; j++) {
                double eij = e.at(i, j);
                double Qij = Q.at(i, j);
                double newQij = Qij + alpha * delta * eij;
                double neweij = gamma * lambda * eij;
                Q.set(newQij, i, j);
                e.set(neweij, i, j);
            }
        }
        // double alphaBlock = alpha * (r + gammaQsPrimeaPrime - Qsa);
        // double newQsa = Qsa + alphaBlock;
        // Q.set(newQsa, s, a);
        // update stuff
        lastState = sPrime;
        lastAction = aPrime;
        state = applyAction(aPrime, sPrime);
        Rational ratio = stateToRatio(state);
        LOG.info("Updated learner: r={}, s={}, a={}, s'={}, a'={}, s''={} ({}), \n Q={} \n e={}",
                new Object[] { r, s, a, sPrime, aPrime, state, ratio, Q, e });
        return ratio;
    }

    @Override
    public void initialState(Rational initState) {
        state = ratioToState(initState);
    }

    private int ratioToState(Rational ratio) {

        // bound at the edges
        if (ratio.isGreaterThan(Rational.ONE)) {
            return (int) (Q.maxState());
        }
        if (ratio.isLessThan(Rational.ONE.inverse())) {
            return 0;
        }
        // it's a number in [-1, 1]
        LargeInteger rq = ratio.getDividend().times(stepSize.getDividend());
        Rational rqs = Rational.valueOf(rq, ratio.getDivisor());
        int p = rqs.round().intValue();
        return p + Q.middleState();
    }

    private Rational stateToRatio(int state) {
        long directionAdjusted = state - Q.middleState();
        return stepSize.times(directionAdjusted);
    }

    private int applyAction(int action, int state) {
        int steps = actions[action];
        int newState = state + steps;
        if (newState < 0) {
            return 0;
        } else if (newState > Q.maxState()) {
            return Q.maxState();
        } else {
            return newState;
        }
    }

    private static int[] completeActions(List<Integer> basicActions) {
        List<Integer> actionsCompletion = new ArrayList<Integer>(2 * basicActions.size() + 1);
        boolean sawZero = false;
        for (Integer action : basicActions) {
            actionsCompletion.add(action);
            if (action != 0) {
                actionsCompletion.add(-action);
            } else {
                sawZero = true;
            }
        }
        if (!sawZero) {
            actionsCompletion.add(0);
        }
        int[] actions = new int[actionsCompletion.size()];
        for (int i = 0; i < actions.length; i++) {
            actions[i] = actionsCompletion.get(i);
        }
        Arrays.sort(actions);
        return actions;
    }

    static interface ActionValueEstimator {

        public int numStates();

        public int bestActionAt(int state);

        public int randomActionAt(int state, Random rand);

        public double at(int state, int action);

        public void set(double val, int state, int action);

        public int middleState();

        public int maxState();

        public static enum Implementation {

            MATRIX, COLLAPSED, FUNCTION;
        }
    }

    class AVEFactory {

        public ActionValueEstimator getInstance(ActionValueEstimator.Implementation impl, Rational stepSize) {
            switch (impl) {
            case MATRIX:
                return new BasicMatrixEstimator(stepSize);
            case COLLAPSED:
                return new CollapsedMatrixEstimator(stepSize);
            case FUNCTION:
                return new FunctionApproximationEstimator(stepSize);
            default:
                return null;
            }
        }
    }

    class BasicMatrixEstimator implements ActionValueEstimator {

        private final int stateMiddleIndex; // 50/50 point
        private final int height;
        private final int width;
        private final Matrix m;

        public BasicMatrixEstimator(Rational stepSize) {
            width = actions.length;
            height = Rational.valueOf(2, 1).divide(stepSize).intValue() + 1;
            LOG.trace("Initialising {}x{} Matrix", height, width);
            if (Math.max(width, height) < 10) { // use dense
                m = DenseMatrix.Factory.zeros(height, width);
            } else { // use sparse
                m = SparseMatrix.Factory.zeros(height, width);
            }
            stateMiddleIndex = height / 2;
        }

        @Override
        public int numStates() {
            return height;
        }

        @Override
        public double at(int state, int action) {
            return m.getAsDouble(state, action);
        }

        @Override
        public void set(double val, int state, int action) {
            m.setAsDouble(val, state, action);
        }

        @Override
        public int middleState() {
            return this.stateMiddleIndex;
        }

        @Override
        public int maxState() {
            return this.height - 1;
        }

        @Override
        public String toString() {
            return "BasicMatrixEstimator with Q=\n" + m;
        }

        @Override
        public int bestActionAt(int state) {
            Matrix stateRow = m.getRowList().get(state);
            if (stateRow == null) {
                stateRow = SparseMatrix.Factory.zeros(1, actions.length);
            }
            Matrix maxIndex = stateRow.indexOfMax(Calculation.Ret.NEW, Calculation.COLUMN);
            // LOG.trace("Max is: {}", maxIndex);
            return maxIndex.getAsInt(0, 0);
        }

        @Override
        public int randomActionAt(int state, Random rand) {
            Multimap<Integer, Integer> stateActions = TreeMultimap.create();
            for (int i = 0; i < actions.length; i++) {
                int s = state + actions[i];
                if (s < 0) {
                    s = 0;
                } else if (s >= height) {
                    s = maxState();
                }
                stateActions.put(s, i);
            }
            Object[] resultStates = stateActions.keySet().toArray();
            int target = (int) resultStates[rand.nextInt(resultStates.length)];
            Collection<Integer> targetActions = stateActions.get(target);
            int bestAction = Integer.MAX_VALUE;
            for (Integer action : targetActions) {
                if (Math.abs(action) < Math.abs(bestAction)) {
                    bestAction = action;
                }
            }
            return bestAction;
        }
    }

    class CollapsedMatrixEstimator implements ActionValueEstimator {

        private final int stateMiddleIndex; // 50/50 point
        private final double[] m;

        public CollapsedMatrixEstimator(Rational stepSize) {
            int height = Rational.valueOf(2, 1).divide(stepSize).intValue() + 1;
            m = new double[height];
            Arrays.fill(m, 0.0);
            stateMiddleIndex = height / 2;
        }

        @Override
        public int numStates() {
            return m.length;
        }

        @Override
        public double at(int state, int action) {
            int s = state + actions[action];
            if (s < 0) {
                s = 0;
            } else if (s >= m.length) {
                s = m.length - 1;
            }
            return m[s];
        }

        @Override
        public void set(double val, int state, int action) {
            int s = state + actions[action];
            if (s < 0) {
                s = 0;
            } else if (s >= m.length) {
                s = m.length - 1;
            }
            m[s] = val;
        }

        @Override
        public int middleState() {
            return stateMiddleIndex;
        }

        @Override
        public int maxState() {
            return m.length - 1;
        }

        @Override
        public int bestActionAt(int state) {
            Multimap<Integer, Integer> stateActions = TreeMultimap.create();
            for (int i = 0; i < actions.length; i++) {
                int s = state + actions[i];
                if (s < 0) {
                    s = 0;
                } else if (s >= m.length) {
                    s = maxState();
                }
                stateActions.put(s, i);
            }
            int bestState = state;
            double bestValue = m[state];
            for (Integer possibleState : stateActions.keys()) {
                double val = m[possibleState];
                if (val > bestValue) {
                    bestState = possibleState;
                    bestValue = val;
                }
            }
            LOG.trace("Of the target states {}, the best is {} (val: {}) in {}",
                    new Object[] { stateActions, bestState, bestValue, Arrays.toString(m) });
            Collection<Integer> targetActions = stateActions.get(bestState);
            int bestAction = Integer.MAX_VALUE;
            for (Integer action : targetActions) {
                if (Math.abs(action) < Math.abs(bestAction)) {
                    bestAction = action;
                }
            }
            return bestAction;
        }

        @Override
        public int randomActionAt(int state, Random rand) {
            Multimap<Integer, Integer> stateActions = TreeMultimap.create();
            for (int i = 0; i < actions.length; i++) {
                int s = state + actions[i];
                if (s < 0) {
                    s = 0;
                } else if (s >= m.length) {
                    s = maxState();
                }
                stateActions.put(s, i);
            }
            Object[] resultStates = stateActions.keySet().toArray();
            int target = (int) resultStates[rand.nextInt(resultStates.length)];
            Collection<Integer> targetActions = stateActions.get(target);
            int bestAction = Integer.MAX_VALUE;
            for (Integer action : targetActions) {
                if (Math.abs(action) < Math.abs(bestAction)) {
                    bestAction = action;
                }
            }
            return bestAction;
        }

        @Override
        public String toString() {
            return "CollapsedMatrixEstimator with Q=\n" + Arrays.toString(m);
        }
    }

    class FunctionApproximationEstimator implements ActionValueEstimator {

        private final int stateMiddleIndex; // 50/50 point
        private final double[] m;
        private PolynomialFunction approxFunction;

        public FunctionApproximationEstimator(Rational stepSize) {
            int height = Rational.valueOf(2, 1).divide(stepSize).intValue() + 1;
            m = new double[height];
            Arrays.fill(m, 0.0);
            stateMiddleIndex = height / 2;
            approxFunction = new PolynomialFunction(new double[] { 0.0 }); // f(x) = 0
        }

        private void updateFunction() {
            final WeightedObservedPoints obs = new WeightedObservedPoints();
            int nonZeroValues = 0;
            double highestValue = 0.0;
            for (int i = 0; i < m.length; i++) {
                double v = m[i];
                if (v != 0.0) {
                    nonZeroValues++;
                    obs.add((double) i, v);
                    if (v > highestValue) {
                        highestValue = v;
                    }
                }
            }
            if (nonZeroValues == 0) {
                return; // can't do anything without data
            }
            if (nonZeroValues == 1) {
                // not enough data points for anything but a constant line (which doesn't help)
                // => cheat and force the algorithm to greedily explore
                obs.add(-1.0, 2 * highestValue);
                nonZeroValues++;
            }
            if (nonZeroValues == 2) {
                // not enough for a quadratic function, but enough for a line
                final PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);
                final double[] coeff = fitter.fit(obs.toList());
                approxFunction = new PolynomialFunction(coeff);
                return;
            }
            // fit a quadratic function
            final PolynomialCurveFitter fitter = PolynomialCurveFitter.create(2);
            final double[] coeff = fitter.fit(obs.toList());
            approxFunction = new PolynomialFunction(coeff);
        }

        @Override
        public int numStates() {
            return m.length;
        }

        @Override
        public double at(int state, int action) {
            int s = state + actions[action];
            if (s < 0) {
                s = 0;
            } else if (s >= m.length) {
                s = m.length - 1;
            }
            return m[s];
        }

        @Override
        public void set(double val, int state, int action) {
            int s = state + actions[action];
            if (s < 0) {
                s = 0;
            } else if (s >= m.length) {
                s = m.length - 1;
            }
            m[s] = val;
        }

        @Override
        public int middleState() {
            return stateMiddleIndex;
        }

        @Override
        public int maxState() {
            return m.length - 1;
        }

        @Override
        public int bestActionAt(int state) {
            boolean functionUpdated = false;

            Multimap<Integer, Integer> stateActions = TreeMultimap.create();
            for (int i = 0; i < actions.length; i++) {
                int s = state + actions[i];
                if (s < 0) {
                    s = 0;
                } else if (s >= m.length) {
                    s = maxState();
                }
                stateActions.put(s, i);
            }
            int bestState = state;
            double bestValue = m[state];
            TreeMap<Integer, Double> approxValues = new TreeMap<>();
            for (Integer possibleState : stateActions.keys()) {
                double val = m[possibleState];
                if (val == 0.0) {
                    if (!functionUpdated) {
                        updateFunction();
                        functionUpdated = true;
                    }
                    val = approxFunction.value(possibleState);
                }
                approxValues.put(possibleState, val);
                if (val > bestValue) {
                    bestState = possibleState;
                    bestValue = val;
                }
            }
            LOG.trace("Of the target states {} with values {}, the best is {} (val: {}) in {}",
                    new Object[] { stateActions, approxValues, bestState, bestValue, Arrays.toString(m) });
            Collection<Integer> targetActions = stateActions.get(bestState);
            int bestAction = Integer.MAX_VALUE;
            for (Integer action : targetActions) {
                if (Math.abs(action) < Math.abs(bestAction)) {
                    bestAction = action;
                }
            }
            return bestAction;
        }

        @Override
        public int randomActionAt(int state, Random rand) {
            Multimap<Integer, Integer> stateActions = TreeMultimap.create();
            for (int i = 0; i < actions.length; i++) {
                int s = state + actions[i];
                if (s < 0) {
                    s = 0;
                } else if (s >= m.length) {
                    s = maxState();
                }
                stateActions.put(s, i);
            }
            Object[] resultStates = stateActions.keySet().toArray();
            int target = (int) resultStates[rand.nextInt(resultStates.length)];
            Collection<Integer> targetActions = stateActions.get(target);
            int bestAction = Integer.MAX_VALUE;
            for (Integer action : targetActions) {
                if (Math.abs(action) < Math.abs(bestAction)) {
                    bestAction = action;
                }
            }
            return bestAction;
        }

        @Override
        public String toString() {
            return "CollapsedMatrixEstimator with Q=\n" + Arrays.toString(m);
        }
    }

    static interface DerivedPolicy {

        public int chooseAction();
    }

    class EpsilonGreedy implements DerivedPolicy {

        private double epsilon;
        private final double epsilonDelta;
        private final double minEpsilon;
        private final Random RAND = new Random(1);

        EpsilonGreedy() {
            epsilon = config.getValue("kompics.net.data.td.epsilonGreedy.epsilon", Double.class);
            epsilonDelta = config.getValue("kompics.net.data.td.epsilonGreedy.epsilonDelta", Double.class);
            minEpsilon = config.getValue("kompics.net.data.td.epsilonGreedy.minEpsilon", Double.class);
        }

        @Override
        public int chooseAction() {
            if (epsilon > minEpsilon) { // decay temperature over time
                epsilon = Math.max(epsilon - epsilonDelta, minEpsilon);
            }
            if (RAND.nextDouble() < epsilon) { // select at random
                int action = Q.randomActionAt(state, RAND);
                LOG.trace("Selected {} randomly.", actions[action]);
                return action;
            } else { // select greedy
                int action = Q.bestActionAt(state);
                LOG.trace("Selected {} greedily", actions[action]);
                return action;
            }
        }

    }
}
