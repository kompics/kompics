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
package se.sics.kompics.p2p.web.chord;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.fdstatus.FailureDetectorStatus;
import se.sics.kompics.p2p.fdstatus.ProbedPeerData;
import se.sics.kompics.p2p.fdstatus.StatusRequest;
import se.sics.kompics.p2p.fdstatus.StatusResponse;
import se.sics.kompics.p2p.overlay.OverlayAddress;
import se.sics.kompics.p2p.overlay.chord.ChordAddress;
import se.sics.kompics.p2p.overlay.chord.ChordLookupRequest;
import se.sics.kompics.p2p.overlay.chord.ChordLookupResponse;
import se.sics.kompics.p2p.overlay.chord.ChordNeighborsRequest;
import se.sics.kompics.p2p.overlay.chord.ChordNeighborsResponse;
import se.sics.kompics.p2p.overlay.chord.ChordStatus;
import se.sics.kompics.p2p.overlay.chord.ChordStructuredOverlay;
import se.sics.kompics.p2p.overlay.chord.FingerTableView;
import se.sics.kompics.p2p.overlay.chord.LookupInfo;
import se.sics.kompics.p2p.overlay.chord.ChordLookupResponse.ChordLookupStatus;
import se.sics.kompics.p2p.overlay.key.NumericRingKey;
import se.sics.kompics.web.Web;
import se.sics.kompics.web.WebRequest;
import se.sics.kompics.web.WebResponse;

