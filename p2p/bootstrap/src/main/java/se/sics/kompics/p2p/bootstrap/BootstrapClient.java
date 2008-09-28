package se.sics.kompics.p2p.bootstrap;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.ComponentMembrane;
import se.sics.kompics.api.EventHandler;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentShareMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.api.annotation.MayTriggerEventTypes;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.events.Message;
import se.sics.kompics.p2p.bootstrap.events.BootstrapCacheReset;
import se.sics.kompics.p2p.bootstrap.events.BootstrapCompleted;
import se.sics.kompics.p2p.bootstrap.events.BootstrapRequest;
import se.sics.kompics.p2p.bootstrap.events.BootstrapResponse;
import se.sics.kompics.p2p.bootstrap.events.CacheAddPeerRequest;
import se.sics.kompics.p2p.bootstrap.events.CacheGetPeersRequest;
import se.sics.kompics.p2p.bootstrap.events.CacheGetPeersResponse;
import se.sics.kompics.p2p.bootstrap.events.CacheResetRequest;
import se.sics.kompics.p2p.bootstrap.events.ClientRefreshPeer;
import se.sics.kompics.p2p.bootstrap.events.ClientRetryRequest;
import se.sics.kompics.timer.TimerHandler;
import se.sics.kompics.timer.events.ScheduleTimeout;

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

	private long retryPeriod;

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
		Channel timerSetChannel = timerMembrane
				.getChannelIn(ScheduleTimeout.class);
		timerSignalChannel = component.createChannel(ClientRefreshPeer.class,
				ClientRetryRequest.class);

		// use shared PerfectNetwork component
		ComponentMembrane pnMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.p2p.network.PerfectNetwork");
		pnSendChannel = pnMembrane.getChannelIn(Message.class);
		Channel pnDeliverChannel = pnMembrane.getChannelOut(Message.class);

		component.subscribe(this.requestChannel, handleBootstrapRequest);
		component.subscribe(this.requestChannel, handleBootstrapCompleted);
		component.subscribe(this.requestChannel, handleBootstrapCacheReset);
		component.subscribe(pnDeliverChannel, handleCacheGetPeersResponse);

		this.timerHandler = new TimerHandler(component, timerSetChannel);
		component.subscribe(timerSignalChannel, handleClientRefreshPeer);
		component.subscribe(timerSignalChannel, handleClientRetryRequest);
	}

	@ComponentShareMethod
	public ComponentMembrane share(String name) {
		ComponentMembrane membrane = new ComponentMembrane(component);
		membrane.inChannel(BootstrapRequest.class, requestChannel);
		membrane.outChannel(BootstrapResponse.class, responseChannel);
		membrane.seal();
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
		retryPeriod = 1000 * Long.parseLong(properties
				.getProperty("client.retry.period"));

		bootstrapServerAddress = new Address(ip, port, BigInteger.ZERO);
		localPeerAddress = peerAddress;

		logger = LoggerFactory.getLogger(BootstrapClient.class.getName() + "@"
				+ peerAddress.getId());
	}

	@MayTriggerEventTypes(CacheGetPeersRequest.class)
	private EventHandler<BootstrapRequest> handleBootstrapRequest = new EventHandler<BootstrapRequest>() {
		public void handle(BootstrapRequest event) {
			// set an alarm to retry the request if no response
			long id = timerHandler.setTimer(new ClientRetryRequest(event),
					timerSignalChannel, retryPeriod);

			CacheGetPeersRequest request = new CacheGetPeersRequest(event
					.getPeersMax(), id, bootstrapServerAddress);

			logger.debug("Sending GetPeersRequest");
			component.triggerEvent(request, pnSendChannel);
		}
	};

	@MayTriggerEventTypes(CacheGetPeersRequest.class)
	private EventHandler<ClientRetryRequest> handleClientRetryRequest = new EventHandler<ClientRetryRequest>() {
		public void handle(ClientRetryRequest event) {
			if (!timerHandler.isOustandingTimer(event.getTimerId())) {
				return;
			}
			timerHandler.cancelTimer(event.getTimerId());

			// set an alarm to retry the request if no response
			long id = timerHandler.setTimer(new ClientRetryRequest(event
					.getRequest()), timerSignalChannel, retryPeriod);

			CacheGetPeersRequest request = new CacheGetPeersRequest(event
					.getRequest().getPeersMax(), id, bootstrapServerAddress);

			logger.debug("Sending GetPeersRequest");
			component.triggerEvent(request, pnSendChannel);
		}
	};

	@MayTriggerEventTypes(BootstrapResponse.class)
	private EventHandler<CacheGetPeersResponse> handleCacheGetPeersResponse = new EventHandler<CacheGetPeersResponse>() {
		public void handle(CacheGetPeersResponse event) {
			if (timerHandler.isOustandingTimer(event.getReqestId())) {
				timerHandler.cancelTimer(event.getReqestId());
			}

			BootstrapResponse response = new BootstrapResponse(event.getPeers());

			logger.debug("Received GetPeersResponse");
			component.triggerEvent(response, responseChannel);
		}
	};

	@MayTriggerEventTypes( { CacheAddPeerRequest.class, ScheduleTimeout.class })
	private EventHandler<BootstrapCompleted> handleBootstrapCompleted = new EventHandler<BootstrapCompleted>() {
		public void handle(BootstrapCompleted event) {
			CacheAddPeerRequest request = new CacheAddPeerRequest(
					localPeerAddress, bootstrapServerAddress);

			component.triggerEvent(request, pnSendChannel);

			// set refresh timer
			ClientRefreshPeer refreshEvent = new ClientRefreshPeer(
					localPeerAddress);
			timerHandler.setTimer(refreshEvent, timerSignalChannel,
					refreshPeriod);
		}
	};

	@MayTriggerEventTypes( { CacheAddPeerRequest.class, ScheduleTimeout.class })
	private EventHandler<ClientRefreshPeer> handleClientRefreshPeer = new EventHandler<ClientRefreshPeer>() {
		public void handle(ClientRefreshPeer event) {
			CacheAddPeerRequest request = new CacheAddPeerRequest(
					localPeerAddress, bootstrapServerAddress);

			component.triggerEvent(request, pnSendChannel);

			// reset refresh timer
			timerHandler.setTimer(event, timerSignalChannel, refreshPeriod);
		}
	};

	@MayTriggerEventTypes(CacheResetRequest.class)
	private EventHandler<BootstrapCacheReset> handleBootstrapCacheReset = new EventHandler<BootstrapCacheReset>() {
		public void handle(BootstrapCacheReset event) {
			CacheResetRequest request = new CacheResetRequest(localPeerAddress,
					bootstrapServerAddress);
			component.triggerEvent(request, pnSendChannel);
		}
	};
}
