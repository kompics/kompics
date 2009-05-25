package se.sics.kompics.wan.daemon.masterclient;

import java.util.HashSet;
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
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.wan.daemon.DaemonAddress;
import se.sics.kompics.wan.master.ClientRefreshPeer;
import se.sics.kompics.wan.master.ClientRetryRequest;
import se.sics.kompics.wan.master.ConnectMasterRequest;
import se.sics.kompics.wan.master.ConnectMasterRequestMsg;
import se.sics.kompics.wan.master.ConnectMasterResponse;
import se.sics.kompics.wan.master.ConnectMasterResponseMsg;
import se.sics.kompics.wan.master.DisconnectMasterRequest;
import se.sics.kompics.wan.master.DisconnectMasterRequestMsg;
import se.sics.kompics.wan.master.KeepAliveDaemonMsg;

public class MasterClient extends ComponentDefinition {

	Negative<MasterClientP> masterPort = negative(MasterClientP.class);

	Positive<Network> network = positive(Network.class);
	Positive<Timer> timer = positive(Timer.class);

	private Logger logger;

	private final HashSet<UUID> outstandingTimeouts;
	private ConnectMasterRequest activeConnectMasterRequest;

	private HashSet<PeerEntry> overlays;

	private Address masterAddress;
	private DaemonAddress self;
	private long refreshPeriod;
	private long retryPeriod;
	private long clientKeepAlivePeriod ;
	private int retriesCount;

	public MasterClient() {
		outstandingTimeouts = new HashSet<UUID>();
		overlays = new HashSet<PeerEntry>();

		subscribe(handleInit, control);


		subscribe(handleConnectMasterRequest, masterPort);
		subscribe(handleConnectMasterResponse, network);
		subscribe(handleClientRefreshPeer, timer);
		subscribe(handleClientRetryRequest, timer);
		subscribe(handleDisconnectMasterRequest, masterPort);
	}

	Handler<MasterClientInit> handleInit = new Handler<MasterClientInit>() {
		public void handle(MasterClientInit init) {
			refreshPeriod = init.getMasterConfiguration().getClientKeepAlivePeriod();
			retryPeriod = init.getMasterConfiguration().getClientRetryPeriod();
			retriesCount = init.getMasterConfiguration().getClientRetryCount();
			masterAddress = init.getMasterConfiguration().getMasterAddress();
			self = init.getSelf();
			clientKeepAlivePeriod = init.getMasterConfiguration().getClientKeepAlivePeriod();

			logger = LoggerFactory.getLogger(MasterClient.class.getName() + "@"
					+ self.getDaemonId());
		}
	};		

	private Handler<ConnectMasterRequest> handleConnectMasterRequest = new Handler<ConnectMasterRequest>() {
		public void handle(ConnectMasterRequest event) {
			// set an alarm to retry the request if no response
			ScheduleTimeout st = new ScheduleTimeout(retryPeriod);
			ClientRetryRequest retryRequest = new ClientRetryRequest(st, retriesCount, event);
			st.setTimeoutEvent(retryRequest);
			UUID timerId = retryRequest.getTimeoutId();
			ConnectMasterRequestMsg request = new ConnectMasterRequestMsg(timerId, self,
					masterAddress);

			outstandingTimeouts.add(timerId);

			activeConnectMasterRequest = event;

			trigger(request, network);
			trigger(st, timer);

			logger.debug("Sending GetPeersRequest to " + masterAddress);
		}
	};

	private Handler<ClientRetryRequest> handleClientRetryRequest = new Handler<ClientRetryRequest>() {
		public void handle(ClientRetryRequest event) {
			if (!outstandingTimeouts.contains(event.getTimeoutId())) {
				return;
			}
			outstandingTimeouts.remove(event.getTimeoutId());

			if (event.getRetriesLeft() > 0) {
				// set an alarm to retry the request if no response
				ScheduleTimeout st = new ScheduleTimeout(retryPeriod);
				ClientRetryRequest retryRequest = new ClientRetryRequest(st,
						event.getRetriesLeft() - 1, event.getRequest());
				st.setTimeoutEvent(retryRequest);
				UUID timerId = retryRequest.getTimeoutId();
				ConnectMasterRequestMsg request = new ConnectMasterRequestMsg(timerId, self,
						masterAddress);

				outstandingTimeouts.add(timerId);

				activeConnectMasterRequest = event.getRequest();

				trigger(request, network);
				trigger(st, timer);

				logger.debug("Sending GetPeersRequest to  " + masterAddress);
			} else {
				ConnectMasterResponse response = new ConnectMasterResponse(
						activeConnectMasterRequest, false);
				trigger(response, masterPort);
			}
		}
	};

	private Handler<ConnectMasterResponseMsg> handleConnectMasterResponse = new Handler<ConnectMasterResponseMsg>() {
		public void handle(ConnectMasterResponseMsg event) {
			if (outstandingTimeouts.contains(event.getRequestId())) {
				CancelTimeout ct = new CancelTimeout(event.getRequestId());
				trigger(ct, timer);
				outstandingTimeouts.remove(event.getRequestId());
			} else {
				// request was retried. we ignore this first slow response.
				// (to avoid double response;TODO add a local BOOTSTRAPPED flag
				// per overlay)
				return;
			}

			// TODO request map for MULTIPLE overlays
			ConnectMasterResponse response = new ConnectMasterResponse(activeConnectMasterRequest,
					true);

			logger.debug("Received ConectMasterResponse");
			trigger(response, masterPort);
			
			keepAlive();
		}
	};


	private void keepAlive()
	{
			KeepAliveDaemonMsg request = new KeepAliveDaemonMsg(self, masterAddress);
			trigger(request, network);

			// set refresh periodic timer
			SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(clientKeepAlivePeriod, clientKeepAlivePeriod);
			spt.setTimeoutEvent(new ClientRefreshPeer(spt));
			trigger(spt, timer);
	}
	
	private Handler<ClientRefreshPeer> handleClientRefreshPeer = new Handler<ClientRefreshPeer>() {
		public void handle(ClientRefreshPeer event) {
			KeepAliveDaemonMsg request = new KeepAliveDaemonMsg(self, masterAddress);
			trigger(request, network);
		}
	};

	private Handler<DisconnectMasterRequest> handleDisconnectMasterRequest = 
			new Handler<DisconnectMasterRequest>() {
		public void handle(DisconnectMasterRequest event) {

			DisconnectMasterRequestMsg request = new DisconnectMasterRequestMsg(self,
					masterAddress);

			trigger(request, network);
			logger.debug("Sending DisconnectMasterRequestMsg to " + masterAddress);
		}
	};

}
