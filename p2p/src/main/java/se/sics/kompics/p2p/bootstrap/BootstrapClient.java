package se.sics.kompics.p2p.bootstrap;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.ComponentMembrane;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
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
import se.sics.kompics.p2p.network.events.PerfectNetworkDeliverEvent;
import se.sics.kompics.p2p.network.events.PerfectNetworkSendEvent;

/**
 * The <code>BootstrapClient</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public class BootstrapClient {

	private final Component component;

	// BootstrapClient channels
	private Channel requestChannel, responseChannel;

	// PerfectNetwork send channel
	private Channel pnSendChannel;

	private Address bootstrapServerAddress;

	private Address localPeerAddress;

	public BootstrapClient(Component component) {
		super();
		this.component = component;
	}

	@ComponentCreateMethod
	public void create(Channel requestChannel, Channel responseChannel) {
		this.requestChannel = requestChannel;
		this.responseChannel = responseChannel;

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
	}

	@ComponentInitializeMethod("bootstrap.properties")
	public void init(Properties properties, Address peerAddress)
			throws UnknownHostException {
		InetAddress ip = InetAddress.getByName(properties
				.getProperty("server.ip"));
		int port = Integer.parseInt(properties.getProperty("server.port"));

		bootstrapServerAddress = new Address(ip, port, BigInteger.ZERO);
		localPeerAddress = peerAddress;
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(PerfectNetworkSendEvent.class)
	public void handleBootstrapRequest(BootstrapRequest event) {
		CacheGetPeersRequest request = new CacheGetPeersRequest(event
				.getPeersMax());

		PerfectNetworkSendEvent sendEvent = new PerfectNetworkSendEvent(
				request, bootstrapServerAddress);

		component.triggerEvent(sendEvent, pnSendChannel);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(BootstrapResponse.class)
	public void handleCacheGetPeersResponse(CacheGetPeersResponse event) {
		BootstrapResponse response = new BootstrapResponse(event.getPeers());
		component.triggerEvent(response, responseChannel);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(PerfectNetworkSendEvent.class)
	public void handleBootstrapCompleted(BootstrapCompleted event) {
		CacheAddPeerRequest request = new CacheAddPeerRequest(localPeerAddress);

		PerfectNetworkSendEvent sendEvent = new PerfectNetworkSendEvent(
				request, bootstrapServerAddress);

		component.triggerEvent(sendEvent, pnSendChannel);
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
