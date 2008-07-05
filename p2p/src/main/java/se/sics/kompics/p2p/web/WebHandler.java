package se.sics.kompics.p2p.web;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.ComponentMembrane;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.EventAttributeFilter;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.api.annotation.EventHandlerMethod;
import se.sics.kompics.api.annotation.MayTriggerEventTypes;
import se.sics.kompics.network.Address;
import se.sics.kompics.p2p.fd.events.StatusRequest;
import se.sics.kompics.p2p.fd.events.StatusResponse;
import se.sics.kompics.p2p.son.ps.events.GetRingNeighborsRequest;
import se.sics.kompics.p2p.son.ps.events.GetRingNeighborsResponse;
import se.sics.kompics.web.events.WebRequestEvent;
import se.sics.kompics.web.events.WebResponseEvent;

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

	private Channel chordRingRequestChannel, chordRingResponseChannel;

	// FailureDetector channels
	private Channel fdRequestChannel, fdResponseChannel;

	private Address localAddress;

	private int parts;

	private WebRequestEvent requestEvent;

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
		webRequestChannel = webMembrane.getChannel(WebRequestEvent.class);
		webResponseChannel = webMembrane.getChannel(WebResponseEvent.class);

		// use shared FailureDetector component
		ComponentMembrane fdMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.p2p.fd.FailureDetector");
		fdRequestChannel = fdMembrane.getChannel(StatusRequest.class);
		fdResponseChannel = component.createChannel(StatusResponse.class);

		// use shared ChordRing component
		ComponentMembrane crMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.p2p.son.ps.ChordRing");
		chordRingRequestChannel = crMembrane
				.getChannel(GetRingNeighborsRequest.class);
		chordRingResponseChannel = crMembrane
				.getChannel(GetRingNeighborsResponse.class);

		component.subscribe(fdResponseChannel, "handleStatusResponse");
		component.subscribe(chordRingResponseChannel,
				"handleGetRingNeighborsResponse");
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

		EventAttributeFilter filter = new EventAttributeFilter("destination",
				this.localAddress.getId());
		component.subscribe(webRequestChannel, "handleWebRequest", filter);

		logger.debug("Subscribed for WebRequestEvent with destination {}",
				localAddress.getId());
	}

	@EventHandlerMethod
	public void handleWebRequest(WebRequestEvent event) {
		String request = event.getRequest();
		logger.debug("Handling request {}", request);

		if (request.substring(1).startsWith("nav")) {
			Component boot = component;
			while (boot.getSuperComponent() != null) {
				boot = boot.getSuperComponent();
			}

			WebResponseEvent responseEvent = new WebResponseEvent(navigateDF(
					boot, 0), event, 1, 1);
			component.triggerEvent(responseEvent, webResponseChannel);
		} else {
			requestEvent = event;

			GetRingNeighborsRequest ringRequest = new GetRingNeighborsRequest();
			StatusRequest fdRequest = new StatusRequest(fdResponseChannel);

			parts = 4;

			component.triggerEvent(ringRequest, chordRingRequestChannel);
			component.triggerEvent(fdRequest, fdRequestChannel);

			WebResponseEvent responseEvent = new WebResponseEvent(htmlHeader,
					requestEvent, 1, parts);
			component.triggerEvent(responseEvent, webResponseChannel);
			responseEvent = new WebResponseEvent(htmlFooter, requestEvent,
					parts, parts);
			component.triggerEvent(responseEvent, webResponseChannel);
		}
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(WebResponseEvent.class)
	public void handleStatusResponse(StatusResponse response) {
		String html = dumpFdStatusToHtml(response);

		WebResponseEvent responseEvent = new WebResponseEvent(html,
				requestEvent, 3, parts);
		component.triggerEvent(responseEvent, webResponseChannel);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(WebResponseEvent.class)
	public void handleGetRingNeighborsResponse(GetRingNeighborsResponse event) {
		String html = dumpRingViewToHtml(event);

		WebResponseEvent responseEvent = new WebResponseEvent(html,
				requestEvent, 2, parts);
		component.triggerEvent(responseEvent, webResponseChannel);
	}

	private String dumpFdStatusToHtml(StatusResponse response) {
		LinkedList<Address> peers = response.getProbedPeers();
		StringBuffer sb = new StringBuffer();

		sb.append("<h2 align=\"center\" class=\"style2\">Failure Detector:");
		sb.append("</h2><table width=\"500\" border=\"2\" align=\"center\">");
		sb.append("<th class=\"style2\" width=\"100\" scope=\"col\">Peer</th>");
		sb.append("<th class=\"style2\" width=\"100\" scope=\"col\">Data</th>");
		sb.append("</tr>");

		if (peers != null) {
			Iterator<Address> iter = peers.iterator();
			while (iter.hasNext()) {
				sb.append("<tr><td><div align=\"center\">");
				appendWebLink(sb, iter.next(), null);
				sb.append("</div></td><td>Data</td></tr>");
			}
		} else {
			sb.append("<tr><td bgcolor=\"#FFCCFF\"><div align=\"center\">");
			sb.append("[empty]</div></td><td>Data</td></tr>");
		}
		return sb.toString();
	}

	private String dumpRingViewToHtml(GetRingNeighborsResponse response) {
		StringBuffer sb = new StringBuffer();
		sb.append("<h2 align=\"center\" class=\"style2\">ChordRing:</h2>");
		sb.append("<table width=\"500\" border=\"2\" align=\"center\"><tr>");
		sb
				.append("<th class=\"style2\" width=\"100\" scope=\"col\">Predecessor</th>");
		sb
				.append("<th class=\"style2\" width=\"100\" scope=\"col\">Successor</th>");
		sb
				.append("<th class=\"style2\" width=\"300\" scope=\"col\">Successor List</th></tr>");
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
		sb.append("</div></td></tr></table>");
		return sb.toString();
	}

	private String navigateDF(Component comp, int level) {
		StringBuffer sb = new StringBuffer();
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
		StringBuffer sb = new StringBuffer();
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

	private void appendWebLink(StringBuffer sb, Address address, String label) {
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
