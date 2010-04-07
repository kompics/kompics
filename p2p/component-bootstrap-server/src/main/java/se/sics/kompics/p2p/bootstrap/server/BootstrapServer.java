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
package se.sics.kompics.p2p.bootstrap.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.p2p.bootstrap.PeerEntry;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.web.Web;
import se.sics.kompics.web.WebRequest;
import se.sics.kompics.web.WebResponse;

/**
 * The <code>BootstrapServer</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class BootstrapServer extends ComponentDefinition
{

    Positive<Network> network = positive(Network.class);
    Positive<Timer> timer = positive(Timer.class);
    Negative<Web> web = negative(Web.class);
    private static final Logger logger = LoggerFactory.getLogger(BootstrapServer.class);
    private final HashMap<String, HashSet<UUID>> outstandingTimeouts;
    private final HashMap<String, HashMap<Address, CacheEntry>> cache;
    private final HashMap<String, Long> cacheEpoch;
    private long evictAfter;
    private Address self;
    private String webAddress;
    private int webPort;

    public BootstrapServer()
    {
        this.cache = new HashMap<String, HashMap<Address, CacheEntry>>();
        this.cacheEpoch = new HashMap<String, Long>();

        outstandingTimeouts = new HashMap<String, HashSet<UUID>>();

        subscribe(handleInit, control);

        subscribe(handleWebRequest, web);
        subscribe(handleCacheResetRequest, network);
        subscribe(handleCacheAddPeerRequest, network);
        subscribe(handleCacheGetPeersRequest, network);

        subscribe(handleCacheEvictPeer, timer);
    }

    private Handler<BootstrapServerInit> handleInit = new Handler<BootstrapServerInit>()
    {

        public void handle(BootstrapServerInit event)
        {
            evictAfter = event.getConfiguration().getCacheEvictAfter();
            self = event.getConfiguration().getBootstrapServerAddress();

            webPort = event.getConfiguration().getServerWebPort();
            webAddress = "http://" + self.getIp().getHostAddress() + ":"
                    + webPort + "/" + self.getId() + "/";

            logger.debug("Started");
            dumpCacheToLog();
        }

    };
    private Handler<CacheResetRequest> handleCacheResetRequest = new Handler<CacheResetRequest>()
    {

        public void handle(CacheResetRequest event)
        {
            resetCache(event.getOverlay());
        }

    };
    private Handler<CacheAddPeerRequest> handleCacheAddPeerRequest = new Handler<CacheAddPeerRequest>()
    {

        public void handle(CacheAddPeerRequest event)
        {
            addPeerToCache(event.getPeerAddress(), event.getPeerOverlays());
            dumpCacheToLog();
        }

    };
    private Handler<CacheEvictPeer> handleCacheEvictPeer = new Handler<CacheEvictPeer>()
    {

        public void handle(CacheEvictPeer event)
        {
            // only evict if it was not refreshed in the meantime
            // which means the timer is not anymore outstanding
            HashSet<UUID> overlayEvictionTimoutIds = outstandingTimeouts.get(event.getOverlay());
            if (overlayEvictionTimoutIds != null)
            {
                if (overlayEvictionTimoutIds.contains(event.getTimeoutId()))
                {
                    removePeerFromCache(event.getPeerAddress(), event.getOverlay(), event.getEpoch());
                    overlayEvictionTimoutIds.remove(event.getTimeoutId());
                }
            }
            dumpCacheToLog();
        }

    };
    private Handler<CacheGetPeersRequest> handleCacheGetPeersRequest = new Handler<CacheGetPeersRequest>()
    {

        public void handle(CacheGetPeersRequest event)
        {
            int peersMax = event.getPeersMax();
            HashSet<PeerEntry> peers = new HashSet<PeerEntry>();
            long now = System.currentTimeMillis();
            String overlay = event.getOverlay();

            HashMap<Address, CacheEntry> overlayCache = cache.get(overlay);

            if (overlayCache != null)
            {
                Collection<CacheEntry> entries = overlayCache.values();
                ArrayList<CacheEntry> sorted = new ArrayList<CacheEntry>(
                        entries);

                // get the most recent up to peersMax entries
                Collections.sort(sorted);
                for (CacheEntry cacheEntry : sorted)
                {
                    PeerEntry peerEntry = new PeerEntry(overlay, cacheEntry.getOverlayAddress(), cacheEntry.getPeerWebPort(),
                            cacheEntry.getPeerAddress(), now
                            - cacheEntry.getAddedAt(), now
                            - cacheEntry.getRefreshedAt());
                    peers.add(peerEntry);
                    peersMax--;

                    if (peersMax == 0)
                    {
                        break;
                    }
                }
            }

            CacheGetPeersResponse response = new CacheGetPeersResponse(peers,
                    event.getRequestId(), self, event.getSource());
            trigger(response, network);

            logger.debug("Responded with {} peers to peer {}", peers.size(),
                    event.getSource());
        }

    };
    private Handler<WebRequest> handleWebRequest = new Handler<WebRequest>()
    {

        public void handle(WebRequest event)
        {
            logger.debug("Handling WebRequest");

            WebResponse response = new WebResponse(dumpCacheToHtml(event.getTarget()), event, 1, 1);
            trigger(response, web);
        }

    };

    private final void resetCache(String overlay)
    {
        // cancel all eviction timers for this overlay
        HashSet<UUID> overlayEvictionTimoutIds = outstandingTimeouts.get(overlay);
        if (overlayEvictionTimoutIds != null)
        {
            for (UUID timoutId : overlayEvictionTimoutIds)
            {
                CancelTimeout ct = new CancelTimeout(timoutId);
                trigger(ct, timer);
            }
            overlayEvictionTimoutIds.clear();
        }

        // reset cache
        HashMap<Address, CacheEntry> overlayCache = cache.get(overlay);
        if (overlayCache != null)
        {
            overlayCache.clear();
            Long epoch = cacheEpoch.get(overlay);
            cacheEpoch.put(overlay, 1 + epoch);
        }
        else
        {
            cache.put(overlay, new HashMap<Address, CacheEntry>());
            cacheEpoch.put(overlay, 1L);
            outstandingTimeouts.put(overlay, new HashSet<UUID>());
        }
        logger.debug("Cleared cache for " + overlay);
        dumpCacheToLog();
    }

    private final void addPeerToCache(Address address, Set<PeerEntry> overlays)
    {
        if (address != null)
        {
            long now = System.currentTimeMillis();

            for (PeerEntry peerEntry : overlays)
            {
                String overlay = peerEntry.getOverlay();

                HashMap<Address, CacheEntry> overlayCache = cache.get(overlay);
                if (overlayCache == null)
                {
                    overlayCache = new HashMap<Address, CacheEntry>();
                    cache.put(overlay, overlayCache);
                    cacheEpoch.put(overlay, 1L);
                    outstandingTimeouts.put(overlay, new HashSet<UUID>());
                }
                CacheEntry entry = overlayCache.get(address);
                if (entry == null)
                {
                    // add a new entry
                    entry = new CacheEntry(address, overlay, peerEntry.getOverlayAddress(), peerEntry.getPeerWebPort(),
                            now, now);
                    overlayCache.put(address, entry);

                    // set a new eviction timeout
                    ScheduleTimeout st = new ScheduleTimeout(evictAfter);
                    st.setTimeoutEvent(new CacheEvictPeer(st, address, overlay,
                            cacheEpoch.get(overlay)));

                    UUID evictionTimerId = st.getTimeoutEvent().getTimeoutId();
                    entry.setEvictionTimerId(evictionTimerId);
                    outstandingTimeouts.get(overlay).add(evictionTimerId);
                    trigger(st, timer);

                    logger.debug("Added peer {}", address);
                }
                else
                {
                    // update an existing entry
                    entry.setRefreshedAt(now);

                    // cancel an old eviction timeout, if it exists
                    UUID oldTimeoutId = entry.getEvictionTimerId();
                    if (oldTimeoutId != null)
                    {
                        trigger(new CancelTimeout(oldTimeoutId), timer);
                        outstandingTimeouts.get(overlay).remove(oldTimeoutId);
                    }
                    // set a new eviction timeout
                    ScheduleTimeout st = new ScheduleTimeout(evictAfter);
                    st.setTimeoutEvent(new CacheEvictPeer(st, address, overlay,
                            cacheEpoch.get(overlay)));

                    UUID evictionTimerId = st.getTimeoutEvent().getTimeoutId();
                    entry.setEvictionTimerId(evictionTimerId);
                    outstandingTimeouts.get(overlay).add(evictionTimerId);
                    trigger(st, timer);

                    logger.debug("Refreshed peer {}", address);
                }
            }
        }
    }

    private final void removePeerFromCache(Address address, String overlay,
            long epoch)
    {
        long thisEpoch = cacheEpoch.get(overlay);
        if (address != null && epoch == thisEpoch)
        {
            cache.get(overlay).remove(address);

            logger.debug("Removed peer {}", address);
        }
    }

    private void dumpCacheToLog()
    {
        for (String overlay : cache.keySet())
        {
            dumpCacheToLog(overlay);
        }
    }

    private void dumpCacheToLog(String overlay)
    {
        logger.info("Overlay {} now contains:", overlay);
        logger.info("Age=====Freshness==Peer address=========================");
        long now = System.currentTimeMillis();

        Collection<CacheEntry> entries = cache.get(overlay).values();
        ArrayList<CacheEntry> sorted = new ArrayList<CacheEntry>(entries);

        // get all peers in most recently added order
        Collections.sort(sorted);
        for (CacheEntry cacheEntry : sorted)
        {
            logger.info("{}\t{}\t  {}", new Object[]
                    {
                        durationToString(now - cacheEntry.getAddedAt()),
                        durationToString(now - cacheEntry.getRefreshedAt()),
                        cacheEntry.getPeerAddress()
                    });
        }

        logger.info("========================================================");
    }

    private String dumpCacheToHtml(String overlay)
    {
        if (!cache.containsKey(overlay))
        {
            StringBuilder sb = new StringBuilder(
                    "<!DOCTYPE html PUBLIC \"-//W3C");
            sb.append("//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR");
            sb.append("/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http:");
            sb.append("//www.w3.org/1999/xhtml\"><head><meta http-equiv=\"Conten");
            sb.append("t-Type\" content=\"text/html; charset=utf-8\" />");
            sb.append("<title>Kompics P2P Bootstrap Server</title>");
            sb.append("<style type=\"text/css\"><!--.style2 {font-family: ");
            sb.append("Arial, Helvetica, sans-serif; color: #0099FF;}--></style>");
            sb.append("</head><body><h2 align=\"center\" class=\"style2\">");
            sb.append("Kompics P2P Bootstrap Overlays:</h2><br>");
            for (String o : cache.keySet())
            {
                sb.append("<a href=\"" + webAddress + o + "\">" + o + "</a>").append("<br>");
            }
            sb.append("</body></html>");
            return sb.toString();
        }

        StringBuilder sb = new StringBuilder("<!DOCTYPE html PUBLIC \"-//W3C");
        sb.append("//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR");
        sb.append("/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http:");
        sb.append("//www.w3.org/1999/xhtml\"><head><meta http-equiv=\"Conten");
        sb.append("t-Type\" content=\"text/html; charset=utf-8\" />");
        sb.append("<title>Kompics P2P Bootstrap Server</title>");
        sb.append("<style type=\"text/css\"><!--.style2 {font-family: ");
        sb.append("Arial, Helvetica, sans-serif; color: #0099FF;}--></style>");
        sb.append("</head><body><h2 align=\"center\" class=\"style2\">");
        sb.append("Kompics P2P Bootstrap Cache for " + overlay + "</h2>");
        sb.append("<table width=\"1000\" border=\"0\" align=\"center\"><tr>");
        sb.append("<th class=\"style2\" width=\"100\" scope=\"col\">Count</th>");
        sb.append("<th class=\"style2\" width=\"80\" scope=\"col\">Age</th>");
        sb.append("<th class=\"style2\" width=\"120\" scope=\"col\">Freshness</th>");
        sb.append("<th class=\"style2\" width=\"300\" scope=\"col\">" + overlay
                + " id</th>");
        sb.append("<th class=\"style2\" width=\"700\" scope=\"col\">Peer address</th></tr>");
        long now = System.currentTimeMillis();

        Collection<CacheEntry> entries = cache.get(overlay).values();
        ArrayList<CacheEntry> sorted = new ArrayList<CacheEntry>(entries);

        // get all peers in most recently added order
        Collections.sort(sorted);

        int count = 1;

        for (CacheEntry cacheEntry : sorted)
        {
            sb.append("<tr>");
            sb.append("<td><div align=\"center\">").append(count++);
            sb.append("</div></td>");
            sb.append("<td><div align=\"right\">");
            sb.append(durationToString(now - cacheEntry.getAddedAt()));
            sb.append("</div></td><td><div align=\"right\">");
            sb.append(durationToString(now - cacheEntry.getRefreshedAt()));
            sb.append("</div></td><td><div align=\"center\">");
            String webAddress = "http://"
                    + cacheEntry.getPeerAddress().getIp().getHostAddress()
                    + ":" + cacheEntry.getPeerWebPort() + "/"
                    + cacheEntry.getPeerAddress().getId() + "/";
            sb.append("<a href=\"").append(webAddress).append("\">");
            sb.append(cacheEntry.getOverlayAddress().toString()).append("</a>");
            sb.append("</div></td><td><div align=\"left\">");
            sb.append(cacheEntry.getPeerAddress());
            sb.append("</div></td>");
            sb.append("</tr>");
        }

        sb.append("</table></body></html>");
        return sb.toString();
    }

    private String durationToString(long duration)
    {
        StringBuilder sb = new StringBuilder();

        // get duration in seconds
        duration /= 1000;

        int s = 0, m = 0, h = 0, d = 0, y = 0;
        s = (int) (duration % 60);
        // get duration in minutes
        duration /= 60;
        if (duration > 0)
        {
            m = (int) (duration % 60);
            // get duration in hours
            duration /= 60;
            if (duration > 0)
            {
                h = (int) (duration % 24);
                // get duration in days
                duration /= 24;
                if (duration > 0)
                {
                    d = (int) (duration % 365);
                    // get duration in years
                    y = (int) (duration / 365);
                }
            }
        }

        boolean printed = false;

        if (y > 0)
        {
            sb.append(y).append("y");
            printed = true;
        }
        if (d > 0)
        {
            sb.append(d).append("d");
            printed = true;
        }
        if (h > 0)
        {
            sb.append(h).append("h");
            printed = true;
        }
        if (m > 0)
        {
            sb.append(m).append("m");
            printed = true;
        }
        if (s > 0 || printed == false)
        {
            sb.append(s).append("s");
        }

        return sb.toString();
    }

}
