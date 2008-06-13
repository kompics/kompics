package se.sics.kompics.p2p.web;

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
import se.sics.kompics.network.Address;
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

	private Address localAddress;

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
	}

	@ComponentInitializeMethod
	public void init(Address localAddress) {
		this.localAddress = localAddress;
		logger = LoggerFactory.getLogger(WebHandler.class.getName() + "@"
				+ localAddress.getId());

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

		// int parts = 10;
		// int to = 1;
		//
		// for (int i = to; i < parts; i++) {
		// WebResponseEvent responseEvent = new WebResponseEvent(request + i
		// + "/" + parts + " My address is " + localAddress + "<br>",
		// event, i, parts);
		// component.triggerEvent(responseEvent, webResponseChannel);
		// }

		if (request.substring(1).startsWith("nav")) {
			Component boot = component;
			while (boot.getSuperComponent() != null) {
				boot = boot.getSuperComponent();
			}

			WebResponseEvent responseEvent = new WebResponseEvent(navigateDF(
					boot, 0), event, 1, 1);
			component.triggerEvent(responseEvent, webResponseChannel);
		} else {
			WebResponseEvent responseEvent = new WebResponseEvent(
					"My address is " + localAddress + "<br>", event, 1, 1);
			component.triggerEvent(responseEvent, webResponseChannel);
		}
	}

	private String navigateDF(Component comp, int level) {
		StringBuffer out = new StringBuffer();
		String space = "";
		String indent = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
		for (int i = 0; i < level; i++) {
			space += indent;
		}

		out.append(space).append("Navigating component ")
				.append(comp.getName());
		out.append(" (parent is ");
		out.append((comp.getSuperComponent() == null ? "[null]" : comp
				.getSuperComponent().getName()));
		out.append(")<br>");
		List<Component> subcomponents = comp.getSubComponents();
		List<Channel> channels = comp.getLocalChannels();

		if (channels.size() == 0) {
			out.append(space).append(indent).append("No local channels<br>");
		} else {
			for (Channel ch : channels) {
				out.append(space).append(indent).append("Local channel@");
				out.append(ch.hashCode()).append(" is ");
				out.append(channelToString(ch)).append("<br>");
			}
		}
		if (subcomponents.size() == 0) {
			out.append(space).append(indent).append(
					"No Children components<br>");
		} else {
			for (Component c : subcomponents) {
				out.append(space).append(indent).append("Child is ");
				out.append(c.getName()).append("<br>");
				out.append(navigateDF(c, level + 1));
			}
		}
		return out.toString();
	}

	private String channelToString(Channel c) {
		Set<Class<? extends Event>> types = c.getEventTypes();
		String ret = "[";
		for (Class<? extends Event> type : types) {
			ret += " " + type.getSimpleName() + " ";
		}
		return ret + "]";
	}
}
