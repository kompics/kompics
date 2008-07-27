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

	private ChordIterativeRouter router;

	public FingerTable(int log2RingSize, Address localPeer,
			ChordIterativeRouter router) {
		this.router = router;

		this.log2RingSize = log2RingSize;
		ringSize = new BigInteger("2").pow(log2RingSize);
		this.localPeer = localPeer;

		begin = new BigInteger[log2RingSize];
		end = new BigInteger[log2RingSize];
		finger = new Address[log2RingSize];

		initFingerTable();
	}

	private void initFingerTable() {
		for (int i = 0; i < log2RingSize; i++) {
			begin[i] = new BigInteger("2").pow(i).add(localPeer.getId()).mod(
					ringSize);
		}

		for (int i = 0; i < log2RingSize; i++) {
			end[i] = new BigInteger("2").pow(i + 1).add(localPeer.getId()).mod(
					ringSize);
		}

		for (int i = 0; i < log2RingSize; i++) {
			finger[i] = null;
			// try {
			// finger[i] = new Address(InetAddress.getLocalHost(), 1234,
			// begin[i]);
			// } catch (UnknownHostException e) {
			// e.printStackTrace();
			// }
		}
	}

	void fingerFixed(int fingerIndex, Address fingerPeer) {
		fingerIndex--;
		// finger peers have to either null or inside the finger interval
		if (fingerPeer != null
				&& RingMath.belongsTo(fingerPeer.getId(), begin[fingerIndex],
						end[fingerIndex], IntervalBounds.CLOSED_OPEN, ringSize)) {
			finger[fingerIndex] = fingerPeer;

			router.fingerTableChanged();
		}
	}

	void learnedAboutPeer(Address newPeer) {
		// TODO we have learned about this new peer so we check whether it is a
		// better alternative for one of our fingers

		;

		// not self

		// router.fingerTableChanged();
	}

	void fingerSuspected(Address peer) {
		boolean changed = false;

		for (int i = 0; i < finger.length; i++) {
			if (finger[i].equals(peer)) {
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
		return begin[fingerIndex - 1];
	}

	// public void dump() {
	// for (int i = 0; i < log2RingSize; i++) {
	// System.err.println((i + 1) + "[" + begin[i] + ", " + end[i]
	// + ") -> " + finger[i]);
	// }
	// }
	//
	// public static void main(String args[]) throws UnknownHostException {
	// FingerTable ft = new FingerTable(8, new Address(InetAddress
	// .getLocalHost(), 1234, BigInteger.ZERO));
	//
	// ft.dump();
	//
	// System.out.println(ft.closestPreceedingPeer(new BigInteger("5")));
	// }
}
