package se.sics.kompics.p2p.monitor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.ComponentMembrane;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.api.annotation.EventHandlerMethod;
import se.sics.kompics.api.annotation.MayTriggerEventTypes;
import se.sics.kompics.network.Address;
import se.sics.kompics.p2p.monitor.events.PeerViewNotification;
import se.sics.kompics.p2p.network.events.LossyNetworkDeliverEvent;
import se.sics.kompics.p2p.son.ps.events.GetRingNeighborsResponse;
import se.sics.kompics.web.events.WebRequestEvent;
import se.sics.kompics.web.events.WebResponseEvent;

/**
 * The <code>PeerMonitorServer</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public class PeerMonitorServer {

	private static final Logger logger = LoggerFactory
			.getLogger(PeerMonitorServer.class);

	private final Component component;

	private Channel webRequestChannel, webResponseChannel;

	// private Channel lnSendChannel;
	//
	// private long updatePeriod;

	private HashMap<Address, Map<String, Object>> p2pNetworkData;

	private HashMap<Address, Address> successor, predecessor;

	private TreeSet<Address> knownPeers;

	public PeerMonitorServer(Component component) {
		super();
		this.component = component;
		this.p2pNetworkData = new HashMap<Address, Map<String, Object>>();
		this.successor = new HashMap<Address, Address>();
		this.predecessor = new HashMap<Address, Address>();
		this.knownPeers = new TreeSet<Address>();
	}

	@ComponentCreateMethod
	public void create() {
		// use shared LossyNetwork component
		ComponentMembrane lnMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.p2p.network.LossyNetwork");
		// lnSendChannel = lnMembrane.getChannel(LossyNetworkSendEvent.class);
		Channel lnDeliverChannel = lnMembrane
				.getChannel(LossyNetworkDeliverEvent.class);

		// use the shared WebComponent
		ComponentMembrane webMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.Web");
		webRequestChannel = webMembrane.getChannel(WebRequestEvent.class);
		webResponseChannel = webMembrane.getChannel(WebResponseEvent.class);

		component.subscribe(webRequestChannel, "handleWebRequest");

		component.subscribe(lnDeliverChannel, "handlePeerNotification");
	}

	@ComponentInitializeMethod()
	public void init(long updatePeriod) {
		// this.updatePeriod = updatePeriod;
	}

	@EventHandlerMethod
	public void handlePeerNotification(PeerViewNotification event) {
		Address peerAddress = event.getPeerAddress();
		Map<String, Object> peerData = event.getPeerData();

		p2pNetworkData.put(peerAddress, peerData);
		knownPeers.add(peerAddress);

		Address pred = knownPeers.lower(peerAddress);
		if (pred != null) {
			predecessor.put(peerAddress, pred);
		} else {
			predecessor.put(peerAddress, knownPeers.last());
		}

		Address succ = knownPeers.higher(peerAddress);
		if (succ != null) {
			successor.put(peerAddress, succ);
		} else {
			successor.put(peerAddress, knownPeers.first());
		}

		logger.debug("Got notification from peer {}", peerAddress);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(WebResponseEvent.class)
	public void handleWebRequest(WebRequestEvent event) {
		logger.debug("Handling WebRequest");

		WebResponseEvent response = new WebResponseEvent(dumpViewToHtml(),
				event, 1, 1);
		component.triggerEvent(response, webResponseChannel);
	}

	private String dumpViewToHtml() {
		StringBuffer sb = new StringBuffer(
				"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN"
						+ "\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transiti"
						+ "onal.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtm"
						+ "l\"><head><meta http-equiv=\"Content-Type\" content="
						+ "\"text/html; charset=utf-8\" />"
						+ "<title>Kompics P2P Monitor Server</title>"
						+ "<style type=\"text/css\"><!--.style2 {font-family: "
						+ "Arial, Helvetica, sans-serif; color: #0099FF;}-->"
						+ "</style>"
						+ "</head><body><h1 align=\"center\" class=\"style2\">"
						+ "Kompics P2P Monitor</h1>"
						+ "<h2 align=\"center\" class=\"style2\">"
						+ "View of Chord SON:</h2>"
						+ "<table width=\"900\" border=\"2\" align=\"center\"><tr>"
						+ "<th class=\"style2\" width=\"100\" scope=\"col\">Predecessor</th>"
						+ "<th class=\"style2\" width=\"100\" scope=\"col\">Peer Id</th>"
						+ "<th class=\"style2\" width=\"100\" scope=\"col\">Successor</th>"
						+ "<th class=\"style2\" width=\"300\" scope=\"col\">Successor List</th>"
						+ "<th class=\"style2\" width=\"300\" scope=\"col\">Finger List</th></tr>");

		LinkedList<Address> peers = new LinkedList<Address>(p2pNetworkData
				.keySet());
		Collections.sort(peers);

		// get all peers in most recently added order
		Iterator<Address> iterator = peers.iterator();
		while (iterator.hasNext()) {
			Address address = iterator.next();
			Map<String, Object> peerData = p2pNetworkData.get(address);
			GetRingNeighborsResponse sonData = (GetRingNeighborsResponse) peerData
					.get("ChordRing");

			Address pred = sonData.getPredecessorPeer();
			Address succ = sonData.getSuccessorPeer();
			Address realPred = predecessor.get(address);
			Address realSucc = successor.get(address);
			List<Address> succList = sonData.getSuccessorList();
			List<Address> fingerList = null;

			sb.append("<tr>");

			// print predecessor
			if (pred != null) {
				if (pred.equals(realPred)) {
					sb.append("<td bgcolor=\"#99FF99\"><div align=\"center\">");
					appendWebLink(sb, pred);
				} else {
					sb.append("<td bgcolor=\"#FFFF66\"><div align=\"center\">");
					appendWebLink(sb, pred);
					sb.append(" (<b>");
					appendWebLink(sb, realPred);
					sb.append("</b>)");
				}
			} else {
				sb.append("<td bgcolor=\"#FFCCFF\"><div align=\"center\">");
				sb.append("NIL");
				sb.append(" (<b>");
				appendWebLink(sb, realPred);
				sb.append("</b>)");
			}

			// print peer address
			sb
					.append("</div></td><td bgcolor=\"#99CCFF\"><div align=\"center\">");
			sb.append("<a href=\"http://").append(getWebAddress(address));
			sb.append("\">").append(address.getId()).append("</a>");
			sb.append("</div></td>");

			// print successor
			if (succ != null) {
				if (succ.equals(realSucc)) {
					sb.append("<td bgcolor=\"#99FF99\"><div align=\"center\">");
					appendWebLink(sb, succ);
				} else {
					sb.append("<td bgcolor=\"#FFFF66\"><div align=\"center\">");
					appendWebLink(sb, succ);
					sb.append(" (<b>");
					appendWebLink(sb, realSucc);
					sb.append("</b>)");
				}
			} else {
				sb.append("<td bgcolor=\"#FFCCFF\"><div align=\"center\">");
				sb.append("NIL");
				sb.append(" (<b>");
				appendWebLink(sb, realSucc);
				sb.append("</b>)");
			}
			sb.append("</div></td>");

			// print successor list
			if (succList != null) {
				sb.append("<td><div align=\"left\">");
				sb.append("[");
				Iterator<Address> iter = succList.iterator();
				while (iter.hasNext()) {
					appendWebLink(sb, iter.next());
					if (iter.hasNext()) {
						sb.append(", ");
					}
				}
				sb.append("]");
			} else {
				sb.append("<td bgcolor=\"#FFCCFF\"><div align=\"left\">");
				sb.append("[empty]");
			}
			sb.append("</div></td>");

			// print finger list
			if (fingerList != null) {
				sb.append("<td><div align=\"left\">");
				sb.append("[");
				Iterator<Address> iter = fingerList.iterator();
				while (iter.hasNext()) {
					appendWebLink(sb, iter.next());
					if (iter.hasNext()) {
						sb.append(", ");
					}
				}
				sb.append("]");
			} else {
				sb.append("<td bgcolor=\"#FFCCFF\"><div align=\"left\">");
				sb.append("[empty]");
			}
			sb.append("</div></td>");
			sb.append("</tr>");
		}

		sb.append("</table></body></html>");
		return sb.toString();
	}

	private void appendWebLink(StringBuffer sb, Address address) {
		sb.append("<a href=\"http://").append(address.getIp()).append(":");
		sb.append(address.getPort() - 21920).append("/");
		sb.append(address.getId()).append("/inf\">").append(address.getId());
		sb.append("</a>");
	}

	private String getWebAddress(Address address) {
		return address.getIp().toString() + ":" + (address.getPort() - 21920)
				+ "/" + address.getId() + "/inf";
	}
}
