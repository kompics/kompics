package se.sics.kompics.network;

import java.io.Serializable;
import java.math.BigInteger;
import java.net.InetAddress;

public class Address implements Comparable<Address>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 226803467944970895L;

	private static Address localAddress;

	public static void setLocalAddress(Address address) {
		localAddress = address;
	}

	public static Address getLocalAddress() {
		return localAddress;
	}

	private InetAddress ip;

	private int port;

	private BigInteger id;

	public Address(InetAddress ip, int port, BigInteger id) {
		super();
		this.ip = ip;
		this.port = port;
		this.id = id;
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
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((ip == null) ? 0 : ip.hashCode());
		result = prime * result + port;
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
		final Address other = (Address) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
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

	@Override
	public String toString() {
		return ip.getHostAddress() + ":" + port + "/" + id.toString();
	}
}
