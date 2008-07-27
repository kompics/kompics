package se.sics.kompics.p2p.chord.router;

import java.math.BigInteger;

import se.sics.kompics.network.Address;

/**
 * The <code>FingerTableView</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: FingerTableView.java 139 2008-06-04 10:55:59Z cosmin $
 */
public class FingerTableView {

	public final Address ownerPeer;

	public final BigInteger[] begin;

	public final BigInteger[] end;

	public final Address[] finger;

	public FingerTableView(Address ownerPeer, BigInteger[] begin,
			BigInteger[] end, Address[] fingers) {
		this.ownerPeer = ownerPeer;
		this.begin = begin;
		this.end = end;
		this.finger = fingers;
	}
}
