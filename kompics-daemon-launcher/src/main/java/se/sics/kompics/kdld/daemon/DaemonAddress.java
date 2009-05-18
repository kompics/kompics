package se.sics.kompics.kdld.daemon;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.overlay.OverlayAddress;


public final class DaemonAddress extends OverlayAddress implements
		Comparable<DaemonAddress> {

	private static final long serialVersionUID = 7243446311710133136L;
	private final Integer daemonId;

	public DaemonAddress(Integer daemonId, Address address) {
		super(address);
		this.daemonId = daemonId;
	}

	public final Integer getDaemonId() {
		return daemonId;
	}

	@Override
	public String toString() {
		return daemonId.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((daemonId == null) ? 0 : daemonId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DaemonAddress other = (DaemonAddress) obj;
		if (daemonId == null) {
			if (other.daemonId != null)
				return false;
		} else if (!daemonId.equals(other.daemonId))
			return false;
		return true;
	}
	
	@Override
	public int compareTo(DaemonAddress that) {
		if (this.daemonId < that.daemonId)
			return -1;
		if (this.daemonId > that.daemonId)
			return 1;
		if (this.peerAddress.getPort() < that.peerAddress.getPort())
			return -1;
		if (this.peerAddress.getPort() > that.peerAddress.getPort())
			return 1;
		if (this.peerAddress.getId() < that.peerAddress.getId())
			return -1;
		if (this.peerAddress.getId() > that.peerAddress.getId())
			return 1;
		ByteBuffer thisIpBytes = ByteBuffer.wrap(this.peerAddress.getIp().getAddress()).order(
				ByteOrder.BIG_ENDIAN);
		ByteBuffer thatIpBytes = ByteBuffer.wrap(that.peerAddress.getIp().getAddress()).order(
				ByteOrder.BIG_ENDIAN);
		return thisIpBytes.compareTo(thatIpBytes);
	}

}
