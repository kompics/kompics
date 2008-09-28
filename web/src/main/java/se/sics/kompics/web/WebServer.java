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
import se.sics.kompics.api.EventHandler;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentShareMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.web.events.WebRequest;
import se.sics.kompics.web.events.WebResponse;

/**
 * The <code>WebServer</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public final class WebServer {

	private static final Logger logger = LoggerFactory
			.getLogger(WebServer.class);

	private final Component component;

	private Channel requestChannel, responseChannel;

	private final HashMap<WebRequest, LinkedBlockingQueue<WebResponse>> activeRequests;

	private long requestTimeout;

	private long requestId;

	public WebServer(Component component) {
		this.component = component;
		activeRequests = new HashMap<WebRequest, LinkedBlockingQueue<WebResponse>>();
	}

	@ComponentCreateMethod
	public void create(Channel requestChannel, Channel responseChannel) {
		this.requestChannel = requestChannel;
		this.responseChannel = responseChannel;

		component.subscribe(responseChannel, handleWebResponse);
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
		ComponentMembrane membrane = new ComponentMembrane(component);
		membrane.outChannel(WebRequest.class, requestChannel);
		membrane.inChannel(WebResponse.class, responseChannel);
		membrane.seal();
		return component.registerSharedComponentMembrane(name, membrane);
	}

	private EventHandler<WebResponse> handleWebResponse = new EventHandler<WebResponse>() {
		public void handle(WebResponse event) {
			WebRequest requestEvent = event.getRequestEvent();

			LinkedBlockingQueue<WebResponse> queue;

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
	};

	void handleRequest(String target, Request request,
			HttpServletResponse response) throws IOException {
		logger.debug("Handling request {}", target);

		String[] args = target.split("/");

		String handlerId = args[1];

		WebRequest requestEvent = new WebRequest(new BigInteger(handlerId),
				requestId++, target.substring(target.indexOf('/', 1)), request);

		LinkedBlockingQueue<WebResponse> queue = new LinkedBlockingQueue<WebResponse>();

		synchronized (activeRequests) {
			activeRequests.put(requestEvent, queue);
		}

		logger.debug("Triggering request {}", target);
		component.triggerEvent(requestEvent, requestChannel);

		int expectedPart = 1;
		HashMap<Integer, WebResponse> earlyResponses = new HashMap<Integer, WebResponse>();
		do {
			WebResponse responseEvent;
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
