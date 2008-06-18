package se.sics.kompics.p2p.son.ps;

import java.io.Serializable;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
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

		// ListIterator<Address> iter = successors.listIterator();
		// int i = 0;
		// while (iter.hasNext()) {
		// if (iter.next().equals(localPeer))
		// break;
		// i++;
		// }
		// while (successors.size() > i + 1)
		// successors.remove(successors.size() - 1);
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

	public static void main(String[] args) throws UnknownHostException {
		Address a1 = new Address(InetAddress.getByName("127.0.0.1"), 1234,
				new BigInteger("1"));
		Address a2 = new Address(InetAddress.getByName("127.0.0.1"), 1234,
				new BigInteger("2"));

		ArrayList<Address> l1 = new ArrayList<Address>();
		ArrayList<Address> l2 = new ArrayList<Address>();

		l1.add(a1);
		l1.add(a2);

		l2.add(a1);
		l2.add(a1);

		System.err.println("l1 is " + l1);
		System.err.println("l2 is " + l2);

		SuccessorList s1 = new SuccessorList(10, a1, l1);
		SuccessorList s2 = new SuccessorList(10, a2, l2);

		try {
			s1.updateSuccessorList(s2);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("s1 is " + s1.getSuccessors());
		try {
			s2.updateSuccessorList(s1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("s2 is " + s2.getSuccessors());
	}
}
