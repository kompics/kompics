package se.sics.kompics.p2p.network.topology;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import se.sics.kompics.network.Address;

public class NeighbourLinks {

	private int nodesCount;

	private HashMap<Integer, Address> nodesList;

	private HashMap<Integer, LinkDescriptor> linksMap;

	private Address localAddress;

	public NeighbourLinks(int nodesCount) {
		this.nodesCount = nodesCount;
		this.linksMap = new HashMap<Integer, LinkDescriptor>();
		this.nodesList = new HashMap<Integer, Address>();
	}

	public int getNodesCount() {
		return nodesCount;
	}

	public void setLocalAddress(Address ref) {
		this.localAddress = ref;
		addLink(ref, ref, 0, 0.0);
	}

	public Address addNode(String nodeId, String Ip, int port)
			throws UnknownHostException {

		InetAddress address = InetAddress.getByName(Ip);

		BigInteger bIntNodeId = new BigInteger(nodeId);

		Address nodeReference = new Address(address, port, bIntNodeId);

		nodesList.put(bIntNodeId.intValue(), nodeReference);

		return nodeReference;
	}

	public LinkDescriptor addLink(Address sourceNodeId, Address destNodeId,
			long latency, double loss_rate) {

		LinkDescriptor linkDescriptor = new LinkDescriptor(sourceNodeId,
				destNodeId, latency, loss_rate);

		linksMap.put(destNodeId.getId().intValue(), linkDescriptor);

		return linkDescriptor;
	}

	public Collection<LinkDescriptor> getAllLinks() {
		return linksMap.values();
	}

	public LinkDescriptor getLink(BigInteger nodeId) {
		return linksMap.get(nodeId.intValue());
	}

	public Address getNode(BigInteger nodeId) {
		return nodesList.get(nodeId.intValue());
	}

	/**
	 * Gets all the node references of the nodes specified in this Topology
	 * Descriptor.
	 * 
	 * @return a {@link Collection} containing all the {@link NodeReference}
	 *         instances
	 */
	public Collection<Address> getAllNodes() {
		return nodesList.values();
	}

	/**
	 * Gets all the node references of the nodes specified in this Topology
	 * Descriptor, except the one of the current node.
	 * 
	 * @return a {@link Collection} containing all the {@link NodeReference}
	 *         instances
	 */
	public Collection<Address> getAllOtherNodes() {
		Collection<Address> nodes = new HashSet<Address>(nodesList.values());
		nodes.remove(localAddress);
		return nodes;
	}

	public boolean isNodePresent(int nodeId) {
		return nodesList.containsKey(nodeId);
	}

	public Address getLocalAddress() {
		return localAddress;
	}
}
