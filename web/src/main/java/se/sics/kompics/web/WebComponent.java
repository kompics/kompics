package se.sics.kompics.web;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.ComponentMembrane;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentShareMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.api.annotation.EventHandlerMethod;
import se.sics.kompics.web.events.WebRequestEvent;
import se.sics.kompics.web.events.WebResponseEvent;

/**
 * The <code>WebComponent</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public final class WebComponent {

	private static final Logger logger = LoggerFactory
			.getLogger(WebComponent.class);

	private final Component component;

	private Channel requestChannel, responseChannel;

	private final HashMap<WebRequestEvent, LinkedBlockingQueue<WebResponseEvent>> activeRequests;

	private long requestTimeout;

	private long requestId;

	public WebComponent(Component component) {
		this.component = component;
		activeRequests = new HashMap<WebRequestEvent, LinkedBlockingQueue<WebResponseEvent>>();
	}

	@ComponentCreateMethod
	public void create(Channel requestChannel, Channel responseChannel) {
		this.requestChannel = requestChannel;
		this.responseChannel = responseChannel;

		component.subscribe(responseChannel, "handleWebResponseEvent");
	}

	@ComponentInitializeMethod
	public void init(String host, int port, long requestTimeout)
			throws Exception {

		this.requestTimeout = requestTimeout;

		Server server = new Server();
		Connector connector = new SelectChannelConnector();
		if (host != null && !host.equals("")) {
			connector.setHost(host);
		}
		connector.setPort(port);
		server.setConnectors(new Connector[] { connector });

		Handler webHandler = new WebHandler(this);
		server.setHandler(webHandler);

		server.start();
	}

	@ComponentShareMethod
	public ComponentMembrane share(String name) {
		HashMap<Class<? extends Event>, Channel> map = new HashMap<Class<? extends Event>, Channel>();
		map.put(WebRequestEvent.class, requestChannel);
		map.put(WebResponseEvent.class, responseChannel);
		ComponentMembrane membrane = new ComponentMembrane(component, map);
		return component.registerSharedComponentMembrane(name, membrane);
	}

	@EventHandlerMethod
	public void handleWebResponseEvent(WebResponseEvent event) {
		WebRequestEvent requestEvent = event.getRequestEvent();

		LinkedBlockingQueue<WebResponseEvent> queue;

		synchronized (activeRequests) {
			queue = activeRequests.get(requestEvent);
			if (queue != null) {
				queue.offer(event);
			} else {
				// request expired
				return;
			}
		}
	}

	void handleRequest(String target, Request request,
			HttpServletResponse response) throws IOException {
		logger.debug("Handling request {}", target);

		String[] args = target.split("/");

		String handlerId = args[1];

		WebRequestEvent requestEvent = new WebRequestEvent(new BigInteger(
				handlerId), requestId++, target.substring(target
				.indexOf('/', 1)), request);

		LinkedBlockingQueue<WebResponseEvent> queue = new LinkedBlockingQueue<WebResponseEvent>();

		synchronized (activeRequests) {
			activeRequests.put(requestEvent, queue);
		}

		logger.debug("Triggering request {}", target);
		component.triggerEvent(requestEvent, requestChannel);

		int expectedPart = 1;
		HashMap<Integer, WebResponseEvent> earlyResponses = new HashMap<Integer, WebResponseEvent>();
		do {
			WebResponseEvent responseEvent;
			while (true) {
				try {
					// wait for response event
					responseEvent = queue.poll(requestTimeout,
							TimeUnit.MILLISECONDS);
					break; // waiting
				} catch (InterruptedException e) {
					continue; // waiting
				}
			}

			if (responseEvent != null) {
				logger.debug("I got response({}), part {}/{}", new Object[] {
						target, responseEvent.getPartIndex(),
						responseEvent.getPartsTotal() });
				// got a response event
				if (responseEvent.getPartIndex() == expectedPart) {
					// got expected part
					logger.debug("Writing response({}), part {}/{}",
							new Object[] { target, expectedPart,
									responseEvent.getPartsTotal() });
					response.getWriter().println(responseEvent.getHtml());
					response.flushBuffer();

					if (expectedPart == responseEvent.getPartsTotal()) {
						// got all parts
						break;
					} else {
						// more parts expected
						expectedPart++;
						// maybe got here before
						while (earlyResponses.containsKey(expectedPart)) {
							logger.debug("Writing response({}), part {}/{}",
									new Object[] { target, expectedPart,
											responseEvent.getPartsTotal() });
							response.getWriter().println(
									earlyResponses.get(expectedPart).getHtml());
							response.flushBuffer();
							expectedPart++;
						}

						if (expectedPart > responseEvent.getPartsTotal()) {
							// got all parts now
							break;
						}
					}
				} else {
					// got a future part
					earlyResponses.put(responseEvent.getPartIndex(),
							responseEvent);
				}
			} else {
				// request expired
				response.getWriter().println("Request expired! <br>");
				response.flushBuffer();
				logger.debug("Request expired: {}", target);
				break;
			}
		} while (true);

		synchronized (activeRequests) {
			activeRequests.remove(requestEvent);
		}
	}
}
