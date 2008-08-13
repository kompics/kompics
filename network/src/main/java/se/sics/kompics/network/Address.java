package se.sics.kompics.network;

import java.io.Serializable;
import java.math.BigInteger;
import java.net.InetAddress;

public class Address implements Comparable<Address>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6782338837235531468L;

	private final InetAddress ip;

	private final int port;

	private final BigInteger id;

	private final int hashCode;

	public Address(InetAddress ip, int port, BigInteger id) {
		this.ip = ip;
		this.port = port;
		this.id = id;
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((ip == null) ? 0 : ip.hashCode());
		result = prime * result + port;
		this.hashCode = result;
	}

	public InetAddress getIp() {
		return ip;
	}

	public int getPort() {
		return port;
	}

	public BigInteger getId() {
		return id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Address o) {
		return id.compareTo(o.getId());
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		Address other = (Address) obj;
		if (hashCode != other.hashCode)
			return false;
		if (!id.equals(other.id))
			return false;
		if (!ip.equals(other.ip))
			return false;
		if (port != other.port)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return ip.getHostAddress() + ":" + port + "/" + id.toString();
	}
}
