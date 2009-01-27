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
import se.sics.kompics.api.EventHandler;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.api.annotation.MayTriggerEventTypes;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.events.Message;
import se.sics.kompics.p2p.chord.events.GetChordNeighborsResponse;
import se.sics.kompics.p2p.chord.router.FingerTableView;
import se.sics.kompics.p2p.monitor.events.PeerViewNotification;
import se.sics.kompics.p2p.monitor.events.ViewEvictPeer;
import se.sics.kompics.timer.TimerHandler;
import se.sics.kompics.timer.events.ScheduleTimeout;
import se.sics.kompics.timer.events.Timeout;
import se.sics.kompics.web.events.WebRequest;
import se.sics.kompics.web.events.WebResponse;

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

	private Channel timerSignalChannel;

	// private Channel lnSendChannel;

	// private long updatePeriod;

	private final HashMap<Address, ViewEntry> view;

	private HashMap<Address, Map<String, Object>> p2pNetworkData;

	private HashMap<Address, Address> successor, predecessor;

	private TreeSet<Address> alivePeers;

	private HashMap<Address, ViewEntry> deadPeers;

	private TimerHandler timerHandler;

	private long evictAfter;

	public PeerMonitorServer(Component component) {
		super();
		this.component = component;
		this.view = new HashMap<Address, ViewEntry>();
		this.p2pNetworkData = new HashMap<Address, Map<String, Object>>();
		this.successor = new HashMap<Address, Address>();
		this.predecessor = new HashMap<Address, Address>();
		this.alivePeers = new TreeSet<Address>();
		this.deadPeers = new HashMap<Address, ViewEntry>();
	}

	@ComponentCreateMethod
	public void create() {
		// use shared timer component
		ComponentMembrane timerMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.Timer");
		Channel timerSetChannel = timerMembrane
				.getChannelIn(ScheduleTimeout.class);
		timerSignalChannel = timerMembrane.getChannelOut(Timeout.class);

		// use shared LossyNetwork component
		ComponentMembrane lnMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.p2p.network.LossyNetwork");
		// lnSendChannel = lnMembrane.getChannel(LossyNetworkSendEvent.class);
		Channel lnDeliverChannel = lnMembrane.getChannelOut(Message.class);

		// use the shared WebComponent
		ComponentMembrane webMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.Web");
		webRequestChannel = webMembrane.getChannelOut(WebRequest.class);
		webResponseChannel = webMembrane.getChannelIn(WebResponse.class);

		component.subscribe(webRequestChannel, handleWebRequest);

		component.subscribe(lnDeliverChannel, handlePeerNotification);

		this.timerHandler = new TimerHandler(component, timerSetChannel);
		component.subscribe(timerSignalChannel, handleViewEvictPeer);
	}

	@ComponentInitializeMethod()
	public void init(long updatePeriod, long evictAfterSeconds) {
		// this.updatePeriod = updatePeriod;
		evictAfter = 1000 * evictAfterSeconds;

		logger.debug("INIT");
	}

	private EventHandler<PeerViewNotification> handlePeerNotification = new EventHandler<PeerViewNotification>() {
		public void handle(PeerViewNotification event) {
			Address peerAddress = event.getPeerAddress();
			Map<String, Object> peerData = event.getPeerData();

			p2pNetworkData.put(peerAddress, peerData);

			addPeerToView(peerAddress);

			Address pred = alivePeers.lower(peerAddress);
			if (pred != null) {
				predecessor.put(peerAddress, pred);
			} else {
				predecessor.put(peerAddress, alivePeers.last());
			}

			Address succ = alivePeers.higher(peerAddress);
			if (succ != null) {
				successor.put(peerAddress, succ);
			} else {
				successor.put(peerAddress, alivePeers.first());
			}

			logger.debug("Got notification from peer {}", peerAddress);
		}
	};

	private EventHandler<ViewEvictPeer> handleViewEvictPeer = new EventHandler<ViewEvictPeer>() {
		public void handle(ViewEvictPeer event) {
			// only evict if it was not refreshed in the meantime
			// which means the timer is not anymore outstanding
			if (timerHandler.isOustandingTimer(event.getTimerId())) {
				removePeerFromView(event.getPeerAddress());
			}
		}
	};

	@MayTriggerEventTypes(WebResponse.class)
	private EventHandler<WebRequest> handleWebRequest = new EventHandler<WebRequest>() {
		public void handle(WebRequest event) {
			logger.debug("Handling WebRequest");

			WebResponse response = new WebResponse(dumpViewToHtml(), event, 1,
					1);
			component.triggerEvent(response, webResponseChannel);
		}
	};

	private void addPeerToView(Address address) {
		if (address != null) {

			long now = System.currentTimeMillis();

			alivePeers.add(address);
			deadPeers.remove(address);

			ViewEntry entry = view.get(address);
			if (entry == null) {
				entry = new ViewEntry(address, now, now);
				view.put(address, entry);

				// set eviction timer
				long evictionTimerId = timerHandler.setTimer(new ViewEvictPeer(
						address), timerSignalChannel, evictAfter);
				entry.setEvictionTimerId(evictionTimerId);

				logger.debug("Added peer {}", address);
			} else {
				entry.setRefreshedAt(now);

				// reset eviction timer
				timerHandler.cancelTimer(entry.getEvictionTimerId());

				long evictionTimerId = timerHandler.setTimer(new ViewEvictPeer(
						address), timerSignalChannel, evictAfter);
				entry.setEvictionTimerId(evictionTimerId);

				logger.debug("Refreshed peer {}", address);
			}
		}
	}

	private void removePeerFromView(Address address) {
		if (address != null) {
			ViewEntry oldViewEntry = view.remove(address);

			alivePeers.remove(address);

			deadPeers.put(address, oldViewEntry);
			logger.debug("Removed peer {}", address);
		}
	}

	private String dumpViewToHtml() {
		StringBuilder sb = new StringBuilder("<!DOCTYPE html PUBLIC ");
		sb.append("\"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3");
		sb.append(".org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=");
		sb.append("\"http://www.w3.org/1999/xhtml\"><head><meta http-equiv=\"");
		sb.append("Content-Type\" content=\"text/html; charset=utf-8\" />");

		// page refresh
		// sb.append("<meta http-equiv=\"refresh\" content=\"10\">");
		sb.append("<title>Kompics P2P Monitor Server</title>");
		sb.append("<style type=\"text/css\"><!--.style2 {font-family: ");
		sb.append("Arial, Helvetica, sans-serif; color: #0099FF;}--></style>");
		sb.append("</head><body><h1 align=\"center\" class=\"style2\">");
		sb.append("Kompics P2P Monitor</h1>");
		sb.append("<h2 align=\"center\" class=\"style2\">");
		sb.append("View of Chord SON:</h2>");
		sb.append("<table width=\"1300\" border=\"1\" align=\"center\"><tr>");
		sb.append("<th class=\"style2\" width=\"50\" scope=\"col\">Count</th>");
		sb
				.append("<th class=\"style2\" width=\"100\" scope=\"col\">Predecessor</th>");
		sb
				.append("<th class=\"style2\" width=\"100\" scope=\"col\">Peer Id</th>");
		sb
				.append("<th class=\"style2\" width=\"100\" scope=\"col\">Successor</th>");
		sb
				.append("<th class=\"style2\" width=\"350\" scope=\"col\">Successor List</th>");
		sb
				.append("<th class=\"style2\" width=\"400\" scope=\"col\">Finger List</th>");
		sb.append("<th class=\"style2\" width=\"100\" scope=\"col\">Age</th>");
		sb
				.append("<th class=\"style2\" width=\"100\" scope=\"col\">Freshness</th></tr>");

		// LinkedList<Address> peers = new LinkedList<Address>(p2pNetworkData
		// .keySet());
		// Collections.sort(peers);

		// get all peers in their id order
		Iterator<Address> iterator = alivePeers.iterator();

		int count = 1;

		while (iterator.hasNext()) {
			Address address = iterator.next();
			Map<String, Object> peerData = p2pNetworkData.get(address);
			GetChordNeighborsResponse sonData = (GetChordNeighborsResponse) peerData
					.get("ChordRing");

			Address pred = sonData.getPredecessorPeer();
			Address succ = sonData.getSuccessorPeer();
			Address realPred = predecessor.get(address);
			Address realSucc = successor.get(address);
			List<Address> succList = sonData.getSuccessorList();
			FingerTableView fingerTableView = sonData.getFingerTable();

			sb.append("<tr>");
			sb.append("<td><div align=\"center\">").append(count++);
			sb.append("</div></td>");

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
			if (fingerTableView != null) {
				sb.append("<td><div align=\"left\">");
				sb.append("[");
				boolean first = true;
				for (int i = 0; i < fingerTableView.finger.length; i++) {
					if (fingerTableView.finger[i] != null) {
						if (!first) {
							sb.append(", ");
						}
						appendWebLink(sb, fingerTableView.finger[i]);
						first = false;
					}
				}
				sb.append("]");
			} else {
				sb.append("<td bgcolor=\"#FFCCFF\"><div align=\"left\">");
				sb.append("[empty]");
			}
			sb.append("</div></td>");

			long now = System.currentTimeMillis();
			ViewEntry viewEntry = view.get(address);

			// print age
			sb.append("<td><div align=\"right\">");
			sb.append(durationToString(now - viewEntry.getAddedAt()));
			sb.append("</div></td>");

			// print freshness
			sb.append("<td><div align=\"right\">");
			sb.append(durationToString(now - viewEntry.getRefreshedAt()));
			sb.append("</div></td>");

			sb.append("</tr>");
		}
		sb.append("</table>");

		// print dead peers
		if (deadPeers.size() > 0) {
			sb.append("<h2 align=\"center\" class=\"style2\">Dead peers:</h2>");
			sb
					.append("<table width=\"1300\" border=\"0\" align=\"center\"><tr>");
			sb
					.append("<th class=\"style2\" width=\"50\" scope=\"col\">Count</th>");
			sb
					.append("<th class=\"style2\" width=\"100\" scope=\"col\">Predecessor</th>");
			sb
					.append("<th class=\"style2\" width=\"100\" scope=\"col\">Peer Id</th>");
			sb
					.append("<th class=\"style2\" width=\"100\" scope=\"col\">Successor</th>");
			sb
					.append("<th class=\"style2\" width=\"350\" scope=\"col\">Successor List</th>");
			sb
					.append("<th class=\"style2\" width=\"400\" scope=\"col\">Finger List</th>");
			sb
					.append("<th class=\"style2\" width=\"100\" scope=\"col\">Lifetime</th>");
			sb
					.append("<th class=\"style2\" width=\"100\" scope=\"col\">Dead for</th></tr>");

			LinkedList<Address> peers = new LinkedList<Address>(deadPeers
					.keySet());
			Collections.sort(peers);

			count = 1;

			// get all peers in their id order
			iterator = peers.iterator();
			while (iterator.hasNext()) {
				Address address = iterator.next();
				Map<String, Object> peerData = p2pNetworkData.get(address);
				GetChordNeighborsResponse sonData = (GetChordNeighborsResponse) peerData
						.get("ChordRing");

				Address pred = sonData.getPredecessorPeer();
				Address succ = sonData.getSuccessorPeer();
				Address realPred = predecessor.get(address);
				Address realSucc = successor.get(address);
				List<Address> succList = sonData.getSuccessorList();
				FingerTableView fingerTableView = sonData.getFingerTable();

				sb.append("<tr>");

				sb.append("<tr>");
				sb.append("<td><div align=\"center\">").append(count++);
				sb.append("</div></td>");

				// print predecessor
				if (pred != null) {
					if (pred.equals(realPred)) {
						sb
								.append("<td bgcolor=\"#99FF99\"><div align=\"center\">");
						appendWebLink(sb, pred);
					} else {
						sb
								.append("<td bgcolor=\"#FFFF66\"><div align=\"center\">");
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
						sb
								.append("<td bgcolor=\"#99FF99\"><div align=\"center\">");
						appendWebLink(sb, succ);
					} else {
						sb
								.append("<td bgcolor=\"#FFFF66\"><div align=\"center\">");
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

				// print fingers
				if (fingerTableView != null) {
					sb.append("<td><div align=\"left\">");
					sb.append("[");
					boolean first = true;
					for (int i = 0; i < fingerTableView.finger.length; i++) {
						if (fingerTableView.finger[i] != null) {
							if (!first) {
								sb.append(", ");
							}
							appendWebLink(sb, fingerTableView.finger[i]);
							first = false;
						}
					}
					sb.append("]");
				} else {
					sb.append("<td bgcolor=\"#FFCCFF\"><div align=\"left\">");
					sb.append("[empty]");
				}
				sb.append("</div></td>");

				long now = System.currentTimeMillis();
				ViewEntry viewEntry = deadPeers.get(address);

				// print lifetime
				sb.append("<td><div align=\"right\">");
				sb.append(durationToString(viewEntry.getRefreshedAt()
						- viewEntry.getAddedAt()));
				sb.append("</div></td>");

				// print dead for
				sb.append("<td><div align=\"right\">");
				sb.append(durationToString(now - viewEntry.getRefreshedAt()));
				sb.append("</div></td>");

				sb.append("</tr>");
			}
			sb.append("</table>");
		}

		sb.append("</body></html>");
		return sb.toString();
	}

	private void appendWebLink(StringBuilder sb, Address address, String label) {
		if (address == null) {
			sb.append("NIL");
			return;
		}
		sb.append("<a href=\"http://").append(address.getIp()).append(":");
		if (label == null) {
			sb.append(address.getPort() - 21920).append("/");
			sb.append(address.getId()).append("/inf\">");
			sb.append(address.getId());
		} else {
			sb.append(address.getPort()).append("/");
			sb.append(address.getId()).append("/inf\">");
			sb.append(label);
		}
		sb.append("</a>");
	}

	private void appendWebLink(StringBuilder sb, Address address) {
		appendWebLink(sb, address, null);
	}

	private String getWebAddress(Address address) {
		return address.getIp().toString() + ":" + (address.getPort() - 21920)
				+ "/" + address.getId() + "/inf";
	}

	private String durationToString(long duration) {
		StringBuilder sb = new StringBuilder();

		// get duration in seconds
		duration /= 1000;

		int s = 0, m = 0, h = 0, d = 0, y = 0;
		s = (int) (duration % 60);
		// get duration in minutes
		duration /= 60;
		if (duration > 0) {
			m = (int) (duration % 60);
			// get duration in hours
			duration /= 60;
			if (duration > 0) {
				h = (int) (duration % 24);
				// get duration in days
				duration /= 24;
				if (duration > 0) {
					d = (int) (duration % 365);
					// get duration in years
					y = (int) (duration / 365);
				}
			}
		}

		boolean printed = false;

		if (y > 0) {
			sb.append(y).append("y");
			printed = true;
		}
		if (d > 0) {
			sb.append(d).append("d");
			printed = true;
		}
		if (h > 0) {
			sb.append(h).append("h");
			printed = true;
		}
		if (m > 0) {
			sb.append(m).append("m");
			printed = true;
		}
		if (s > 0 || printed == false) {
			sb.append(s).append("s");
		}

		return sb.toString();
	}
}
