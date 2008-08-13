package se.sics.kompics.p2p.chord.router;

import java.math.BigInteger;

import se.sics.kompics.network.Address;
import se.sics.kompics.p2p.chord.IntervalBounds;
import se.sics.kompics.p2p.chord.RingMath;

/**
 * The <code>FingerTable</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: FingerTable.java 139 2008-06-04 10:55:59Z cosmin $
 */
public class FingerTable {

	private int log2RingSize;

	private BigInteger ringSize;

	private Address localPeer;

	private BigInteger[] begin;

	private BigInteger[] end;

	private Address[] finger;

	private boolean[] skip;

	private ChordIterativeRouter router;

	private int nextFingerToFix;

	public FingerTable(int log2RingSize, Address localPeer,
			ChordIterativeRouter router) {
		this.router = router;

		this.log2RingSize = log2RingSize;
		ringSize = new BigInteger("2").pow(log2RingSize);
		this.localPeer = localPeer;

		begin = new BigInteger[log2RingSize];
		end = new BigInteger[log2RingSize];
		finger = new Address[log2RingSize];
		skip = new boolean[log2RingSize];

		nextFingerToFix = -1;

		initFingerTable();
	}

	private void initFingerTable() {
		for (int i = 0; i < log2RingSize; i++) {
			begin[i] = new BigInteger("2").pow(i).add(localPeer.getId()).mod(
					ringSize);
			end[i] = new BigInteger("2").pow(i + 1).add(localPeer.getId()).mod(
					ringSize);
			finger[i] = null;
			skip[i] = false;
		}
	}

	int nextFingerToFix() {
		do {
			if (nextFingerToFix > 0) {
				skip[nextFingerToFix] = false;
			}
			nextFingerToFix++;
			if (nextFingerToFix >= log2RingSize) {
				nextFingerToFix = 0;
			}
		} while (skip[nextFingerToFix]);
		return nextFingerToFix;
	}

	void fingerFixed(int f, Address fingerPeer) {
		if (fingerPeer == null || fingerPeer.equals(localPeer)) {
			skip[f] = false;
			return;
		}

		while (!RingMath.belongsTo(fingerPeer.getId(), begin[f], end[f],
				IntervalBounds.CLOSED_OPEN, ringSize)) {
			skip[f] = false;
			f++;
			if (f >= log2RingSize) {
				f = 0;
			}
		}
		skip[f] = false;
		finger[f] = fingerPeer;

		nextFingerToFix = f;

		// // we find the actual finger being fixed
		// int actualFinger = f;
		// for (int i = 0; i < log2RingSize; i++) {
		// if (RingMath.belongsTo(fingerPeer.getId(), begin[i], end[i],
		// IntervalBounds.CLOSED_OPEN, ringSize)) {
		// // we got finger i
		// actualFinger = i;
		// break;
		// }
		// }
		//
		// // we have now fixed all fingers between the one that was supposed to
		// // be fixed and the one actually fixed since there is no node in
		// between
		// // thus we mark the fact that they should not be skipped in the next
		// // round
		// int i = f;
		// while (i != actualFinger) {
		// skip[i] = false;
		// i++;
		// if (i >= log2RingSize) {
		// i = 0;
		// }
		// }
		// skip[actualFinger] = false;
		//
		// // we actually fix the finger now
		// if (finger[actualFinger] == null
		// || !finger[actualFinger].equals(fingerPeer)) {
		// finger[actualFinger] = fingerPeer;
		// router.fingerTableChanged();
		// }
		//
		// nextFingerToFix = actualFinger;
	}

	boolean learnedAboutFreshPeer(Address newPeer) {
		return learnedAboutPeer(newPeer, true, true);
	}

	boolean learnedAboutPeer(Address newPeer) {
		return learnedAboutPeer(newPeer, true, false);
	}

	boolean learnedAboutPeer(Address newPeer, boolean update, boolean fresh) {
		// we have learned about this new peer so we check whether it is a
		// better alternative for one of our fingers

		if (newPeer == null || newPeer.equals(localPeer)) {
			return false;
		}

		BigInteger distance = RingMath.modMinus(newPeer.getId(), localPeer
				.getId(), ringSize);
		int i = distance.bitLength() - 1;

		boolean changed = false;

		// for (int i = 0; i < log2RingSize; i++) {
		if (RingMath.belongsTo(newPeer.getId(), begin[i], end[i],
				IntervalBounds.CLOSED_OPEN, ringSize)) {
			// it belongs to this interval
			// we update no matter what
			if (finger[i] == null
					|| (fresh && RingMath.belongsTo(newPeer.getId(), begin[i],
							finger[i].getId(), IntervalBounds.CLOSED_OPEN,
							ringSize))) {
				// it is closer to the beginning of the interval than
				// the current finger
				finger[i] = newPeer;
				// we skip this finger in the next Fixfingers attempt
				skip[i] = true;
				changed = true;
			}
			// break;
		}
		// }

		if (changed && update) {
			router.fingerTableChanged();
		}
		return changed;
	}

	void fingerSuspected(Address peer) {
		boolean changed = false;

		for (int i = 0; i < finger.length; i++) {
			if (finger[i] != null && finger[i].equals(peer)) {
				finger[i] = null;
				changed = true;
			}
		}

		if (changed) {
			router.fingerTableChanged();
		}
	}

	Address closestPreceedingPeer(BigInteger key) {
		for (int i = log2RingSize - 1; i >= 0; i--) {
			if (finger[i] != null
					&& RingMath.belongsTo(finger[i].getId(), localPeer.getId(),
							key, IntervalBounds.OPEN_OPEN, ringSize))
				return finger[i];
		}
		return localPeer;
	}

	FingerTableView getView() {
		FingerTableView view = new FingerTableView(localPeer, begin.clone(),
				end.clone(), finger.clone());
		return view;
	}

	public BigInteger getFingerBegin(int fingerIndex) {
		return begin[fingerIndex];
	}
}
