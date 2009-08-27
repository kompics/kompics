/**
 * This file is part of the Kompics P2P Framework.
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
package se.sics.kompics.web.jetty;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.web.Web;
import se.sics.kompics.web.WebRequest;
import se.sics.kompics.web.WebResponse;

/**
 * The <code>JettyWebServer</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class JettyWebServer extends ComponentDefinition {

	Positive<Web> web = positive(Web.class);

	private static final Logger logger = LoggerFactory
			.getLogger(JettyWebServer.class);

	private final HashMap<WebRequest, LinkedBlockingQueue<WebResponse>> activeRequests;

	private long requestTimeout;

	private long requestId;

	private final JettyWebServer thisWS = this;

	private String homePage;

	public JettyWebServer() {
		activeRequests = new HashMap<WebRequest, LinkedBlockingQueue<WebResponse>>();

		subscribe(handleInit, control);
		subscribe(handleWebResponse, web);
	}

	private Handler<JettyWebServerInit> handleInit = new Handler<JettyWebServerInit>() {
		public void handle(JettyWebServerInit event) {
			logger.debug("Handling init in thread {}", Thread.currentThread());

			requestTimeout = event.getRequestTimeout();
			homePage = event.getHomePage();
			if (homePage == null) {
				homePage = "<h1>Welcome!</h1>"
						+ "This is the JettyWebServer Kompics component.<br>"
						+ "Please initialize me with a proper home page.";
			}

			Server server = new Server(event.getPort());
			// server.setStopAtShutdown(true);
			QueuedThreadPool qtp = new QueuedThreadPool();
			qtp.setMinThreads(1);
			qtp.setMaxThreads(event.getMaxThreads());
			qtp.setDaemon(true);
			server.setThreadPool(qtp);

			Connector connector = new SelectChannelConnector();
			connector.setHost(event.getIp().getCanonicalHostName());
			connector.setPort(event.getPort());
			server.setConnectors(new Connector[] { connector });

			try {
				org.mortbay.jetty.Handler webHandler = new JettyHandler(thisWS);
				server.setHandler(webHandler);
				server.start();
			} catch (Exception e) {
				throw new RuntimeException(
						"Cannot initialize the Jetty web server", e);
			}
		}
	};

	private Handler<WebResponse> handleWebResponse = new Handler<WebResponse>() {
		public void handle(WebResponse event) {
			logger.debug("Handling web response {}", event);

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

	void handleRequest(String target, org.mortbay.jetty.Request request,
			HttpServletResponse response) throws IOException {
		logger.debug("Handling request {} in thread {}", target, Thread
				.currentThread());

		String[] args = target.split("/");

		int destination = -1;
		String command = "";
		if (args.length >= 2) {
			try {
				destination = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				destination = -1;
			}
		}
		if (args.length >= 3) {
			command = args[2];
		}
		// System.err.println("TAGET:\"" + target + "\", COM=" + command +
		// "DEST="
		// + destination + " PARAMS " + request.getParameterMap().keySet()
		// + " VALS " + request.getParameter("kadLookup"));

		if (destination == -1) {
			// render home page
			response.getWriter().println(homePage);
			response.flushBuffer();
			return;
		}

		WebRequest requestEvent = new WebRequest(destination, requestId++,
				command, request);

		LinkedBlockingQueue<WebResponse> queue = new LinkedBlockingQueue<WebResponse>();

		synchronized (activeRequests) {
			activeRequests.put(requestEvent, queue);
		}

		logger.debug("Triggering request {}", target);
		trigger(requestEvent, web);

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
