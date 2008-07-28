package se.sics.kompics.p2p.chord.ring;

import java.math.BigInteger;
import java.net.InetAddress;

import junit.framework.TestCase;

import org.junit.Before;

import se.sics.kompics.network.Address;

public class SuccessorListTest extends TestCase {

	SuccessorList list1, list2, list3;

	@Before
	public void setUp() throws Exception {
		InetAddress ip = InetAddress.getByName("127.0.0.1");
		Address addr1 = new Address(ip, 1234, new BigInteger("1"));
		Address addr2 = new Address(ip, 1234, new BigInteger("2"));
		Address addr3 = new Address(ip, 1234, new BigInteger("3"));

		list1 = new SuccessorList(10, addr1, new BigInteger("1024"));
		list2 = new SuccessorList(10, addr2, new BigInteger("1024"));
		list3 = new SuccessorList(10, addr3, new BigInteger("1024"));

		list1.getSuccessors().set(0, addr2);
		// list1.getSuccessors().add(1, addr3);
		// list1.getSuccessors().add(2, addr1);

		list2.getSuccessors().set(0, addr3);
		list2.getSuccessors().add(1, addr1);
		// list2.getSuccessors().add(2, addr2);

		list3.getSuccessors().set(0, addr1);
		// list3.getSuccessors().add(1, addr2);
	}

	public void testUpdate() {
		System.out.println("Before 1: " + list1.getSuccessors());
		System.out.println("Before 2: " + list2.getSuccessors());
		System.out.println("Before 3: " + list3.getSuccessors());

		list3.updateSuccessorList(list1);
		// list2.updateSuccessorList(list3);
		// list1.updateSuccessorList(list2);

		System.out.println("After 1: " + list1.getSuccessors());
		System.out.println("After 2: " + list2.getSuccessors());
		System.out.println("After 3: " + list3.getSuccessors());
	}
}
