package se.sics.kompics.p2p.chord.router;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;

import se.sics.kompics.network.Address;

public class FingerTableTest extends TestCase {

	FingerTable ft;

	FingerTableView view;

	Address self;

	int size;

	@Before
	public void setUp() throws Exception {
		size = 6;
		self = makeAddress(10);
		ft = new FingerTable(size, self, null);
	}

	private void printFT() {
		view = ft.getView();
		for (int i = 0; i < size; i++) {
			System.out.print("f[" + i + "]=[" + view.begin[i] + ", "
					+ view.end[i] + ") is " + view.finger[i]);
			System.out.println();
		}
		System.out.println("---------------------------------------");
	}

	private Address makeAddress(int x) {
		try {
			return new Address(InetAddress.getByName("127.0.0.1"), x,
					new BigInteger("" + x));
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return null;
		}
	}

	private void fixOneFinger(FingerTableView v) {
		int next = ft.nextFingerToFix();
		System.out.print("Next f: " + next);
		ft.fingerFixed(next, makeAddress(v.end[next].intValue() - 1));
		System.out.println();
	}

	@Ignore
	public void test1() {
		// printFT();
		// ft.learnedAboutPeer(makeAddress(11), false, true); // 0
		ft.learnedAboutPeer(makeAddress(60), false, true); // 5
		// ft.learnedAboutPeer(makeAddress(13), false, true); // 1
		ft.learnedAboutPeer(makeAddress(41), false, true); // 4
		ft.learnedAboutPeer(makeAddress(17), false, true); // 2
		ft.learnedAboutPeer(makeAddress(27), false, true); // 3
		printFT();

		for (int i = 0; i < 10; i++) {
			fixOneFinger(ft.getView());
		}
		ft.learnedAboutPeer(makeAddress(12), false, true); // 1

		printFT();
		for (int i = 0; i < 3; i++) {
			fixOneFinger(ft.getView());
		}

		// System.out.println(ft.getView().ownerPeer);
		// System.out.println(makeAddress(10));
		//
		// ft.fingerFixed(3, makeAddress(10));
	}

	// public void test2() {
	// printFT();
	// ft.fingerFixed(0, makeAddress(11));
	// fixOneFinger(ft.getView());
	// printFT();
	// }

	@After
	public void tearDown() throws Exception {
	}
}
