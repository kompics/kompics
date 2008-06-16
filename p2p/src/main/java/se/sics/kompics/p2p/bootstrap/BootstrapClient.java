package se.sics.kompics.p2p.bootstrap;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Properties;

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
import se.sics.kompics.api.annotation.MayTriggerEventTypes;
import se.sics.kompics.network.Address;
import se.sics.kompics.p2p.bootstrap.events.BootstrapCacheReset;
import se.sics.kompics.p2p.bootstrap.events.BootstrapCompleted;
import se.sics.kompics.p2p.bootstrap.events.BootstrapRequest;
import se.sics.kompics.p2p.bootstrap.events.BootstrapResponse;
import se.sics.kompics.p2p.bootstrap.events.CacheAddPeerRequest;
import se.sics.kompics.p2p.bootstrap.events.CacheGetPeersRequest;
import se.sics.kompics.p2p.bootstrap.events.CacheGetPeersResponse;
import se.sics.kompics.p2p.bootstrap.events.CacheResetRequest;
import se.sics.kompics.p2p.bootstrap.events.ClientRefreshPeer;
import se.sics.kompics.p2p.network.events.PerfectNetworkDeliverEvent;
import se.sics.kompics.p2p.network.events.PerfectNetworkSendEvent;
import se.sics.kompics.timer.TimerHandler;
import se.sics.kompics.timer.events.SetTimerEvent;

/**
 * The <code>BootstrapClient</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public class BootstrapClient {

	private Logger logger;

	private final Component component;

	// BootstrapClient channels
	private Channel requestChannel, responseChannel;

	// PerfectNetwork send channel
	private Channel pnSendChannel;

	private Channel timerSignalChannel;

	private Address bootstrapServerAddress;

	private Address localPeerAddress;

	private TimerHandler timerHandler;

	private long refreshPeriod;

	public BootstrapClient(Component component) {
		super();
		this.component = component;
	}

	@ComponentCreateMethod
	public void create(Channel requestChannel, Channel responseChannel) {
		this.requestChannel = requestChannel;
		this.responseChannel = responseChannel;

		// use shared timer component
		ComponentMembrane timerMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.Timer");
		Channel timerSetChannel = timerMembrane.getChannel(SetTimerEvent.class);
		timerSignalChannel = component.createChannel(ClientRefreshPeer.class);

		// use shared PerfectNetwork component
		ComponentMembrane pnMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.p2p.network.PerfectNetwork");
		pnSendChannel = pnMembrane.getChannel(PerfectNetworkSendEvent.class);
		Channel pnDeliverChannel = pnMembrane
				.getChannel(PerfectNetworkDeliverEvent.class);

		component.subscribe(this.requestChannel, "handleBootstrapRequest");
		component.subscribe(this.requestChannel, "handleBootstrapCompleted");
		component.subscribe(this.requestChannel, "handleBootstrapCacheReset");
		component.subscribe(pnDeliverChannel, "handleCacheGetPeersResponse");

		this.timerHandler = new TimerHandler(component, timerSetChannel);
		component.subscribe(timerSignalChannel, "handleClientRefreshPeer");
	}

	@ComponentShareMethod
	public ComponentMembrane share(String name) {
		HashMap<Class<? extends Event>, Channel> map = new HashMap<Class<? extends Event>, Channel>();
		map.put(BootstrapRequest.class, requestChannel);
		map.put(BootstrapResponse.class, responseChannel);
		ComponentMembrane membrane = new ComponentMembrane(component, map);
		return component.registerSharedComponentMembrane(name, membrane);
	}

	@ComponentInitializeMethod("bootstrap.properties")
	public void init(Properties properties, Address peerAddress)
			throws UnknownHostException {
		InetAddress ip = InetAddress.getByName(properties
				.getProperty("bootstrap.server.ip"));
		int port = Integer.parseInt(properties
				.getProperty("bootstrap.server.port"));

		refreshPeriod = 1000 * Long.parseLong(properties
				.getProperty("client.refresh.period"));

		bootstrapServerAddress = new Address(ip, port, BigInteger.ZERO);
		localPeerAddress = peerAddress;

		logger = LoggerFactory.getLogger(BootstrapClient.class.getName() + "@"
				+ peerAddress.getId());
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(PerfectNetworkSendEvent.class)
	public void handleBootstrapRequest(BootstrapRequest event) {
		CacheGetPeersRequest request = new CacheGetPeersRequest(event
				.getPeersMax());

		PerfectNetworkSendEvent sendEvent = new PerfectNetworkSendEvent(
				request, bootstrapServerAddress);

		logger.debug("Sending GetPeersRequest");
		component.triggerEvent(sendEvent, pnSendChannel);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(BootstrapResponse.class)
	public void handleCacheGetPeersResponse(CacheGetPeersResponse event) {
		BootstrapResponse response = new BootstrapResponse(event.getPeers());

		logger.debug("Received GetPeersResponse");
		component.triggerEvent(response, responseChannel);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes( { PerfectNetworkSendEvent.class, SetTimerEvent.class })
	public void handleBootstrapCompleted(BootstrapCompleted event) {
		CacheAddPeerRequest request = new CacheAddPeerRequest(localPeerAddress);

		PerfectNetworkSendEvent sendEvent = new PerfectNetworkSendEvent(
				request, bootstrapServerAddress);
		component.triggerEvent(sendEvent, pnSendChannel);

		// set refresh timer
		ClientRefreshPeer refreshEvent = new ClientRefreshPeer(localPeerAddress);
		timerHandler.setTimer(refreshEvent, timerSignalChannel, refreshPeriod);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes( { PerfectNetworkSendEvent.class, SetTimerEvent.class })
	public void handleClientRefreshPeer(ClientRefreshPeer event) {
		CacheAddPeerRequest request = new CacheAddPeerRequest(localPeerAddress);

		PerfectNetworkSendEvent sendEvent = new PerfectNetworkSendEvent(
				request, bootstrapServerAddress);
		component.triggerEvent(sendEvent, pnSendChannel);

		// reset refresh timer
		timerHandler.setTimer(event, timerSignalChannel, refreshPeriod);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(PerfectNetworkSendEvent.class)
	public void handleBootstrapCacheReset(BootstrapCacheReset event) {
		CacheResetRequest request = new CacheResetRequest(localPeerAddress);

		PerfectNetworkSendEvent sendEvent = new PerfectNetworkSendEvent(
				request, bootstrapServerAddress);

		component.triggerEvent(sendEvent, pnSendChannel);
	}
}
