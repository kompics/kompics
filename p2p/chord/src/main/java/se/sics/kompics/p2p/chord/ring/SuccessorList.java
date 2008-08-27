package se.sics.kompics.p2p.chord.ring;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;

import se.sics.kompics.network.Address;
import se.sics.kompics.p2p.chord.IntervalBounds;
import se.sics.kompics.p2p.chord.RingMath;

/**
 * The <code>SuccessorList</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: SuccessorList.java 158 2008-06-16 10:42:01Z Cosmin $
 */
public class SuccessorList implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7424183854576208482L;

	private final int length;

	private final Address localPeer;

	private ArrayList<Address> successors;

	private BigInteger ringSize;

	public SuccessorList(int length, Address localPeer, BigInteger ringSize) {
		super();
		this.length = length;
		this.localPeer = localPeer;
		this.successors = new ArrayList<Address>(length + 1);
		this.successors.add(localPeer);

		this.ringSize = ringSize;
	}

	public Address getSuccessor() {
		// if (successors.size() == 0)
		// return null;
		return successors.get(0);
	}

	public void setSuccessor(Address succ) {
		if (!successors.get(0).equals(succ)) {
			successors.add(0, succ);
			trimToLength();
			trimSelf();
		}
	}

	public void updateSuccessorList(SuccessorList list) {
		ArrayList<Address> myList = new ArrayList<Address>(successors);
		ArrayList<Address> theirList = list.getSuccessors();

		// we merge the 2 lists
		int m = 0, t = 0, f = 0;
		Address mine, theirs, last = list.getLocalPeer();

		successors.clear();

		successors.add(f++, last);

		while (f < length && m < myList.size() && t < theirList.size()) {
			mine = myList.get(m);
			theirs = theirList.get(t);

			if (RingMath.belongsTo(mine.getId(), last.getId(), theirs.getId(),
					IntervalBounds.OPEN_OPEN, ringSize)) {
				if (RingMath.belongsTo(mine.getId(), last.getId(), localPeer
						.getId(), IntervalBounds.OPEN_OPEN, ringSize)) {
					successors.add(f++, mine);
					last = mine;
				}
				m++;
			} else if (RingMath.belongsTo(theirs.getId(), last.getId(), mine
					.getId(), IntervalBounds.OPEN_OPEN, ringSize)) {
				if (RingMath.belongsTo(theirs.getId(), last.getId(), localPeer
						.getId(), IntervalBounds.OPEN_OPEN, ringSize)) {
					successors.add(f++, theirs);
					last = theirs;
				}
				t++;
			} else {
				m++;
				t++;
			}

		}

		while (f < length && m < myList.size()) {
			mine = myList.get(m);
			if (RingMath.belongsTo(mine.getId(), last.getId(), localPeer
					.getId(), IntervalBounds.OPEN_OPEN, ringSize)) {
				successors.add(f++, mine);
				last = mine;
			}
			m++;
		}

		while (f < length && t < theirList.size()) {
			theirs = theirList.get(t);
			if (RingMath.belongsTo(theirs.getId(), last.getId(), localPeer
					.getId(), IntervalBounds.OPEN_OPEN, ringSize)) {
				successors.add(f++, theirs);
				last = theirs;
			}
			t++;
		}

		while (successors.size() > f) {
			successors.remove(f);
		}

		// if (successors.get(0).equals(localPeer) && successors.size() > 1) {
		// successors.remove(0);
		// }

		// trimToLength();

		// trim to ring
		// Address succ = successors.get(0);
		// ListIterator<Address> iter = successors.listIterator();
		// iter.next();
		// int i = 1;
		// while (iter.hasNext()) {
		// if (iter.next().equals(succ)) {
		// break;
		// } else {
		// i++;
		// }
		// }
		// if (i < successors.size()) {
		// successors.remove(i);
		// }
	}

	// public void updateSuccessorList(SuccessorList list) {
	// successors = list.getSuccessors();
	// if (successors.size() > 0) {
	// if (!successors.get(0).equals(list.getLocalPeer())) {
	// successors.add(0, list.getLocalPeer());
	// }
	// } else {
	// successors.add(0, list.getLocalPeer());
	// }
	//
	// trimToLength();
	//
	// // trim to ring
	// Address succ = successors.get(0);
	// ListIterator<Address> iter = successors.listIterator();
	// iter.next();
	// int i = 1;
	// while (iter.hasNext()) {
	// if (iter.next().equals(succ)) {
	// break;
	// } else {
	// i++;
	// }
	// }
	// if (i < successors.size()) {
	// successors.remove(i);
	// }
	// }

	public void successorFailed(Address peer) {
		successors.remove(peer);

		if (successors.size() == 0) {
			// last successor died
			successors.add(localPeer);
		}
	}

	public int getLength() {
		return length;
	}

	public Address getLocalPeer() {
		return localPeer;
	}

	public ArrayList<Address> getSuccessors() {
		return successors;
	}

	public ArrayList<Address> getSuccessorListView() {
		return new ArrayList<Address>(successors);
	}

	private void trimToLength() {
		while (successors.size() > length) {
			successors.remove(length);
		}
	}

	private void trimSelf() {
		if (successors.size() > 1
				&& successors.get(successors.size() - 1).equals(localPeer)) {
			successors.remove(successors.size() - 1);
		}
	}
}
