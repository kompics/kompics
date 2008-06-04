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
import se.sics.kompics.timer.events.SetTimerEvent;
import se.sics.kompics.timer.events.TimerSignalEvent;

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
		Channel timerSetChannel = timerMembrane.getChannel(SetTimerEvent.class);
		timerSignalChannel = timerMembrane.getChannel(TimerSignalEvent.class);

		// use shared PerfectNetwork component
		ComponentMembrane pnMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.p2p.network.PerfectNetwork");
		pnSendChannel = pnMembrane.getChannel(PerfectNetworkSendEvent.class);
		Channel pnDeliverChannel = pnMembrane
				.getChannel(PerfectNetworkDeliverEvent.class);

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
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(SetTimerEvent.class)
	public void handleCacheResetRequest(CacheResetRequest event) {
		// cancel all eviction timers
		timerHandler.cancelAllOutstandingTimers();

		// reset cache
		cache.clear();
		mostRecentEntriesFirst.clear();
		cacheEpoch++;

		logger.debug("Cleared cache");
		addPeerToCache(event.getPeerAddress());
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(SetTimerEvent.class)
	public void handleCacheAddPeerRequest(CacheAddPeerRequest event) {
		addPeerToCache(event.getPeerAddress());
	}

	@EventHandlerMethod
	public void handleCacheEvictPeer(CacheEvictPeer event) {
		// only evict if it was not refreshed in the meantime
		// which means the timer is not anymore outstanding
		if (timerHandler.isOustandingTimer(event.getTimerId())) {
			removePeerFromCache(event.getPeerAddress(), event.getEpoch());
		}
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(CacheGetPeersResponse.class)
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

		CacheGetPeersResponse response = new CacheGetPeersResponse(peers);
		component.triggerEvent(response, pnSendChannel);

		logger.debug("Responded with {} peers", peers.size());
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
}
