package se.sics.kompics.p2p.son.ps;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.ListIterator;

import se.sics.kompics.network.Address;

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

	public SuccessorList(int length, Address localPeer,
			ArrayList<Address> successors) {
		super();
		this.length = length;
		this.localPeer = localPeer;
		this.successors = successors;
	}

	public void updateSuccessorList(SuccessorList list) {
		successors = list.getSuccessors();
		if (successors.size() > 0) {
			if (!successors.get(0).equals(list.getLocalPeer())) {
				successors.add(0, list.getLocalPeer());
			}
		} else {
			successors.add(0, list.getLocalPeer());
		}

		// trim to length
		while (successors.size() > length) {
			successors.remove(length);
		}

		// trim to ring
		Address succ = successors.get(0);
		ListIterator<Address> iter = successors.listIterator();
		iter.next();
		int i = 1;
		while (iter.hasNext()) {
			if (iter.next().equals(succ)) {
				break;
			} else {
				i++;
			}
		}
		if (i < successors.size()) {
			successors.remove(i);
		}
	}

	public void successorFailed(Address peer) {
		successors.remove(peer);
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
}
