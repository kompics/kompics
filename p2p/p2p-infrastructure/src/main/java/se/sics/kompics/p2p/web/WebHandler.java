package se.sics.kompics.p2p.web;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mortbay.jetty.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.ComponentMembrane;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.EventHandler;
import se.sics.kompics.api.FastEventFilter;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.api.annotation.MayTriggerEventTypes;
import se.sics.kompics.network.Address;
import se.sics.kompics.p2p.chord.events.ChordLookupFailed;
import se.sics.kompics.p2p.chord.events.ChordLookupRequest;
import se.sics.kompics.p2p.chord.events.ChordLookupResponse;
import se.sics.kompics.p2p.chord.events.GetChordNeighborsRequest;
import se.sics.kompics.p2p.chord.events.GetChordNeighborsResponse;
import se.sics.kompics.p2p.chord.router.FingerTableView;
import se.sics.kompics.p2p.chord.router.LookupInfo;
import se.sics.kompics.p2p.fd.ProbedPeerData;
import se.sics.kompics.p2p.fd.events.StatusRequest;
import se.sics.kompics.p2p.fd.events.StatusResponse;
import se.sics.kompics.web.events.WebRequest;
import se.sics.kompics.web.events.WebResponse;

/**
 * The <code>WebHandler</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public class WebHandler {

	private Logger logger;

	private final Component component;

	private Channel webRequestChannel, webResponseChannel;

	// Chord channels
	private Channel chordRequestChannel, chordResponseChannel,
			chordLookupResponseChannel;

	// FailureDetector channels
	private Channel fdRequestChannel, fdResponseChannel;

	private Address localAddress;

	private int parts;

	private WebRequest requestEvent;

	private String htmlHeader, htmlFooter;

	private Address monitorWebAddress, bootstrapWebAddress;

	public WebHandler(Component component) {
		super();
		this.component = component;
	}

	@ComponentCreateMethod
	public void create() {
		// use the shared WebComponent
		ComponentMembrane webMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.Web");
		webRequestChannel = webMembrane.getChannelOut(WebRequest.class);
		webResponseChannel = webMembrane.getChannelIn(WebResponse.class);

		// use shared FailureDetector component
		ComponentMembrane fdMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.p2p.fd.FailureDetector");
		fdRequestChannel = fdMembrane.getChannelIn(StatusRequest.class);
		fdResponseChannel = component.createChannel(StatusResponse.class);

		// use shared ChordRing component
		ComponentMembrane crMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.p2p.chord.Chord");
		chordRequestChannel = crMembrane
				.getChannelIn(GetChordNeighborsRequest.class);
		chordResponseChannel = component
				.createChannel(GetChordNeighborsResponse.class);
		chordLookupResponseChannel = component.createChannel(
				ChordLookupResponse.class, ChordLookupFailed.class);

		component.subscribe(fdResponseChannel, handleStatusResponse);
		component.subscribe(chordResponseChannel,
				handleGetChordNeighborsResponse);

		component.subscribe(chordLookupResponseChannel,
				handleChordLookupResponse);
		component
				.subscribe(chordLookupResponseChannel, handleChordLookupFailed);
	}

	@ComponentInitializeMethod
	public void init(Address localAddress, Address monitorWebAddress,
			Address bootstrapWebAddress) {
		this.localAddress = localAddress;
		this.monitorWebAddress = monitorWebAddress;
		this.bootstrapWebAddress = bootstrapWebAddress;

		logger = LoggerFactory.getLogger(WebHandler.class.getName() + "@"
				+ localAddress.getId());

		htmlHeader = getHtmlHeader();
		htmlFooter = "</body></html>";

		component.subscribe(webRequestChannel, handleWebRequest,
				new FastEventFilter<WebRequest>("destination", localAddress
						.getId()) {
					public boolean filter(WebRequest request) {
						return value.equals(request.destination);
					}
				});

		logger.debug("Subscribed for WebRequestEvent with destination {}",
				localAddress.getId());
	}

	private EventHandler<WebRequest> handleWebRequest = new EventHandler<WebRequest>() {
		public void handle(WebRequest event) {
			String target = event.getTarget();
			Request request = event.getRequest();
			logger.debug("Handling request {}", target);

			if (target.substring(1).startsWith("nav")) {
				Component boot = component;
				while (boot.getSuperComponent() != null) {
					boot = boot.getSuperComponent();
				}

				WebResponse responseEvent = new WebResponse(
						navigateDF(boot, 0), event, 1, 1);
				component.triggerEvent(responseEvent, webResponseChannel);
			} else {
				requestEvent = event;

				GetChordNeighborsRequest ringRequest = new GetChordNeighborsRequest(
						chordResponseChannel);
				StatusRequest fdRequest = new StatusRequest(fdResponseChannel);

				parts = 5;

				String chordLookupKey = request.getParameter("chordLookup");

				if (chordLookupKey != null) {
					// do Chord lookup
					ChordLookupRequest lookupRequest = new ChordLookupRequest(
							new BigInteger(chordLookupKey),
							chordLookupResponseChannel, null);
					component.triggerEvent(lookupRequest, chordRequestChannel);
				} else {
					// just print lookup form
					WebResponse response = new WebResponse(dumpChordLookupForm(
							null, null), requestEvent, 2, parts);
					component.triggerEvent(response, webResponseChannel);
				}

				component.triggerEvent(ringRequest, chordRequestChannel);
				component.triggerEvent(fdRequest, fdRequestChannel);

				WebResponse responseEvent = new WebResponse(htmlHeader,
						requestEvent, 1, parts);
				component.triggerEvent(responseEvent, webResponseChannel);
				responseEvent = new WebResponse(htmlFooter, requestEvent,
						parts, parts);
				component.triggerEvent(responseEvent, webResponseChannel);
			}
		}
	};

	@MayTriggerEventTypes(WebResponse.class)
	private EventHandler<ChordLookupResponse> handleChordLookupResponse = new EventHandler<ChordLookupResponse>() {
		public void handle(ChordLookupResponse response) {
			WebResponse responseEvent = new WebResponse(dumpChordLookupForm(
					response.getKey(), response), requestEvent, 2, parts);
			component.triggerEvent(responseEvent, webResponseChannel);
		}
	};

	@MayTriggerEventTypes(WebResponse.class)
	private EventHandler<ChordLookupFailed> handleChordLookupFailed = new EventHandler<ChordLookupFailed>() {
		public void handle(ChordLookupFailed response) {
			WebResponse responseEvent = new WebResponse(dumpChordLookupForm(
					response.getKey(), response), requestEvent, 2, parts);
			component.triggerEvent(responseEvent, webResponseChannel);
		}
	};

	@MayTriggerEventTypes(WebResponse.class)
	private EventHandler<StatusResponse> handleStatusResponse = new EventHandler<StatusResponse>() {
		public void handle(StatusResponse response) {
			String html = dumpFdStatusToHtml(response);

			WebResponse responseEvent = new WebResponse(html, requestEvent, 4,
					parts);
			component.triggerEvent(responseEvent, webResponseChannel);
		}
	};

	@MayTriggerEventTypes(WebResponse.class)
	private EventHandler<GetChordNeighborsResponse> handleGetChordNeighborsResponse = new EventHandler<GetChordNeighborsResponse>() {
		public void handle(GetChordNeighborsResponse event) {
			String html = dumpRingViewToHtml(event);

			WebResponse responseEvent = new WebResponse(html, requestEvent, 3,
					parts);
			component.triggerEvent(responseEvent, webResponseChannel);
		}
	};

	private String dumpChordLookupForm(BigInteger key, Event event) {
		StringBuilder sb = new StringBuilder();

		sb.append("<h2 align=\"center\" class=\"style2\">Chord Lookup:</h2>");
		sb.append("<table width=\"800\" border=\"0\" align=\"center\"><tr><td");
		sb.append("><form method=\"get\" name=\"lkpFrm\" id=\"chordLkpFrm\">");
		sb.append("<fieldset><legend> Chord </legend><label>Lookup key ");
		sb.append("<input name=\"chordLookup\" type=\"text\" id=\"lookip\" ");
		sb.append("value=\"").append(key != null ? key : "1");
		sb.append("\" size=\"6\"/>");
		sb.append("</label><input type=\"submit\" value=\"Lookup\" />");
		if (key != null) {
			if (event instanceof ChordLookupResponse) {
				ChordLookupResponse response = (ChordLookupResponse) event;
				LookupInfo info = response.getLookupInfo();
				sb.append("<label> Peer ");
				appendWebLink(sb, response.getResponsible(), null);
				sb.append(" is responsible for key ").append(key).append(".");
				sb.append("<br>Lookup took ").append(info.getduration());
				sb.append(" milliseconds and traversed ");
				sb.append(info.getHopCount()).append(" hops [");
				for (Address hop : info.getHops()) {
					sb.append(" ");
					appendWebLink(sb, hop, null);
				}
				sb.append(" ].</label>");
			} else if (event instanceof ChordLookupFailed) {
				ChordLookupFailed failed = (ChordLookupFailed) event;
				sb.append("<label> Lookup for key ").append(key);
				sb.append(" failed because of peer ");
				sb.append(failed.getSuspectedPeer());
				sb.append(".</label>");
			}
		}
		sb.append("</fieldset></form></td></tr></table>");

		return sb.toString();
	}

	private String dumpFdStatusToHtml(StatusResponse response) {
		Map<Address, ProbedPeerData> probedPeers = response.getProbedPeers();
		StringBuilder sb = new StringBuilder();

		sb.append("<h2 align=\"center\" class=\"style2\">Failure Detector:");
		sb.append("</h2><table width=\"400\" border=\"2\" align=\"center\">");
		sb.append("<th class=\"style2\" width=\"150\" scope=\"col\">Peer</th>");
		sb
				.append("<th class=\"style2\" width=\"50\" scope=\"col\">RTT avg</th>");
		sb
				.append("<th class=\"style2\" width=\"50\" scope=\"col\">RTT var</th>");
		sb.append("<th class=\"style2\" width=\"50\" scope=\"col\">RTTO</th>");
		sb
				.append("<th class=\"style2\" width=\"50\" scope=\"col\">RTTO show</th>");
		sb
				.append("<th class=\"style2\" width=\"50\" scope=\"col\">RTTO min</th>");
		sb.append("</tr>");

		LinkedList<Address> peers = new LinkedList<Address>(probedPeers
				.keySet());

		if (peers != null) {
			Collections.sort(peers);

			Iterator<Address> iter = peers.iterator();
			while (iter.hasNext()) {
				Address address = iter.next();
				ProbedPeerData data = probedPeers.get(address);
				sb.append("<tr><td><div align=\"center\">");
				appendWebLink(sb, address, null);
				sb.append("</div></td><td>");
				sb.append(String.format("%.2f", data.avgRTT));
				sb.append("</td><td>");
				sb.append(String.format("%.2f", data.varRTT));
				sb.append("</td><td>");
				sb.append(String.format("%.2f", data.rtto));
				sb.append("</td><td>");
				sb.append(String.format("%.2f", data.showedRtto));
				sb.append("</td><td>");
				sb.append(String.format("%.2f", data.rttoMin));
				sb.append("</td></tr>");
			}
		} else {
			sb.append("<tr><td bgcolor=\"#FFCCFF\"><div align=\"center\">");
			sb.append("[empty]</div></td><td>Data</td></tr>");
		}
		sb.append("</table>");
		return sb.toString();
	}

	private String dumpRingViewToHtml(GetChordNeighborsResponse response) {
		StringBuilder sb = new StringBuilder();
		sb.append("<h2 align=\"center\" class=\"style2\">ChordRing:</h2>");
		sb.append("<table width=\"800\" border=\"2\" align=\"center\"><tr>");
		sb
				.append("<th class=\"style2\" width=\"100\" scope=\"col\">Predecessor</th>");
		sb
				.append("<th class=\"style2\" width=\"100\" scope=\"col\">Successor</th>");
		sb
				.append("<th class=\"style2\" width=\"300\" scope=\"col\">Successor List</th>");
		sb
				.append("<th class=\"style2\" width=\"300\" scope=\"col\">Fingers</th></tr>");
		sb.append("<tr><td><div align=\"center\">");
		appendWebLink(sb, response.getPredecessorPeer(), null);
		sb.append("</div></td><td><div align=\"center\">");
		appendWebLink(sb, response.getSuccessorPeer(), null);
		sb.append("</div></td>");
		// print successor list
		List<Address> succList = response.getSuccessorList();
		if (succList != null) {
			sb.append("<td><div align=\"left\">");
			sb.append("[");
			Iterator<Address> iter = succList.iterator();
			while (iter.hasNext()) {
				appendWebLink(sb, iter.next(), null);
				if (iter.hasNext()) {
					sb.append(", ");
				}
			}
			sb.append("]");
		} else {
			sb.append("<td bgcolor=\"#FFCCFF\"><div align=\"left\">");
			sb.append("[empty]");
		}
		// print fingers
		FingerTableView fingerTableView = response.getFingerTable();
		if (fingerTableView != null) {
			sb.append("<td><div align=\"left\">");
			sb.append("[");
			boolean first = true;
			for (int i = 0; i < fingerTableView.finger.length; i++) {
				if (fingerTableView.finger[i] != null) {
					if (!first) {
						sb.append(", ");
					}
					appendWebLink(sb, fingerTableView.finger[i], null);
					first = false;
				}
			}
			sb.append("]");
		} else {
			sb.append("<td bgcolor=\"#FFCCFF\"><div align=\"left\">");
			sb.append("[empty]");
		}
		// if (fingerTable != null) {
		// sb.append("</div></td><td><div align=\"left\">");
		// for (int i = 0; i < fingerTable.begin.length; i++) {
		// if (i > 0) {
		// sb.append(", ");
		// }
		// appendWebLink(sb, fingerTable.finger[i], null);
		// sb.append(" [").append(fingerTable.begin[i].toString());
		// sb.append(", ").append(fingerTable.end[i]).append(")");
		// }
		// } else {
		// sb.append("<td bgcolor=\"#FFCCFF\"><div align=\"left\">");
		// sb.append("[empty]");
		// }
		sb.append("</div></td></tr></table>");
		return sb.toString();
	}

	private String navigateDF(Component comp, int level) {
		StringBuilder sb = new StringBuilder();
		String space = "";
		String indent = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
		for (int i = 0; i < level; i++) {
			space += indent;
		}

		sb.append(space).append("Navigating component ").append(comp.getName());
		sb.append(" (parent is ");
		sb.append((comp.getSuperComponent() == null ? "[null]" : comp
				.getSuperComponent().getName()));
		sb.append(")<br>");
		List<Component> subcomponents = comp.getSubComponents();
		List<Channel> channels = comp.getLocalChannels();

		if (channels.size() == 0) {
			sb.append(space).append(indent).append("No local channels<br>");
		} else {
			for (Channel ch : channels) {
				sb.append(space).append(indent).append("Local channel@");
				sb.append(ch.hashCode()).append(" is ");
				sb.append(channelToString(ch)).append("<br>");
			}
		}
		if (subcomponents.size() == 0) {
			sb.append(space).append(indent)
					.append("No Children components<br>");
		} else {
			for (Component c : subcomponents) {
				sb.append(space).append(indent).append("Child is ");
				sb.append(c.getName()).append("<br>");
				sb.append(navigateDF(c, level + 1));
			}
		}
		return sb.toString();
	}

	private String channelToString(Channel c) {
		Set<Class<? extends Event>> types = c.getEventTypes();
		String ret = "[";
		for (Class<? extends Event> type : types) {
			ret += " " + type.getSimpleName() + " ";
		}
		return ret + "]";
	}

	private String getHtmlHeader() {
		StringBuilder sb = new StringBuilder();
		sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transiti");
		sb.append("onal//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-tr");
		sb.append("ansitional.dtd\"><html xmlns=\"http://www.w3.org/1999/");
		sb.append("xhtml\"><head><meta http-equiv=\"Content-Type\" conten");
		sb.append("t=\"text/html; charset=utf-8\" /><title>Peer ");
		sb.append(localAddress.getId());
		sb.append("</title><style type=\"text/css\"><!--.style2 {font-fam");
		sb.append("ily: Arial, Helvetica, sans-serif; color: #0099FF;}-->");
		sb.append("</style></head><body><h1 align=\"center\" class=\"styl");
		sb.append("e2\">Peer ").append(localAddress.getId());
		sb.append("</h1><table width=\"500\" border=\"0\" align=\"center");
		sb.append("\"><tr><td class=\"style2\" width=\"250\" scope=\"col\">");
		sb.append("<div align=\"center\">");
		appendWebLink(sb, monitorWebAddress, "<b>Monitor Server</b>");
		sb.append("</div></td><td class=\"style2\" width=");
		sb.append("\"250\" scope=\"col\"><div align=\"center\">");
		appendWebLink(sb, bootstrapWebAddress, "<b>Bootstrap Server</b>");
		sb.append("</div></td></tr></table><hr>");
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
}