/**
 * The <code>ChordWebApplication</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class ChordWebApplication extends ComponentDefinition {

	Negative<Web> web = negative(Web.class);
	Positive<FailureDetectorStatus> epfdStatus = positive(FailureDetectorStatus.class);
	Positive<ChordStructuredOverlay> chord = positive(ChordStructuredOverlay.class);
	Positive<ChordStatus> chordStatus = positive(ChordStatus.class);

	private Logger logger;

	private ChordAddress self;

	private int parts;

	private WebRequest requestEvent;

	private String htmlHeader, htmlFooter;

	private Address monitorWebAddress, bootstrapWebAddress;

	public ChordWebApplication() {
		subscribe(handleInit, control);

		subscribe(handleWebRequest, web);

		subscribe(handleStatusResponse, epfdStatus);
		subscribe(handleGetChordNeighborsResponse, chordStatus);

		subscribe(handleChordLookupResponse, chord);
	}

	Handler<ChordWebApplicationInit> handleInit = new Handler<ChordWebApplicationInit>() {
		public void handle(ChordWebApplicationInit init) {
			self = init.getSelf();
			monitorWebAddress = init.getMonitorWebAddress();
			bootstrapWebAddress = init.getBootstrapWebAddress();

			logger = LoggerFactory.getLogger(ChordWebApplication.class
					.getName()
					+ "@" + self.getKey());

			htmlHeader = getHtmlHeader();
			htmlFooter = "</body></html>";
		}
	};

	private Handler<WebRequest> handleWebRequest = new Handler<WebRequest>() {
		public void handle(WebRequest event) {
			String target = event.getTarget();
			org.mortbay.jetty.Request request = event.getRequest();
			logger.debug("Handling request {}", target);

			requestEvent = event;

			ChordNeighborsRequest ringRequest = new ChordNeighborsRequest();
			StatusRequest fdRequest = new StatusRequest();

			parts = 5;

			String chordLookupKey = request.getParameter("chordLookup");

			if (chordLookupKey != null) {
				// do Chord lookup
				ChordLookupRequest lookupRequest = new ChordLookupRequest(
						new NumericRingKey(new BigInteger(chordLookupKey)),
						null);
				trigger(lookupRequest, chord);
			} else {
				// just print lookup form
				WebResponse response = new WebResponse(dumpChordLookupForm(
						null, null), requestEvent, 2, parts);
				trigger(response, web);
			}

			trigger(ringRequest, chordStatus);
			trigger(fdRequest, epfdStatus);

			WebResponse responseEvent = new WebResponse(htmlHeader,
					requestEvent, 1, parts);
			trigger(responseEvent, web);
			responseEvent = new WebResponse(htmlFooter, requestEvent, parts,
					parts);
			trigger(responseEvent, web);
		}
	};

	private Handler<ChordLookupResponse> handleChordLookupResponse = new Handler<ChordLookupResponse>() {
		public void handle(ChordLookupResponse response) {
			WebResponse responseEvent = new WebResponse(dumpChordLookupForm(
					response.getKey(), response), requestEvent, 2, parts);
			trigger(responseEvent, web);
		}
	};

	private Handler<StatusResponse> handleStatusResponse = new Handler<StatusResponse>() {
		public void handle(StatusResponse response) {
			String html = dumpFdStatusToHtml(response);

			WebResponse responseEvent = new WebResponse(html, requestEvent, 4,
					parts);
			trigger(responseEvent, web);
		}
	};

	private Handler<ChordNeighborsResponse> handleGetChordNeighborsResponse = new Handler<ChordNeighborsResponse>() {
		public void handle(ChordNeighborsResponse event) {
			String html = dumpRingViewToHtml(event);

			WebResponse responseEvent = new WebResponse(html, requestEvent, 3,
					parts);
			trigger(responseEvent, web);
		}
	};

	private String dumpChordLookupForm(NumericRingKey key,
			ChordLookupResponse response) {
		StringBuilder sb = new StringBuilder();

		sb.append("<h2 align=\"center\" class=\"style2\">Chord Lookup:</h2>");
		sb.append("<table width=\"800\" border=\"0\" align=\"center\"><tr><td");
		sb.append("><form method=\"get\" name=\"lkpFrm\" id=\"chordLkpFrm\">");
		sb.append("<fieldset><legend> Chord </legend><label>Lookup key ");
		sb.append("<input name=\"chordLookup\" type=\"text\" id=\"lookip\" ");
		sb.append("value=\"").append(key != null ? key : self.getKey().next());
		sb.append("\" size=\"6\"/>");
		sb.append("</label><input type=\"submit\" value=\"Lookup\" />");
		if (key != null) {
			if (response.getStatus() == ChordLookupStatus.SUCCESS) {
				LookupInfo info = response.getLookupInfo();

				sb.append("<label> Peer ");
				appendPeerLink(sb, response.getResponsible());
				sb.append(" is responsible for key ").append(key).append(".");
				sb.append("<br>Lookup took ").append(info.getDuration());
				sb.append(" milliseconds and traversed ");
				sb.append(info.getHopCount()).append(" hops [");
				for (ChordAddress hop : info.getHops()) {
					sb.append(" ");
					appendPeerLink(sb, hop);
				}
				sb.append(" ].</label>");
			} else if (response.getStatus() == ChordLookupStatus.FAILURE) {
				sb.append("<label> Lookup for key ").append(key);
				sb.append(" failed because of peer ");
				sb.append(response.getResponsible());
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
				appendPeerLink(sb, data.overlayAddress);
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

	private String dumpRingViewToHtml(ChordNeighborsResponse response) {
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
		if (response.getNeighbors().getPredecessorPeer() != null) {
			appendPeerLink(sb, response.getNeighbors().getPredecessorPeer());
		} else {
			sb.append("NIL");
		}
		sb.append("</div></td><td><div align=\"center\">");
		if (response.getNeighbors().getSuccessorPeer() != null) {
			appendPeerLink(sb, response.getNeighbors().getSuccessorPeer());
		} else {
			sb.append("NIL");
		}
		sb.append("</div></td>");
		// print successor list
		List<ChordAddress> succList = response.getNeighbors()
				.getSuccessorList();
		if (succList != null) {
			sb.append("<td><div align=\"left\">");
			sb.append("[");
			Iterator<ChordAddress> iter = succList.iterator();
			while (iter.hasNext()) {
				ChordAddress ca = iter.next();
				appendPeerLink(sb, ca);
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
		FingerTableView fingerTableView = response.getNeighbors()
				.getFingerTable();
		if (fingerTableView != null) {
			sb.append("<td><div align=\"left\">");
			sb.append("[");
			boolean first = true;
			for (int i = 0; i < fingerTableView.finger.length; i++) {
				if (fingerTableView.finger[i] != null) {
					if (!first) {
						sb.append(", ");
					}
					appendPeerLink(sb, fingerTableView.finger[i]);
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

		if (fingerTableView != null) {
			long atTime = response.getNeighbors().getAtTime();
			sb
					.append("<h2 align=\"center\" class=\"style2\">ChordFingerTable:</h2>");
			sb
					.append("<table width=\"500\" border=\"2\" align=\"center\"><tr>");
			sb
					.append("<th class=\"style2\" width=\"100\" scope=\"col\">Finger</th>");
			sb
					.append("<th class=\"style2\" width=\"100\" scope=\"col\">Begin</th>");
			sb
					.append("<th class=\"style2\" width=\"100\" scope=\"col\">End</th>");
			sb
					.append("<th class=\"style2\" width=\"100\" scope=\"col\">Peer</th>");
			sb
					.append("<th class=\"style2\" width=\"100\" scope=\"col\">Freshness</th></tr>");
			for (int i = 0; i < fingerTableView.finger.length; i++) {
				sb.append("<tr><td><div align=\"center\">").append(i + 1);
				sb.append("</div></td>");
				sb.append("<td><div align=\"center\">");
				sb.append(fingerTableView.begin[i]).append("</div></td>");
				sb.append("<td><div align=\"center\">");
				sb.append(fingerTableView.end[i]).append("</div></td>");
				sb.append("<td><div align=\"center\">");
				if (fingerTableView.finger[i] == null) {
					sb.append("NIL");
				} else {
					appendPeerLink(sb, fingerTableView.finger[i]);
				}
				sb.append("</div></td>");
				sb.append("<td><div align=\"center\">");
				sb.append(durationToString(atTime
						- fingerTableView.lastUpdated[i]));
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
		sb.append("t=\"text/html; charset=utf-8\" /><title>Chord Peer ");
		sb.append(self.getKey());
		sb.append("</title><style type=\"text/css\"><!--.style2 {font-fam");
		sb.append("ily: Arial, Helvetica, sans-serif; color: #0099FF;}-->");
		sb.append("</style></head><body><h1 align=\"center\" class=\"styl");
		sb.append("e2\">Chord Peer ").append(self.getKey()).append("</h1>");
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
		sb.append(":").append(address.getPeerAddress().getPort() - 1).append(
				"/");
		sb.append(address.getPeerAddress().getId()).append("/").append("\">");
		sb.append(address.toString()).append("</a>");
	}

	private final void appendPeerLink(StringBuilder sb, Address peerAddress,
			String label) {
		sb.append("<a href=\"http://");
		sb.append(peerAddress.getIp().getHostAddress());
		sb.append(":").append(peerAddress.getPort() - 1).append("/");
		sb.append(peerAddress.getId()).append("/Chord").append("\">");
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
