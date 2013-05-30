/**
 * This file is part of the Kompics P2P Framework.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package se.sics.kompics.p2p.monitor.chord.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.p2p.overlay.OverlayAddress;
import se.sics.kompics.p2p.overlay.chord.ChordAddress;
import se.sics.kompics.p2p.overlay.chord.ChordNeighbors;
import se.sics.kompics.p2p.overlay.chord.FingerTableView;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.web.Web;
import se.sics.kompics.web.WebRequest;
import se.sics.kompics.web.WebResponse;

/**
 * The
 * <code>ChordMonitorServer</code> class.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class ChordMonitorServer extends ComponentDefinition {

    Positive<Network> network = positive(Network.class);
    Positive<Timer> timer = positive(Timer.class);
    Negative<Web> web = negative(Web.class);
    private static final Logger logger = LoggerFactory
            .getLogger(ChordMonitorServer.class);
    // private long updatePeriod;
    private final HashSet<UUID> outstandingTimeouts;
    private final HashMap<OverlayAddress, OverlayViewEntry> view;
    private HashMap<OverlayAddress, OverlayAddress> successor, predecessor;
    private TreeMap<OverlayAddress, ChordNeighbors> alivePeers;
    private TreeMap<OverlayAddress, ChordNeighbors> deadPeers;
    private long evictAfter;

    public ChordMonitorServer(ChordMonitorServerInit init) {
        this.view = new HashMap<OverlayAddress, OverlayViewEntry>();
        this.successor = new HashMap<OverlayAddress, OverlayAddress>();
        this.predecessor = new HashMap<OverlayAddress, OverlayAddress>();
        this.alivePeers = new TreeMap<OverlayAddress, ChordNeighbors>();
        this.deadPeers = new TreeMap<OverlayAddress, ChordNeighbors>();
        this.outstandingTimeouts = new HashSet<UUID>();


        subscribe(handleWebRequest, web);
        subscribe(handlePeerNotification, network);
        subscribe(handleViewEvictPeer, timer);

        // INIT
        evictAfter = init.getConfiguration().getViewEvictAfter();

        logger.debug("INIT");
    }
    private Handler<ChordNeighborsNotification> handlePeerNotification = new Handler<ChordNeighborsNotification>() {
        public void handle(ChordNeighborsNotification event) {
            Address peerAddress = event.getPeerAddress();
            ChordNeighbors neighbors = event.getChordNeighbors();
            ChordAddress chordAddress = neighbors.getLocalPeer();

            addPeerToView(chordAddress, event.getClientWebPort(), neighbors);

            OverlayAddress pred = alivePeers.lowerKey(chordAddress);
            if (pred == null) {
                pred = alivePeers.lastKey();
            }
            predecessor.put(chordAddress, pred);
            successor.put(pred, chordAddress);

            OverlayAddress succ = alivePeers.higherKey(chordAddress);
            if (succ == null) {
                succ = alivePeers.firstKey();
            }
            successor.put(chordAddress, succ);
            predecessor.put(succ, chordAddress);

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

            WebResponse response = new WebResponse(dumpViewToHtml(), event, 1,
                    1);
            trigger(response, web);
        }
    };

    private void addPeerToView(OverlayAddress address, int clientWebPort,
            ChordNeighbors neighbors) {
        long now = System.currentTimeMillis();

        alivePeers.put(address, neighbors);
        deadPeers.remove(address);

        OverlayViewEntry entry = view.get(address);
        if (entry == null) {
            entry = new OverlayViewEntry(address, clientWebPort, now, now);
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
            ChordNeighbors neighbors = alivePeers.remove(address);
            deadPeers.put(address, neighbors);
            logger.debug("Removed peer {}", address);
        }
    }

    private String dumpViewToHtml() {
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

        printAlivePeers(sb);
        printDeadPeers(sb);

        sb.append("</body></html>");
        return sb.toString();
    }

    private void printAlivePeers(StringBuilder sb) {
        sb.append("<h2 align=\"center\" class=\"style2\">");
        sb.append("View of Chord SON:</h2>");
        sb.append("<table width=\"1300\" border=\"1\" align=\"center\"><tr>");
        sb.append("<th class=\"style2\" width=\"50\" scope=\"col\">Count</th>");
        sb
                .append("<th class=\"style2\" width=\"100\" scope=\"col\">Predecessor</th>");
        sb
                .append("<th class=\"style2\" width=\"100\" scope=\"col\">Peer Id</th>");
        sb
                .append("<th class=\"style2\" width=\"100\" scope=\"col\">Successor</th>");
        sb
                .append("<th class=\"style2\" width=\"350\" scope=\"col\">Successor List</th>");
        sb
                .append("<th class=\"style2\" width=\"400\" scope=\"col\">Finger List</th>");
        sb.append("<th class=\"style2\" width=\"100\" scope=\"col\">Age</th>");
        sb
                .append("<th class=\"style2\" width=\"100\" scope=\"col\">Freshness</th></tr>");

        int count = 1;

        for (OverlayAddress address : alivePeers.keySet()) {
            ChordNeighbors neighbors = alivePeers.get(address);

            OverlayAddress pred = neighbors.getPredecessorPeer();
            OverlayAddress succ = neighbors.getSuccessorPeer();
            OverlayAddress realPred = predecessor.get(address);
            OverlayAddress realSucc = successor.get(address);
            List<ChordAddress> succList = neighbors.getSuccessorList();
            FingerTableView fingerTableView = neighbors.getFingerTable();

            sb.append("<tr>");
            sb.append("<td><div align=\"center\">").append(count++);
            sb.append("</div></td>");

            // print predecessor
            if (pred != null) {
                if (pred.equals(realPred)) {
                    sb.append("<td bgcolor=\"#99FF99\"><div align=\"center\">");
                    appendPeerLink(sb, pred);
                } else {
                    sb.append("<td bgcolor=\"#FFFF66\"><div align=\"center\">");
                    appendPeerLink(sb, pred);
                    sb.append(" (<b>");
                    appendPeerLink(sb, realPred);
                    sb.append("</b>)");
                }
            } else {
                sb.append("<td bgcolor=\"#FFCCFF\"><div align=\"center\">");
                sb.append("NIL");
                sb.append(" (<b>");
                appendPeerLink(sb, realPred);
                sb.append("</b>)");
            }

            // print peer address
            sb
                    .append("</div></td><td bgcolor=\"#99CCFF\"><div align=\"center\">");
            appendPeerLink(sb, address);
            sb.append("</div></td>");

            // print successor
            if (succ != null) {
                if (succ.equals(realSucc)) {
                    sb.append("<td bgcolor=\"#99FF99\"><div align=\"center\">");
                    appendPeerLink(sb, succ);
                } else {
                    sb.append("<td bgcolor=\"#FFFF66\"><div align=\"center\">");
                    appendPeerLink(sb, succ);
                    sb.append(" (<b>");
                    appendPeerLink(sb, realSucc);
                    sb.append("</b>)");
                }
            } else {
                sb.append("<td bgcolor=\"#FFCCFF\"><div align=\"center\">");
                sb.append("NIL");
                sb.append(" (<b>");
                appendPeerLink(sb, realSucc);
                sb.append("</b>)");
            }
            sb.append("</div></td>");

            // print successor list
            if (succList != null) {
                sb.append("<td><div align=\"left\">");
                sb.append("[");
                Iterator<ChordAddress> iter = succList.iterator();
                while (iter.hasNext()) {
                    appendPeerLink(sb, iter.next());
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

            // print finger list
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
            sb.append("<h2 align=\"center\" class=\"style2\">Dead peers:</h2>");
            sb
                    .append("<table width=\"1300\" border=\"0\" align=\"center\"><tr>");
            sb
                    .append("<th class=\"style2\" width=\"50\" scope=\"col\">Count</th>");
            sb
                    .append("<th class=\"style2\" width=\"100\" scope=\"col\">Predecessor</th>");
            sb
                    .append("<th class=\"style2\" width=\"100\" scope=\"col\">Peer Id</th>");
            sb
                    .append("<th class=\"style2\" width=\"100\" scope=\"col\">Successor</th>");
            sb
                    .append("<th class=\"style2\" width=\"350\" scope=\"col\">Successor List</th>");
            sb
                    .append("<th class=\"style2\" width=\"400\" scope=\"col\">Finger List</th>");
            sb
                    .append("<th class=\"style2\" width=\"100\" scope=\"col\">Lifetime</th>");
            sb
                    .append("<th class=\"style2\" width=\"100\" scope=\"col\">Dead for</th></tr>");

            LinkedList<OverlayAddress> peers = new LinkedList<OverlayAddress>(
                    deadPeers.descendingKeySet());

            count = 1;

            for (OverlayAddress address : peers) {
                ChordNeighbors neighbors = deadPeers.get(address);

                OverlayAddress pred = neighbors.getPredecessorPeer();
                OverlayAddress succ = neighbors.getSuccessorPeer();
                OverlayAddress realPred = predecessor.get(address);
                OverlayAddress realSucc = successor.get(address);
                List<ChordAddress> succList = neighbors.getSuccessorList();
                FingerTableView fingerTableView = neighbors.getFingerTable();

                sb.append("<tr>");

                sb.append("<tr>");
                sb.append("<td><div align=\"center\">").append(count++);
                sb.append("</div></td>");

                // print predecessor
                if (pred != null) {
                    if (pred.equals(realPred)) {
                        sb
                                .append("<td bgcolor=\"#99FF99\"><div align=\"center\">");
                        appendPeerLink(sb, pred);
                    } else {
                        sb
                                .append("<td bgcolor=\"#FFFF66\"><div align=\"center\">");
                        appendPeerLink(sb, pred);
                        sb.append(" (<b>");
                        appendPeerLink(sb, realPred);
                        sb.append("</b>)");
                    }
                } else {
                    sb.append("<td bgcolor=\"#FFCCFF\"><div align=\"center\">");
                    sb.append("NIL");
                    sb.append(" (<b>");
                    appendPeerLink(sb, realPred);
                    sb.append("</b>)");
                }

                // print peer address
                sb
                        .append("</div></td><td bgcolor=\"#99CCFF\"><div align=\"center\">");
                appendPeerLink(sb, address);
                sb.append("</div></td>");

                // print successor
                if (succ != null) {
                    if (succ.equals(realSucc)) {
                        sb
                                .append("<td bgcolor=\"#99FF99\"><div align=\"center\">");
                        appendPeerLink(sb, succ);
                    } else {
                        sb
                                .append("<td bgcolor=\"#FFFF66\"><div align=\"center\">");
                        appendPeerLink(sb, succ);
                        sb.append(" (<b>");
                        appendPeerLink(sb, realSucc);
                        sb.append("</b>)");
                    }
                } else {
                    sb.append("<td bgcolor=\"#FFCCFF\"><div align=\"center\">");
                    sb.append("NIL");
                    sb.append(" (<b>");
                    appendPeerLink(sb, realSucc);
                    sb.append("</b>)");
                }
                sb.append("</div></td>");

                // print successor list
                if (succList != null) {
                    sb.append("<td><div align=\"left\">");
                    sb.append("[");
                    Iterator<ChordAddress> iter = succList.iterator();
                    while (iter.hasNext()) {
                        appendPeerLink(sb, iter.next());
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

                // print fingers
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
        OverlayViewEntry viewEntry = view.get(address);
        int wp = 8080;
        if (viewEntry != null) {
            wp = viewEntry.getClientWebPort();
        }
        sb.append("<a href=\"http://");
        sb.append(address.getPeerAddress().getIp().getHostAddress());
        sb.append(":").append(wp).append("/");
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
