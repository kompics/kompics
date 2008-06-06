package se.sics.kompics.p2p.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.ComponentMembrane;
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

		int parts = 10;
		int to = 1;

		for (int i = to; i <= parts; i++) {
			WebResponseEvent responseEvent = new WebResponseEvent(request + i
					+ "/" + parts + "<br>", event, i, parts);
			component.triggerEvent(responseEvent, webResponseChannel);
		}
	}
}
