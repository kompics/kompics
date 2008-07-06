package se.sics.kompics.p2p.son.ps;

import java.math.BigInteger;

import se.sics.kompics.network.Address;

public class RingMath {

	// x belongs to (from, to)
	public static boolean belongsTo(BigInteger x, Address fromAddress,
			Address toAddress, IntervalBounds bounds, BigInteger ringSize) {
		BigInteger from = fromAddress.getId();
		BigInteger to = toAddress.getId();

		BigInteger ny = modMinus(to, from, ringSize);
		BigInteger nx = modMinus(x, from, ringSize);

		switch (bounds) {
		case OPEN_OPEN:
			return ((from.equals(to) && !x.equals(from)) || (nx
					.compareTo(BigInteger.ZERO) > 0 && nx.compareTo(ny) < 0));
		case OPEN_CLOSED:
			return (from.equals(to) || (nx.compareTo(BigInteger.ZERO) > 0 && nx
					.compareTo(ny) <= 0));
		case CLOSED_OPEN:
			return (from.equals(to) || (nx.compareTo(BigInteger.ZERO) >= 0 && nx
					.compareTo(ny) < 0));
		case CLOSED_CLOSED:
			return ((from.equals(to) && x.equals(from)) || (nx
					.compareTo(BigInteger.ZERO) >= 0 && nx.compareTo(ny) <= 0));
		}
		return (from.equals(to) || (nx.compareTo(BigInteger.ZERO) > 0 && nx
				.compareTo(ny) <= 0));
	}

	private static BigInteger modMinus(BigInteger x, BigInteger y,
			BigInteger ringSize) {
		return ringSize.add(x).subtract(y).mod(ringSize);
	}
}
