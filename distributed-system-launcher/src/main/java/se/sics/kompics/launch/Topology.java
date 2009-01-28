/**
 * This file is part of the Kompics component model runtime.
 * 
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.kompics.launch;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import se.sics.kompics.address.Address;

/**
 * The <code>Topology</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id$
 */
public abstract class Topology implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7257077939867770981L;

	private final class Node implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 6802497842084233233L;
		private final int name;
		private final InetAddress ip;
		private final int port;

		public Node(int name, InetAddress ip, int port) {
			super();
			this.name = name;
			this.ip = ip;
			this.port = port;
		}

		public final int getName() {
			return name;
		}

		public final InetAddress getIp() {
			return ip;
		}

		public final int getPort() {
			return port;
		}
	}

	/**
	 * The <code>Link</code> class.
	 * 
	 * @author Cosmin Arad <cosmin@sics.se>
	 * @author Jim Dowling <jdowling@sics.se>
	 * @version $Id$
	 */
	protected final class Link implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 3294108809970102162L;
		final Node source, destination;
		final long latencyMs;
		final double lossRate;

		/**
		 * Instantiates a new link.
		 * 
		 * @param source
		 *            the source
		 * @param destination
		 *            the destination
		 * @param latencyMs
		 *            the latency ms
		 * @param lossRate
		 *            the loss rate
		 */
		public Link(Node source, Node destination, long latencyMs,
				double lossRate) {
			super();
			this.source = source;
			this.destination = destination;
			this.latencyMs = latencyMs;
			this.lossRate = lossRate;
		}

		/**
		 * Bidirectional.
		 */
		public final void bidirectional() {
			link(destination.name, source.name, latencyMs, lossRate);
		}
	}

	private final HashMap<Integer, Node> nodes = new HashMap<Integer, Node>();
	private final HashMap<Address, Node> nodesByAddress = new HashMap<Address, Node>();
	private final HashMap<Node, HashMap<Node, Link>> links = new HashMap<Node, HashMap<Node, Link>>();
	private final HashMap<Integer, Address> addresses = new HashMap<Integer, Address>();
	private boolean nodesDone = false;
	private int self;

	/**
	 * Node.
	 * 
	 * @param name
	 *            the name
	 * @param ip
	 *            the ip
	 * @param port
	 *            the port
	 */
	protected final void node(int name, String ip, int port) {
		if (nodesDone)
			throw new RuntimeException("Define all nodes before defining links");

		try {
			Node node = new Node(name, InetAddress.getByName(ip), port);
			Node old = nodes.put(name, node);
			if (old != null)
				throw new RuntimeException("Node " + name
						+ " defined more than once");
		} catch (UnknownHostException e) {
			throw new RuntimeException("Bad node IP or hostname", e);
		}
	}

	/**
	 * Link.
	 * 
	 * @param source
	 *            the source
	 * @param destination
	 *            the destination
	 * @param latencyMs
	 *            the latency ms
	 * @param lossRate
	 *            the loss rate
	 * 
	 * @return the link
	 */
	protected final Link link(int source, int destination, long latencyMs,
			double lossRate) {
		if (!nodesDone) {
			nodesDone = true;
			checkNodes();
			fillAddresses();
		}

		Node src = nodes.get(source);
		Node dst = nodes.get(destination);

		if (src == null) {
			throw new RuntimeException("Source node " + source + " undefined");
		}
		if (dst == null) {
			throw new RuntimeException("Destination node " + destination
					+ " undefined");
		}
		if (latencyMs < 0) {
			throw new RuntimeException("Negative latency for link " + source
					+ " -> " + destination);
		}
		if (lossRate < 0.0 || lossRate > 1.0) {
			throw new RuntimeException("Loss rate for link " + source + " -> "
					+ destination + " is not in [0, 1]");
		}
		if (source == destination) {
			throw new RuntimeException("Cannot define link " + source + " -> "
					+ destination);
		}

		Link link = new Link(src, dst, latencyMs, lossRate);

		HashMap<Node, Link> srcLinks = links.get(src);
		if (srcLinks == null) {
			srcLinks = new HashMap<Node, Link>();
			links.put(src, srcLinks);
		}
		Link old = srcLinks.put(dst, link);
		if (old != null)
			throw new RuntimeException("Link " + source + " -> " + destination
					+ " defined more than once");
		return link;
	}

	/**
	 * Default links.
	 * 
	 * @param latencyMs
	 *            the latency ms
	 * @param lossRate
	 *            the loss rate
	 */
	protected final void defaultLinks(long latencyMs, double lossRate) {
		for (Node node : nodes.values()) {
			if (links.containsKey(node)) {
				// node is a source, fill links to remaining destinations
				if (!links.get(node).keySet().containsAll(nodes.values())) {
					Set<Node> other = new HashSet<Node>(nodes.values());
					other.removeAll(links.get(node).keySet());
					other.remove(node);
					for (Node dest : other) {
						link(node.name, dest.name, latencyMs, lossRate);
					}
				}
			} else {
				// node is not a source of any link. link it to all other nodes
				HashSet<Node> dests = new HashSet<Node>(nodes.values());
				dests.remove(node);
				for (Node dest : dests) {
					link(node.name, dest.name, latencyMs, lossRate);
				}
			}
		}
	}

	/**
	 * Self.
	 * 
	 * @param self
	 *            the self
	 */
	protected final void self(int self) {
		if (!nodesDone) {
			nodesDone = true;
			checkNodes();
			fillAddresses();
		}
		if (!nodes.containsKey(self)) {
			throw new RuntimeException("Node " + self + " undefined");
		}
		this.self = self;
	}

	private final void fillAddresses() {
		for (Node node : nodes.values()) {
			Address address = new Address(node.ip, node.port, node.name);
			addresses.put(node.name, address);
			nodesByAddress.put(address, node);
		}
	}

	private final void checkNodes() {
		Collection<Node> nodeSet = nodes.values();
		HashMap<InetAddress, Set<Integer>> map = new HashMap<InetAddress, Set<Integer>>();

		for (Node node : nodeSet) {
			Set<Integer> ports = map.get(node.ip);
			if (ports == null) {
				ports = new HashSet<Integer>();
				map.put(node.ip, ports);
			}

			if (ports.contains(node.port)) {
				throw new RuntimeException("More than one node for address "
						+ node.ip.getCanonicalHostName() + ":" + node.port);
			}

			ports.add(node.port);
		}

		map = null;
	}

	/**
	 * Gets the latency ms.
	 * 
	 * @param source
	 *            the source
	 * @param destination
	 *            the destination
	 * 
	 * @return the latency ms
	 * 
	 * @throws NoLinkException
	 *             the no link exception
	 */
	public final long getLatencyMs(int source, int destination)
			throws NoLinkException {
		Node src = nodes.get(source);
		Node dst = nodes.get(destination);
		if (src == null) {
			throw new RuntimeException("Node " + source + " undefined");
		}
		if (dst == null) {
			throw new RuntimeException("Node " + destination + " undefined");
		}
		HashMap<Node, Link> dests = links.get(src);
		if (dests == null) {
			throw new NoLinkException(source, destination);
		}
		Link link = dests.get(dst);
		if (link == null) {
			throw new NoLinkException(source, destination);
		}
		return link.latencyMs;
	}

	/**
	 * Gets the loss rate.
	 * 
	 * @param source
	 *            the source
	 * @param destination
	 *            the destination
	 * 
	 * @return the loss rate
	 * 
	 * @throws NoLinkException
	 *             the no link exception
	 */
	public final double getLossRate(int source, int destination)
			throws NoLinkException {
		Node src = nodes.get(source);
		Node dst = nodes.get(destination);
		if (src == null) {
			throw new RuntimeException("Node " + source + " undefined");
		}
		if (dst == null) {
			throw new RuntimeException("Node " + destination + " undefined");
		}
		HashMap<Node, Link> dests = links.get(src);
		if (dests == null) {
			throw new NoLinkException(source, destination);
		}
		Link link = dests.get(dst);
		if (link == null) {
			throw new NoLinkException(source, destination);
		}
		return link.lossRate;
	}

	/**
	 * Gets the latency ms.
	 * 
	 * @param source
	 *            the source
	 * @param destination
	 *            the destination
	 * 
	 * @return the latency ms
	 * 
	 * @throws NoLinkException
	 *             the no link exception
	 */
	public final long getLatencyMs(Address source, Address destination)
			throws NoLinkException {
		Node src = nodesByAddress.get(source);
		Node dst = nodesByAddress.get(destination);
		if (src == null) {
			throw new RuntimeException("Node " + source + " undefined");
		}
		if (dst == null) {
			throw new RuntimeException("Node " + destination + " undefined");
		}
		HashMap<Node, Link> dests = links.get(src);
		if (dests == null) {
			throw new NoLinkException(src.name, dst.name);
		}
		Link link = dests.get(dst);
		if (link == null) {
			throw new NoLinkException(src.name, dst.name);
		}
		return link.latencyMs;
	}

	/**
	 * Gets the loss rate.
	 * 
	 * @param source
	 *            the source
	 * @param destination
	 *            the destination
	 * 
	 * @return the loss rate
	 * 
	 * @throws NoLinkException
	 *             the no link exception
	 */
	public final double getLossRate(Address source, Address destination)
			throws NoLinkException {
		Node src = nodesByAddress.get(source);
		Node dst = nodesByAddress.get(destination);
		if (src == null) {
			throw new RuntimeException("Node " + source + " undefined");
		}
		if (dst == null) {
			throw new RuntimeException("Node " + destination + " undefined");
		}
		HashMap<Node, Link> dests = links.get(src);
		if (dests == null) {
			throw new NoLinkException(src.name, dst.name);
		}
		Link link = dests.get(dst);
		if (link == null) {
			throw new NoLinkException(src.name, dst.name);
		}
		return link.lossRate;
	}

	/**
	 * Gets the address.
	 * 
	 * @param node
	 *            the node
	 * 
	 * @return the address
	 */
	public final Address getAddress(int node) {
		return addresses.get(node);
	}

	/**
	 * Gets the all addresses.
	 * 
	 * @return the all addresses
	 */
	public final Set<Address> getAllAddresses() {
		return new HashSet<Address>(addresses.values());
	}

	/**
	 * Gets the self address.
	 * 
	 * @return the self address
	 */
	public final Address getSelfAddress() {
		return addresses.get(self);
	}

	/**
	 * Gets the neighbors.
	 * 
	 * @param node
	 *            the node
	 * 
	 * @return the neighbors
	 */
	public final Set<Address> getNeighbors(int node) {
		Node src = nodes.get(node);
		HashSet<Address> neighbors = new HashSet<Address>();
		if (links.get(src) != null) {
			for (Node n : links.get(src).keySet()) {
				neighbors.add(addresses.get(n.name));
			}
		}
		return neighbors;
	}

	/**
	 * Gets the neighbors.
	 * 
	 * @param node
	 *            the node
	 * 
	 * @return the neighbors
	 */
	public final Set<Address> getNeighbors(Address node) {
		Node src = nodesByAddress.get(node);
		HashSet<Address> neighbors = new HashSet<Address>();
		if (links.get(src) != null) {
			for (Node n : links.get(src).keySet()) {
				neighbors.add(addresses.get(n.name));
			}
		}
		return neighbors;
	}

	/**
	 * Load.
	 * 
	 * @param topologyFile
	 *            the topology file
	 * @param self
	 *            the self
	 * 
	 * @return the topology
	 */
	public static Topology load(String topologyFile, int self) {
		Topology topology = null;

		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
					topologyFile));
			topology = (Topology) ois.readObject();
			topology.self(self);
			ois.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return topology;
	}

	/**
	 * Gets the node count.
	 * 
	 * @return the node count
	 */
	public int getNodeCount() {
		return nodes.size();
	}

	/**
	 * Check fully connected.
	 */
	public void checkFullyConnected() {
		String message = ". Add link() or defaultLinks() to fill in the missing links.";
		if (!links.keySet().containsAll(nodes.values())) {
			String warning = "Warning: topology is not fully connected: source ";
			HashSet<Node> srcs = new HashSet<Node>(nodes.values());
			srcs.removeAll(links.keySet());
			for (Node node : srcs) {
				warning += node.name + " ";
			}
			throw new RuntimeException(warning
					+ "not connected to any destination" + message);
		}
		for (Node src : links.keySet()) {
			HashSet<Node> dests = new HashSet<Node>(nodes.values());
			dests.remove(src);
			if (!links.get(src).keySet().containsAll(dests)) {
				throw new RuntimeException(
						"Warning: topology is not fully connected" + message);
			}
		}
	}
}
