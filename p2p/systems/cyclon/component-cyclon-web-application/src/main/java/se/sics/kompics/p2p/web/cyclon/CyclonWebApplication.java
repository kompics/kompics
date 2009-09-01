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
package se.sics.kompics.p2p.web.cyclon;

import java.util.ArrayList;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.overlay.OverlayAddress;
import se.sics.kompics.p2p.overlay.cyclon.CyclonAddress;
import se.sics.kompics.p2p.overlay.cyclon.CyclonNeighborsRequest;
import se.sics.kompics.p2p.overlay.cyclon.CyclonNeighborsResponse;
import se.sics.kompics.p2p.overlay.cyclon.CyclonNodeDescriptor;
import se.sics.kompics.p2p.overlay.cyclon.CyclonPeerSampling;
import se.sics.kompics.p2p.overlay.cyclon.CyclonStatus;
import se.sics.kompics.web.Web;
import se.sics.kompics.web.WebRequest;
import se.sics.kompics.web.WebResponse;

/**
 * The <code>CyclonWebApplication</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class CyclonWebApplication extends ComponentDefinition {

	Negative<Web> web = negative(Web.class);
	Positive<CyclonPeerSampling> cyclon = positive(CyclonPeerSampling.class);
	Positive<CyclonStatus> cyclonStatus = positive(CyclonStatus.class);

	private Logger logger;

	private CyclonAddress self;

	private int parts;

	private WebRequest requestEvent;

	private String htmlHeader, htmlFooter;

	private Address monitorWebAddress, bootstrapWebAddress;

	int webPort;

	public CyclonWebApplication() {
		subscribe(handleInit, control);
		subscribe(handleWebRequest, web);
		subscribe(handleCyclonNeighborsResponse, cyclonStatus);
	}

	Handler<CyclonWebApplicationInit> handleInit = new Handler<CyclonWebApplicationInit>() {
		public void handle(CyclonWebApplicationInit init) {
			self = init.getSelf();
			monitorWebAddress = init.getMonitorWebAddress();
			bootstrapWebAddress = init.getBootstrapWebAddress();
			webPort = init.getWebPort();

			logger = LoggerFactory.getLogger(CyclonWebApplication.class
					.getName()
					+ "@" + self.getCyclonId());

			htmlHeader = getHtmlHeader();
			htmlFooter = "</body></html>";
		}
	};

	private Handler<WebRequest> handleWebRequest = new Handler<WebRequest>() {
		public void handle(WebRequest event) {
			String target = event.getTarget();
			logger.debug("Handling request {}", target);

			requestEvent = event;

			CyclonNeighborsRequest cyclonNeighborsRequest = new CyclonNeighborsRequest();

			parts = 3;

			trigger(cyclonNeighborsRequest, cyclonStatus);

			WebResponse responseEvent = new WebResponse(htmlHeader,
					requestEvent, 1, parts);
			trigger(responseEvent, web);
			responseEvent = new WebResponse(htmlFooter, requestEvent, parts,
					parts);
			trigger(responseEvent, web);
		}
	};

	private Handler<CyclonNeighborsResponse> handleCyclonNeighborsResponse = new Handler<CyclonNeighborsResponse>() {
		public void handle(CyclonNeighborsResponse event) {
			String html = dumpCyclonViewToHtml(event);

			WebResponse responseEvent = new WebResponse(html, requestEvent, 2,
					parts);
			trigger(responseEvent, web);
		}
	};

	private String dumpCyclonViewToHtml(CyclonNeighborsResponse response) {
		StringBuilder sb = new StringBuilder();

		sb.append("<h2 align=\"center\" class=\"style2\">CyclonView:</h2>");

		ArrayList<CyclonNodeDescriptor> descriptors = response.getNeighbors()
				.getDescriptors();
		Collections.sort(descriptors);

		if (descriptors != null) {
			long atTime = response.getNeighbors().getAtTime();
			sb
					.append("<table width=\"500\" border=\"2\" align=\"center\"><tr>");
			sb
					.append("<th class=\"style2\" width=\"100\" scope=\"col\">Count</th>");
			sb
					.append("<th class=\"style2\" width=\"100\" scope=\"col\">Peer</th>");
			sb
					.append("<th class=\"style2\" width=\"100\" scope=\"col\">Age</th>");
			sb
					.append("<th class=\"style2\" width=\"100\" scope=\"col\">Freshness</th></tr>");
			int i = 0;
			for (CyclonNodeDescriptor descriptor : descriptors) {
				sb.append("<tr><td><div align=\"center\">").append(++i);
				sb.append("</div></td>");
				sb.append("<td><div align=\"center\">");
				appendPeerLink(sb, descriptor.getCyclonAddress());
				sb.append("</div></td>");
				sb.append("<td><div align=\"center\">");
				sb.append(descriptor.getAge()).append("</div></td>");
				sb.append("<td><div align=\"center\">");
				sb.append(durationToString(atTime - atTime));
				sb.append("</div></td></tr>");
			}
			sb.append("</table>");
		}
		return sb.toString();
	}

	private String getHtmlHeader() {
		StringBuilder sb = new StringBuilder();
		sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transiti");
		sb.append("onal//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-tr");
		sb.append("ansitional.dtd\"><html xmlns=\"http://www.w3.org/1999/");
		sb.append("xhtml\"><head><meta http-equiv=\"Content-Type\" conten");
		sb.append("t=\"text/html; charset=utf-8\" /><title>Cyclon Peer ");
		sb.append(self.getCyclonId());
		sb.append("</title><style type=\"text/css\"><!--.style2 {font-fam");
		sb.append("ily: Arial, Helvetica, sans-serif; color: #0099FF;}-->");
		sb.append("</style></head><body><h1 align=\"center\" class=\"styl");
		sb.append("e2\">Cyclon Peer ").append(self.getCyclonId()).append(
				"</h1>");
		sb.append("<div align=\"center\">Peer address: ");
		sb.append(self.getPeerAddress()).append("</div>");
		sb.append("<table width=\"500\" border=\"0\" align=\"center");
		sb.append("\"><tr><td class=\"style2\" width=\"250\" scope=\"col\">");
		sb.append("<div align=\"center\">");
		appendPeerLink(sb, monitorWebAddress, "<b>Monitor Server</b>");
		sb.append("</div></td><td class=\"style2\" width=");
		sb.append("\"250\" scope=\"col\"><div align=\"center\">");
		appendPeerLink(sb, bootstrapWebAddress, "<b>Bootstrap Server</b>");
		sb.append("</div></td></tr></table><hr>");
		return sb.toString();
	}

	private final void appendPeerLink(StringBuilder sb, OverlayAddress address) {
		sb.append("<a href=\"http://");
		sb.append(address.getPeerAddress().getIp().getHostAddress());
		sb.append(":").append(webPort).append("/");
		sb.append(address.getPeerAddress().getId()).append("/").append("\">");
		sb.append(address.toString()).append("</a>");
	}

	private final void appendPeerLink(StringBuilder sb, Address peerAddress,
			String label) {
		sb.append("<a href=\"http://");
		sb.append(peerAddress.getIp().getHostAddress());
		sb.append(":").append(webPort).append("/");
		sb.append(peerAddress.getId()).append("/Cyclon").append("\">");
		sb.append(label).append("</a>");
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
