package se.sics.kompics.p2p.bootstrap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.ComponentMembrane;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.api.annotation.EventHandlerMethod;
import se.sics.kompics.api.annotation.MayTriggerEventTypes;
import se.sics.kompics.network.Address;
import se.sics.kompics.p2p.bootstrap.events.CacheAddPeerRequest;
import se.sics.kompics.p2p.bootstrap.events.CacheEvictPeer;
import se.sics.kompics.p2p.bootstrap.events.CacheGetPeersRequest;
import se.sics.kompics.p2p.bootstrap.events.CacheGetPeersResponse;
import se.sics.kompics.p2p.bootstrap.events.CacheResetRequest;
import se.sics.kompics.p2p.bootstrap.events.PeerEntry;
import se.sics.kompics.p2p.bootstrap.events.StartBootstrapServer;
import se.sics.kompics.p2p.network.events.PerfectNetworkDeliverEvent;
import se.sics.kompics.p2p.network.events.PerfectNetworkSendEvent;
import se.sics.kompics.timer.TimerHandler;
import se.sics.kompics.timer.events.Alarm;
import se.sics.kompics.timer.events.SetAlarm;
import se.sics.kompics.web.events.WebRequest;
import se.sics.kompics.web.events.WebResponse;

/**
 * The <code>BootstrapServer</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public class BootstrapServer {

	private static final Logger logger = LoggerFactory
			.getLogger(BootstrapServer.class);

	private final Component component;

	private Channel pnSendChannel, timerSignalChannel;

	private Channel webRequestChannel, webResponseChannel;

	private final HashMap<Address, CacheEntry> cache;

	private final LinkedList<Address> mostRecentEntriesFirst;

	private TimerHandler timerHandler;

	private long evictAfter;

	private long cacheEpoch;

	public BootstrapServer(Component component) {
		this.component = component;
		this.cache = new HashMap<Address, CacheEntry>();
		this.mostRecentEntriesFirst = new LinkedList<Address>();
		this.cacheEpoch = 0;
	}

	@ComponentCreateMethod
	public void create(Channel startChannel) {
		// use shared timer component
		ComponentMembrane timerMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.Timer");
		Channel timerSetChannel = timerMembrane.getChannelIn(SetAlarm.class);
		timerSignalChannel = timerMembrane.getChannelOut(Alarm.class);

		// use shared PerfectNetwork component
		ComponentMembrane pnMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.p2p.network.PerfectNetwork");
		pnSendChannel = pnMembrane.getChannelIn(PerfectNetworkSendEvent.class);
		Channel pnDeliverChannel = pnMembrane
				.getChannelOut(PerfectNetworkDeliverEvent.class);

		// use the shared WebComponent
		ComponentMembrane webMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.Web");
		webRequestChannel = webMembrane.getChannelOut(WebRequest.class);
		webResponseChannel = webMembrane.getChannelIn(WebResponse.class);

		component.subscribe(webRequestChannel, "handleWebRequest");

		component.subscribe(startChannel, "handleStartEvent");

		component.subscribe(pnDeliverChannel, "handleCacheResetRequest");
		component.subscribe(pnDeliverChannel, "handleCacheAddPeerRequest");
		component.subscribe(pnDeliverChannel, "handleCacheGetPeersRequest");

		this.timerHandler = new TimerHandler(component, timerSetChannel);
		component.subscribe(timerSignalChannel, "handleCacheEvictPeer");
	}

	@ComponentInitializeMethod
	public void init(long evictAfterSeconds) {
		this.evictAfter = evictAfterSeconds * 1000;
	}

	@EventHandlerMethod
	public void handleStartEvent(StartBootstrapServer event) {
		logger.debug("Started");

		dumpCacheToLog();
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(SetAlarm.class)
	public void handleCacheResetRequest(CacheResetRequest event) {
		// cancel all eviction timers
		timerHandler.cancelAllOutstandingTimers();

		// reset cache
		cache.clear();
		mostRecentEntriesFirst.clear();
		cacheEpoch++;

		logger.debug("Cleared cache");
		addPeerToCache(event.getPeerAddress());

		dumpCacheToLog();
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(SetAlarm.class)
	public void handleCacheAddPeerRequest(CacheAddPeerRequest event) {
		addPeerToCache(event.getPeerAddress());

		dumpCacheToLog();
	}

	@EventHandlerMethod
	public void handleCacheEvictPeer(CacheEvictPeer event) {
		// only evict if it was not refreshed in the meantime
		// which means the timer is not anymore outstanding
		if (timerHandler.isOustandingTimer(event.getTimerId())) {
			removePeerFromCache(event.getPeerAddress(), event.getEpoch());
		}

		dumpCacheToLog();
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(PerfectNetworkSendEvent.class)
	public void handleCacheGetPeersRequest(CacheGetPeersRequest event) {
		int peersMax = event.getPeersMax();
		HashSet<PeerEntry> peers = new HashSet<PeerEntry>();
		long now = System.currentTimeMillis();

		// get the most recent up to peersMax entries
		Iterator<Address> iterator = mostRecentEntriesFirst
				.descendingIterator();
		while (iterator.hasNext()) {
			Address address = iterator.next();
			CacheEntry cacheEntry = cache.get(address);
			PeerEntry peerEntry = new PeerEntry(address, now
					- cacheEntry.getAddedAt(), now
					- cacheEntry.getRefreshedAt());
			peers.add(peerEntry);
			peersMax--;

			if (peersMax == 0)
				break;
		}

		CacheGetPeersResponse response = new CacheGetPeersResponse(peers, event
				.getRequestId());
		PerfectNetworkSendEvent pnSendEvent = new PerfectNetworkSendEvent(
				response, event.getSource());
		component.triggerEvent(pnSendEvent, pnSendChannel);

		logger.debug("Responded with {} peers to peer {}", peers.size(), event
				.getSource());
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(WebResponse.class)
	public void handleWebRequest(WebRequest event) {
		logger.debug("Handling WebRequest");

		WebResponse response = new WebResponse(dumpCacheToHtml(), event, 1, 1);
		component.triggerEvent(response, webResponseChannel);
	}

	private void addPeerToCache(Address address) {
		if (address != null) {

			long now = System.currentTimeMillis();

			CacheEntry entry = cache.get(address);
			if (entry == null) {
				entry = new CacheEntry(address, now, now);
				cache.put(address, entry);
				mostRecentEntriesFirst.addLast(address);

				// set eviction timer
				long evictionTimerId = timerHandler.setTimer(
						new CacheEvictPeer(address, cacheEpoch),
						timerSignalChannel, evictAfter);
				entry.setEvictionTimerId(evictionTimerId);

				logger.debug("Added peer {}", address);
			} else {
				entry.setRefreshedAt(now);

				// reset eviction timer
				timerHandler.cancelTimer(entry.getEvictionTimerId());

				long evictionTimerId = timerHandler.setTimer(
						new CacheEvictPeer(address, cacheEpoch),
						timerSignalChannel, evictAfter);
				entry.setEvictionTimerId(evictionTimerId);

				// this is slow but not so important in the BootstrapServer
				mostRecentEntriesFirst.remove(address);
				mostRecentEntriesFirst.addLast(address);

				logger.debug("Refreshed peer {}", address);
			}
		}
	}

	private void removePeerFromCache(Address address, long epoch) {
		if (address != null && epoch == cacheEpoch) {
			cache.remove(address);
			mostRecentEntriesFirst.remove(address);

			logger.debug("Removed peer {}", address);
		}
	}

	private void dumpCacheToLog() {
		logger.info("Cache now contains:");
		logger
				.info("Age=====Freshness==Peer address==========================");
		long now = System.currentTimeMillis();

		// get all peers in most recently added order
		Iterator<Address> iterator = mostRecentEntriesFirst
				.descendingIterator();
		while (iterator.hasNext()) {
			Address address = iterator.next();
			CacheEntry cacheEntry = cache.get(address);
			logger.info("{}\t{}\t  {}", new Object[] {
					durationToString(now - cacheEntry.getAddedAt()),
					durationToString(now - cacheEntry.getRefreshedAt()),
					address });
		}

		logger.info("========================================================");
	}

	private String dumpCacheToHtml() {
		StringBuilder sb = new StringBuilder("<!DOCTYPE html PUBLIC \"-//W3C");
		sb.append("//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR");
		sb.append("/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http:");
		sb.append("//www.w3.org/1999/xhtml\"><head><meta http-equiv=\"Conten");
		sb.append("t-Type\" content=\"text/html; charset=utf-8\" />");
		sb.append("<title>Kompics P2P Bootstrap Server</title>");
		sb.append("<style type=\"text/css\"><!--.style2 {font-family: ");
		sb.append("Arial, Helvetica, sans-serif; color: #0099FF;}--></style>");
		sb.append("</head><body><h2 align=\"center\" class=\"style2\">");
		sb.append("Kompics P2P Bootstrap Cache contents:</h2>");
		sb.append("<table width=\"600\" border=\"0\" align=\"center\"><tr>");
		sb
				.append("<th class=\"style2\" width=\"100\" scope=\"col\">Count</th>");
		sb.append("<th class=\"style2\" width=\"80\" scope=\"col\">Age</th>");
		sb
				.append("<th class=\"style2\" width=\"120\" scope=\"col\">Freshness</th>");
		sb
				.append("<th class=\"style2\" width=\"300\" scope=\"col\">Peer address</th></tr>");
		long now = System.currentTimeMillis();

		// get all peers in most recently added order
		Iterator<Address> iterator = mostRecentEntriesFirst
				.descendingIterator();

		int count = 1;

		while (iterator.hasNext()) {
			Address address = iterator.next();
			CacheEntry cacheEntry = cache.get(address);

			sb.append("<tr>");
			sb.append("<td><div align=\"center\">").append(count++);
			sb.append("</div></td>");
			sb.append("<td><div align=\"right\">");
			sb.append(durationToString(now - cacheEntry.getAddedAt()));
			sb.append("</div></td><td><div align=\"right\">");
			sb.append(durationToString(now - cacheEntry.getRefreshedAt()));
			sb.append("</div></td><td><div align=\"center\">");
			String webAddress = address.getIp().toString() + ":"
					+ (address.getPort() - 21920) + "/" + address.getId();
			sb.append("<a href=\"http://").append(webAddress).append("/inf\">");
			sb.append(address).append("</a>");
			sb.append("</div></td>");
			sb.append("</tr>");
		}

		sb.append("</table></body></html>");
		return sb.toString();
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
