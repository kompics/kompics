package se.sics.kompics.address;

import java.io.Serializable;
import java.net.InetAddress;

public final class Address implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7330046056166039991L;

	private final InetAddress ip;

	private final int port;

	private final int id;

	public Address(InetAddress ip, int port, int id) {
		this.ip = ip;
		this.port = port;
		this.id = id;
	}

	public final InetAddress getIp() {
		return ip;
	}

	public final int getPort() {
		return port;
	}

	public final int getId() {
		return id;
	}

	@Override
	public final String toString() {
		return "" + id + ip + ":" + port;
	}

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((ip == null) ? 0 : ip.hashCode());
		result = prime * result + port;
		return result;
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Address other = (Address) obj;
		if (id != other.id)
			return false;
		if (ip == null) {
			if (other.ip != null)
				return false;
		} else if (!ip.equals(other.ip))
			return false;
		if (port != other.port)
			return false;
		return true;
	}
}
