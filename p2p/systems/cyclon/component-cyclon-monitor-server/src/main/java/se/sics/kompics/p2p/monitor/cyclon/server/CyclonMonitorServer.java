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
package se.sics.kompics.p2p.monitor.cyclon.server;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.p2p.overlay.OverlayAddress;
import se.sics.kompics.p2p.overlay.cyclon.CyclonAddress;
import se.sics.kompics.p2p.overlay.cyclon.CyclonNeighbors;
import se.sics.kompics.p2p.overlay.cyclon.CyclonNodeDescriptor;
import se.sics.kompics.p2p.overlay.cyclon.GraphUtil;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.web.Web;
import se.sics.kompics.web.WebRequest;
import se.sics.kompics.web.WebResponse;
import sun.misc.BASE64Encoder;

/**
 * The <code>CyclonMonitorServer</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class CyclonMonitorServer extends ComponentDefinition {

	Positive<Network> network = positive(Network.class);
	Positive<Timer> timer = positive(Timer.class);
	Negative<Web> web = negative(Web.class);

	private static final Logger logger = LoggerFactory
			.getLogger(CyclonMonitorServer.class);

	// private long updatePeriod;
	private final HashSet<UUID> outstandingTimeouts;

	private final HashMap<OverlayAddress, OverlayViewEntry> view;

	private TreeMap<OverlayAddress, CyclonNeighbors> alivePeers;

	private TreeMap<OverlayAddress, CyclonNeighbors> deadPeers;

	private long evictAfter;

	private int webPort;

	public CyclonMonitorServer() {
		this.view = new HashMap<OverlayAddress, OverlayViewEntry>();
		this.alivePeers = new TreeMap<OverlayAddress, CyclonNeighbors>();
		this.deadPeers = new TreeMap<OverlayAddress, CyclonNeighbors>();
		this.outstandingTimeouts = new HashSet<UUID>();

		subscribe(handleInit, control);

		subscribe(handleWebRequest, web);
		subscribe(handlePeerNotification, network);
		subscribe(handleViewEvictPeer, timer);
	}

	private Handler<CyclonMonitorServerInit> handleInit = new Handler<CyclonMonitorServerInit>() {
		public void handle(CyclonMonitorServerInit event) {
			evictAfter = event.getConfiguration().getViewEvictAfter();
			webPort = event.getConfiguration().getClientWebPort();

			logger.debug("INIT");

			try {
				UIManager.setLookAndFeel(UIManager
						.getCrossPlatformLookAndFeelClassName());
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
			} catch (InstantiationException e1) {
				e1.printStackTrace();
			} catch (IllegalAccessException e1) {
				e1.printStackTrace();
			} catch (UnsupportedLookAndFeelException e1) {
				e1.printStackTrace();
			}
		}
	};

	private Handler<CyclonNeighborsNotification> handlePeerNotification = new Handler<CyclonNeighborsNotification>() {
		public void handle(CyclonNeighborsNotification event) {
			Address peerAddress = event.getPeerAddress();
			CyclonNeighbors neighbors = event.getCyclonNeighbors();
			CyclonAddress cyclonAddress = neighbors.getSelf();

			addPeerToView(cyclonAddress, neighbors);

			logger.debug("Got notification from peer {}", peerAddress);
		}
	};

	private Handler<ViewEvictPeer> handleViewEvictPeer = new Handler<ViewEvictPeer>() {
		public void handle(ViewEvictPeer event) {
			// only evict if it was not refreshed in the meantime
			// which means the timer is not anymore outstanding
			if (outstandingTimeouts.contains(event.getTimeoutId())) {
				removePeerFromView(event.getOverlayAddress());
			}
		}
	};

	private Handler<WebRequest> handleWebRequest = new Handler<WebRequest>() {
		public void handle(WebRequest event) {
			logger.debug("Handling WebRequest");

			String peers = event.getRequest().getParameter("peers");

			boolean showPeers = peers == null ? false
					: peers.equals("on") ? true : false;

			String html = dumpViewToHtml(showPeers);
			WebResponse response = new WebResponse(html, event, 1, 1);
			trigger(response, web);
		}
	};

	private void addPeerToView(OverlayAddress address, CyclonNeighbors neighbors) {
		long now = System.currentTimeMillis();

		alivePeers.put(address, neighbors);
		deadPeers.remove(address);

		OverlayViewEntry entry = view.get(address);
		if (entry == null) {
			entry = new OverlayViewEntry(address, now, now);
			view.put(address, entry);

			// set eviction timer
			ScheduleTimeout st = new ScheduleTimeout(evictAfter);
			st.setTimeoutEvent(new ViewEvictPeer(st, address));
			UUID evictionTimerId = st.getTimeoutEvent().getTimeoutId();
			entry.setEvictionTimerId(evictionTimerId);
			outstandingTimeouts.add(evictionTimerId);

			trigger(st, timer);

			logger.debug("Added peer {}", address);
		} else {
			entry.setRefreshedAt(now);

			// reset eviction timer
			outstandingTimeouts.remove(entry.getEvictionTimerId());
			trigger(new CancelTimeout(entry.getEvictionTimerId()), timer);

			ScheduleTimeout st = new ScheduleTimeout(evictAfter);
			st.setTimeoutEvent(new ViewEvictPeer(st, address));
			UUID evictionTimerId = st.getTimeoutEvent().getTimeoutId();
			entry.setEvictionTimerId(evictionTimerId);
			outstandingTimeouts.add(evictionTimerId);

			trigger(st, timer);

			logger.debug("Refreshed peer {}", address);
		}
	}

	private void removePeerFromView(OverlayAddress address) {
		if (address != null) {
			CyclonNeighbors neighbors = alivePeers.remove(address);
			deadPeers.put(address, neighbors);
			logger.debug("Removed peer {}", address);
		}
	}

	private String dumpViewToHtml(boolean showPeers) {
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

		printAlivePeers(sb, showPeers);
		if (showPeers) {
			printDeadPeers(sb);
		}
		sb.append("</body></html>");
		return sb.toString();
	}

	private void printAlivePeers(StringBuilder sb, boolean showPeers) {

		long t0 = System.currentTimeMillis();
		GraphUtil g = new GraphUtil(alivePeers);
		long t1 = System.currentTimeMillis();

		double id, od, cc, pl, istd;
		int diameter;

		id = g.getMeanInDegree();
		istd = g.getInDegreeStdDev();
		od = g.getMeanOutDegree();
		cc = g.getMeanClusteringCoefficient();
		pl = g.getMeanPathLength();
		diameter = g.getDiameter();
		int netSize = g.getNetworkSize();

		sb.append("<h2 align=\"center\" class=\"style2\">");
		sb.append("View of Cyclon Random Overlay:</h2>");
		sb.append("<table width=\"400\" border=\"1\" align=\"center\"><tr>");
		sb.append("<th class=\"style2\" width=\"250\" scope=\"col\">Metric");
		sb.append("</th><th class=\"style2\"");
		sb.append(" width=\"150\" scope=\"col\">Value</th></tr><tr>");

		sb.append("<td>Network size</td><td><div align=\"center\">");
		sb.append(netSize).append("</div></td></tr>");
		sb.append("<td>Disconnected node pairs</td><td><div align=\"center\">");
		sb.append(g.getInfinitePathCount()).append("/");
		sb.append(netSize * (netSize - 1)).append("</div></td></tr>");
		sb.append("<td>Diameter</td><td><div align=\"center\">");
		sb.append(diameter).append("</div></td></tr>");
		sb.append("<td>Average path length</td><td><div align=\"center\">");
		sb.append(String.format("%.4f", pl)).append("</div></td></tr>");
		sb.append("<td>Clustering-coefficient</td><td><div align=\"center\">");
		sb.append(String.format("%.4f", cc)).append("</div></td></tr>");
		sb.append("<td>Average in-degree</td><td><div align=\"center\">");
		sb.append(String.format("%.4f", id)).append("</div></td></tr>");
		sb
				.append("<td>In-degree standard deviation</td><td><div align=\"center\">");
		sb.append(String.format("%.4f", istd)).append("</div></td></tr>");
		sb.append("<td>Average out-degree</td><td><div align=\"center\">");
		sb.append(String.format("%.4f", od)).append("</div></td></tr>");
		sb.append("</table>");

		// print in-degree distribution
		HistogramDataset dataset = new HistogramDataset();
		double[] values = g.getInDegrees();
		int min = (int) g.getMinInDegree(), max = (int) g.getMaxInDegree();
		int bins = max - min;
		bins = bins < 1 ? 1 : bins;
		dataset.addSeries("In-degree distribution", values, bins, min, max);
		JFreeChart chart = ChartFactory.createHistogram(null, null, null,
				dataset, PlotOrientation.VERTICAL, true, false, false);
		// chart.getXYPlot().setForegroundAlpha(0.95f);
		BufferedImage image = chart.createBufferedImage(800, 300);

		String imageString = "";
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(image, "png", out);
			out.close();
			BASE64Encoder bencoder = new BASE64Encoder();
			imageString = bencoder.encode(out.toByteArray());
			imageString = "data:image/png;base64," + imageString;
		} catch (IOException e) {
			sb.append(e);
		}

		sb.append("<br><div align=\"center\"><img src=\"").append(imageString);
		sb.append("\" alt=\"In-degree distribution " + "\" /></div><br>");

		sb.append("<div align=\"center\">It took ").append(t1 - t0);
		sb.append("ms to compute these statistics.<br>");

		// refresh form
		sb.append("<form method=\"get\" name=\"rfrshFrm\" id=\"rfrshFrm\">");
		sb.append("<label>Show peers ");
		sb.append("<input name=\"peers\" type=\"checkbox\" id=\"peers\" />");
		// sb.append("checked=\"").append("false").append("\" />");
		sb.append("<input type=\"submit\" value=\"Refresh\" />");
		sb.append("</form></div>");

		if (!showPeers) {
			return;
		}

		sb.append("<h2 align=\"center\" class=\"style2\">");
		sb.append("Individual peers:</h2>");
		sb.append("<table width=\"1300\" border=\"1\" align=\"center\"><tr>");
		sb.append("<th class=\"style2\" width=\"50\" scope=\"col\">Count</th>");
		sb.append("<th class=\"style2\" width=\"50\" scope=\"col\">Peer</th>");
		sb
				.append("<th class=\"style2\" width=\"800\" scope=\"col\">Neighbors</th>");
		sb
				.append("<th class=\"style2\" width=\"50\" scope=\"col\">In Degree</th>");
		sb
				.append("<th class=\"style2\" width=\"50\" scope=\"col\">Out Degree</th>");
		sb
				.append("<th class=\"style2\" width=\"100\" scope=\"col\">Cluestering Coefficient</th>");
		sb.append("<th class=\"style2\" width=\"50\" scope=\"col\">Age</th>");
		sb
				.append("<th class=\"style2\" width=\"100\" scope=\"col\">Freshness</th></tr>");

		int count = 1;

		for (OverlayAddress address : alivePeers.keySet()) {
			CyclonNeighbors neighbors = alivePeers.get(address);

			List<CyclonNodeDescriptor> descriptors = neighbors.getDescriptors();
			Collections.sort(descriptors);

			sb.append("<tr>");
			sb.append("<td><div align=\"center\">").append(count++);
			// sb.append("(").append(g.map.get(address)).append(")");
			sb.append("</div></td>");

			// print peer address
			sb
					.append("</div></td><td bgcolor=\"#99CCFF\"><div align=\"center\">");
			appendPeerLink(sb, address);
			sb.append("</div></td>");

			// print neighbors
			if (descriptors != null) {
				sb.append("<td><div align=\"left\">");
				sb.append("[");
				Iterator<CyclonNodeDescriptor> iter = descriptors.iterator();
				while (iter.hasNext()) {
					appendPeerLink(sb, iter.next().getCyclonAddress());
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

			int v = g.getNodeIndexByAddress(address);

			// print in-degree
			sb.append("<td><div align=\"center\">").append(g.getInDegree(v));
			sb.append("</div></td>");
			// print out-degree
			sb.append("<td><div align=\"center\">").append(g.getOutDegree(v));
			sb.append("</div></td>");
			// print clustering coefficient

			// directedGraph

			sb.append("<td><div align=\"center\">").append(
					String.format("%.4f", g.getClustering(v)));
			sb.append("</div></td>");

			long now = System.currentTimeMillis();
			OverlayViewEntry viewEntry = view.get(address);

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
	}

	private void printDeadPeers(StringBuilder sb) {
		int count;
		// print dead peers
		if (deadPeers.size() > 0) {
			sb.append("<h2 align=\"center\" class=\"style2\">");
			sb.append("Dead peers:</h2>");
			sb
					.append("<table width=\"1300\" border=\"1\" align=\"center\"><tr>");
			sb
					.append("<th class=\"style2\" width=\"50\" scope=\"col\">Count</th>");
			sb
					.append("<th class=\"style2\" width=\"50\" scope=\"col\">Peer</th>");
			sb
					.append("<th class=\"style2\" width=\"800\" scope=\"col\">Neighbors</th>");
			sb
					.append("<th class=\"style2\" width=\"50\" scope=\"col\">In Degree</th>");
			sb
					.append("<th class=\"style2\" width=\"50\" scope=\"col\">Out Degree</th>");
			sb
					.append("<th class=\"style2\" width=\"100\" scope=\"col\">Cluestering Coefficient</th>");

			sb
					.append("<th class=\"style2\" width=\"50\" scope=\"col\">Lifetime</th>");
			sb
					.append("<th class=\"style2\" width=\"100\" scope=\"col\">Dead for</th></tr>");

			LinkedList<OverlayAddress> peers = new LinkedList<OverlayAddress>(
					deadPeers.descendingKeySet());

			count = 1;

			for (OverlayAddress address : peers) {
				CyclonNeighbors neighbors = deadPeers.get(address);

				List<CyclonNodeDescriptor> descriptors = neighbors
						.getDescriptors();

				sb.append("<tr>");
				sb.append("<td><div align=\"center\">").append(count++);
				// sb.append("(").append(g.map.get(address)).append(")");
				sb.append("</div></td>");

				// print peer address
				sb
						.append("</div></td><td bgcolor=\"#99CCFF\"><div align=\"center\">");
				appendPeerLink(sb, address);
				sb.append("</div></td>");

				// print neighbors
				if (descriptors != null) {
					sb.append("<td><div align=\"left\">");
					sb.append("[");
					Iterator<CyclonNodeDescriptor> iter = descriptors
							.iterator();
					while (iter.hasNext()) {
						appendPeerLink(sb, iter.next().getCyclonAddress());
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

				// print in-degree
				sb.append("<td><div align=\"center\">").append(0);
				sb.append("</div></td>");
				// print out-degree
				sb.append("<td><div align=\"center\">").append(0);
				sb.append("</div></td>");
				// print clustering coefficient

				// directedGraph

				sb.append("<td><div align=\"center\">").append(
						String.format("%.4f", 0.0));
				sb.append("</div></td>");

				long now = System.currentTimeMillis();
				OverlayViewEntry viewEntry = view.get(address);

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
	}

	private final void appendPeerLink(StringBuilder sb, OverlayAddress address) {
		sb.append("<a href=\"http://");
		sb.append(address.getPeerAddress().getIp().getHostAddress());
		sb.append(":").append(webPort).append("/");
		sb.append(address.getPeerAddress().getId()).append("/").append("\">");
		// show dead peer links in red
		if (deadPeers.containsKey(address)) {
			sb.append("<FONT style=\"BACKGROUND-COLOR: #FAAFBA\">");
			sb.append(address.toString()).append("</FONT></a>");
		} else {
			sb.append(address.toString()).append("</a>");
		}

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
