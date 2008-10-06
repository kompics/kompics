package se.sics.kompics.p2p.chord.router;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.ComponentMembrane;
import se.sics.kompics.api.EventHandler;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.api.annotation.MayTriggerEventTypes;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.events.Message;
import se.sics.kompics.p2p.chord.IntervalBounds;
import se.sics.kompics.p2p.chord.RingMath;
import se.sics.kompics.p2p.chord.events.ChordLookupFailed;
import se.sics.kompics.p2p.chord.events.ChordLookupRequest;
import se.sics.kompics.p2p.chord.events.ChordLookupResponse;
import se.sics.kompics.p2p.chord.ring.events.JoinRingCompleted;
import se.sics.kompics.p2p.chord.ring.events.NewPredecessor;
import se.sics.kompics.p2p.chord.ring.events.NewSuccessorList;
import se.sics.kompics.p2p.chord.router.events.FindSuccessorRequest;
import se.sics.kompics.p2p.chord.router.events.FindSuccessorResponse;
import se.sics.kompics.p2p.chord.router.events.FixFingers;
import se.sics.kompics.p2p.chord.router.events.GetFingerTableRequest;
import se.sics.kompics.p2p.chord.router.events.GetFingerTableResponse;
import se.sics.kompics.p2p.chord.router.events.NewFingerTable;
import se.sics.kompics.p2p.chord.router.events.RpcTimeout;
import se.sics.kompics.timer.TimerHandler;
import se.sics.kompics.timer.events.CancelTimeout;
import se.sics.kompics.timer.events.ScheduleTimeout;

/**
 * The <code>ChordIterativeRouter</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public class ChordIterativeRouter {

	private Logger logger;

	private final Component component;

	// ChordRouter channels
	private Channel requestChannel, notificationChannel;

	// PerfectNetwork send channel
	private Channel pnSendChannel;

	private Channel timerSignalChannel, fixFingersLookupChannel,
			chordRingChannel;

	private TimerHandler timerHandler;

	private long fingerStabilizationPeriod;

	private int log2RingSize;

	private BigInteger ringSize;

	private Address localPeer, predecessor, successor;

	private FingerTable fingerTable;

	private long rpcTimeout;

	private HashMap<Long, LookupInfo> outstandingLookups;

	public ChordIterativeRouter(Component component) {
		this.component = component;

		outstandingLookups = new HashMap<Long, LookupInfo>();
	}

	@ComponentCreateMethod
	public void create(Channel requestChannel, Channel notificationChannel,
			Channel chordRingChannel) {
		this.requestChannel = requestChannel;
		this.notificationChannel = notificationChannel;
		this.chordRingChannel = chordRingChannel;

		// use shared timer component
		ComponentMembrane timerMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.Timer");
		Channel timerSetChannel = timerMembrane
				.getChannelIn(ScheduleTimeout.class);
		timerSignalChannel = component.createChannel(FixFingers.class,
				RpcTimeout.class);

		// use shared PerfectNetwork component
		ComponentMembrane pnMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.p2p.network.PerfectNetwork");
		pnSendChannel = pnMembrane.getChannelIn(Message.class);
		Channel pnDeliverChannel = pnMembrane.getChannelOut(Message.class);

		fixFingersLookupChannel = component.createChannel(
				ChordLookupResponse.class, ChordLookupFailed.class);

		component.subscribe(pnDeliverChannel, handleFindSuccessorRequest);
		component.subscribe(pnDeliverChannel, handleFindSuccessorResponse);
		component.subscribe(pnDeliverChannel, handleGetFingerTableRequest);
		component.subscribe(pnDeliverChannel, handleGetFingerTableResponse);

		component.subscribe(this.requestChannel, handleChordLookupRequest);

		component.subscribe(this.chordRingChannel, handleChordLookupRequest);
		component.subscribe(this.chordRingChannel, handleNewSuccessorList);
		component.subscribe(this.chordRingChannel, handleNewPredecessor);

		component.subscribe(fixFingersLookupChannel, handleChordLookupResponse);
		component.subscribe(fixFingersLookupChannel, handleChordLookupFailed);

		component.subscribe(timerSignalChannel, handleRpcTimeout);
		component.subscribe(timerSignalChannel, handleFixFingers);

		this.timerHandler = new TimerHandler(component, timerSetChannel);
	}

	@ComponentInitializeMethod("chord-router.properties")
	public void init(Properties properties, Address localPeer) {
		logger = LoggerFactory.getLogger(getClass().getName() + "@"
				+ localPeer.getId());

		fingerStabilizationPeriod = 1000 * Long.parseLong(properties
				.getProperty("finger.stabilization.period"));
		log2RingSize = Integer.parseInt(properties
				.getProperty("log2.ring.size"));

		rpcTimeout = 1000 * Long.parseLong(properties
				.getProperty("rpc.timeout"));

		ringSize = new BigInteger("2").pow(log2RingSize);

		this.localPeer = localPeer;

		fingerTable = new FingerTable(log2RingSize, localPeer, this);
		fingerTableChanged();
	}

	@MayTriggerEventTypes(GetFingerTableRequest.class)
	private EventHandler<NewSuccessorList> handleNewSuccessorList = new EventHandler<NewSuccessorList>() {
		public void handle(NewSuccessorList event) {
			logger.debug("NEW_SUCC {}", event.getSuccessorListView());

			if (successor == null) {
				// if this is the first time we get this event, we initialize
				// the
				// periodic FixFingers timer
				FixFingers fixFingers = new FixFingers();
				timerHandler.setTimer(fixFingers, timerSignalChannel,
						fingerStabilizationPeriod);

				successor = event.getSuccessorListView().get(0);

				if (!successor.equals(localPeer)) {
					// ask successor for its finger table
					GetFingerTableRequest request = new GetFingerTableRequest(
							successor);
					component.triggerEvent(request, pnSendChannel);
				}
			}

			successor = event.getSuccessorListView().get(0);

			boolean changed = false;
			// try to use the successors
			for (Address address : event.getSuccessorListView()) {
				if (fingerTable.learnedAboutPeer(address, false, false)) {
					changed = true;
				}
			}
			if (changed) {
				fingerTableChanged();
			}
		}
	};

	private EventHandler<NewPredecessor> handleNewPredecessor = new EventHandler<NewPredecessor>() {
		public void handle(NewPredecessor event) {
			logger.debug("NEW_PRED {}", event.getPredecessorPeer());

			predecessor = event.getPredecessorPeer();
		}
	};

	@MayTriggerEventTypes( { ScheduleTimeout.class, FindSuccessorRequest.class,
			ChordLookupResponse.class })
	private EventHandler<ChordLookupRequest> handleChordLookupRequest = new EventHandler<ChordLookupRequest>() {
		public void handle(ChordLookupRequest event) {
			logger.debug("CHORD_LOOKUP_REQ({})", event.getKey());

			BigInteger key = event.getKey();
			Address firstPeer = event.getFirstPeer();

			// special case for Ring join, we don't have a successor yet.
			if (firstPeer != null) {

				logger.debug("FIRST_PEER is {}", firstPeer);

				LookupInfo lookupInfo = new LookupInfo(event);
				lookupInfo.initiatedNow();

				RpcTimeout timeout = new RpcTimeout(event, firstPeer);
				long timerId = timerHandler.setTimer(timeout,
						timerSignalChannel, rpcTimeout);

				outstandingLookups.put(timerId, lookupInfo);

				FindSuccessorRequest request = new FindSuccessorRequest(key,
						timerId, firstPeer);

				component.triggerEvent(request, pnSendChannel);

				// we try to use the hinted first peer as a possible better
				// finger
				fingerTable.learnedAboutFreshPeer(firstPeer);
				return;
			}

			// special case for when we are alone in the ring
			if (successor == null || successor.equals(localPeer)) {
				// to avoid an infinite loop, we return ourselves
				ChordLookupResponse response = new ChordLookupResponse(key,
						localPeer, event.getAttachment(), new LookupInfo(event));
				component.triggerEvent(response, event.getResponseChannel());
				return;
			}

			// normal case
			if (predecessor != null
					&& RingMath.belongsTo(key, predecessor.getId(), localPeer
							.getId(), IntervalBounds.OPEN_CLOSED, ringSize)) {
				// we are responsible
				ChordLookupResponse response = new ChordLookupResponse(key,
						localPeer, event.getAttachment(), new LookupInfo(event));
				component.triggerEvent(response, event.getResponseChannel());
				return;
			} else if (RingMath.belongsTo(key, localPeer.getId(), successor
					.getId(), IntervalBounds.OPEN_CLOSED, ringSize)) {
				// our successor is responsible for the looked up key
				ChordLookupResponse response = new ChordLookupResponse(key,
						successor, event.getAttachment(), new LookupInfo(event));
				component.triggerEvent(response, event.getResponseChannel());
				return;
			} else {
				// some other peer is responsible for the looked up key
				LookupInfo lookupInfo = new LookupInfo(event);
				lookupInfo.initiatedNow();

				Address closest = fingerTable.closestPreceedingPeer(key);

				if (closest.equals(localPeer)) {
					// we found no closes peer so the lookup should fail
					ChordLookupFailed failed = new ChordLookupFailed(key, event
							.getAttachment(), null);
					component.triggerEvent(failed, event.getResponseChannel());
					return;
				}

				RpcTimeout timeout = new RpcTimeout(event, closest);
				long timerId = timerHandler.setTimer(timeout,
						timerSignalChannel, rpcTimeout);

				outstandingLookups.put(timerId, lookupInfo);

				FindSuccessorRequest request = new FindSuccessorRequest(key,
						timerId, closest);

				component.triggerEvent(request, pnSendChannel);
			}
		}
	};

	@MayTriggerEventTypes(FindSuccessorResponse.class)
	private EventHandler<FindSuccessorRequest> handleFindSuccessorRequest = new EventHandler<FindSuccessorRequest>() {
		public void handle(FindSuccessorRequest event) {
			logger.debug("FIND_SUCC_REQ {},succ={}", event.getKey(), successor);

			BigInteger key = event.getKey();

			if (predecessor != null
					&& RingMath.belongsTo(key, predecessor.getId(), localPeer
							.getId(), IntervalBounds.OPEN_CLOSED, ringSize)) {
				// return ourselves as the real responsible
				FindSuccessorResponse response = new FindSuccessorResponse(
						localPeer, event.getLookupId(), false, event
								.getSource());
				component.triggerEvent(response, pnSendChannel);

				logger.error("RETURNED MYSELF {}, {}", key, localPeer);

			} else if (successor != null) {
				if (RingMath.belongsTo(key, localPeer.getId(), successor
						.getId(), IntervalBounds.OPEN_CLOSED, ringSize)) {
					// return my successor as the real successor
					FindSuccessorResponse response = new FindSuccessorResponse(
							successor, event.getLookupId(), false, event
									.getSource());
					component.triggerEvent(response, pnSendChannel);
				} else {
					// return an indirection to my closest preceding finger
					Address closest = fingerTable.closestPreceedingPeer(key);

					if (!closest.equals(localPeer)) {
						FindSuccessorResponse response = new FindSuccessorResponse(
								closest, event.getLookupId(), true, event
										.getSource());
						component.triggerEvent(response, pnSendChannel);
					}
					// else we found no closest peer so the lookup should fail
				}
			}

			// we try to use the requester as a possible better finger
			fingerTable.learnedAboutFreshPeer(event.getSource());
		}
	};

	@MayTriggerEventTypes( { ScheduleTimeout.class, CancelTimeout.class,
			FindSuccessorRequest.class, ChordLookupResponse.class })
	private EventHandler<FindSuccessorResponse> handleFindSuccessorResponse = new EventHandler<FindSuccessorResponse>() {
		public void handle(FindSuccessorResponse event) {

			long lookupId = event.getLookupId();
			if (timerHandler.isOustandingTimer(lookupId)) {
				// we got the response before the RpcTimeout, so we cancel the
				// timer
				timerHandler.cancelTimer(lookupId);
			} else {
				// we got the response too late so we just give up since we have
				// already triggered a LookupFailed event
				return;
			}

			LookupInfo lookupInfo = outstandingLookups.remove(lookupId);
			ChordLookupRequest lookupRequest = lookupInfo.getRequest();

			logger.debug("FIND_SUCC_RESP NH({}) is {}. {}", new Object[] {
					lookupRequest.getKey(), event.getSuccessor(),
					event.isNextHop() });

			if (event.isNextHop()) {
				// we got an indirection
				Address nextHop = event.getSuccessor();

				RpcTimeout timeout = new RpcTimeout(lookupRequest, nextHop);
				long timerId = timerHandler.setTimer(timeout,
						timerSignalChannel, rpcTimeout);

				lookupInfo.appendHop(event.getSource());
				outstandingLookups.put(timerId, lookupInfo);

				FindSuccessorRequest request = new FindSuccessorRequest(
						lookupRequest.getKey(), timerId, nextHop);

				// send find successor request
				component.triggerEvent(request, pnSendChannel);
			} else {
				// we got the real responsible
				lookupInfo.appendHop(event.getSource());
				lookupInfo.completedNow();
				ChordLookupResponse response = new ChordLookupResponse(
						lookupRequest.getKey(), event.getSuccessor(),
						lookupRequest.getAttachment(), lookupInfo);
				component.triggerEvent(response, lookupRequest
						.getResponseChannel());
			}

			// we try to use the responsible or the next hop as a possible
			// better
			// finger
			fingerTable.learnedAboutPeer(event.getSuccessor());
		}
	};

	@MayTriggerEventTypes(ChordLookupFailed.class)
	private EventHandler<RpcTimeout> handleRpcTimeout = new EventHandler<RpcTimeout>() {
		public void handle(RpcTimeout event) {
			logger.debug("RPC_TIMEOUT");

			long timerId = event.getTimerId();

			if (timerHandler.isOustandingTimer(timerId)) {
				timerHandler.handledTimerExpired(timerId);

				// we got an RPC timeout before the RPC response so we have to
				// return a ChordLookupFailed event and to mark the finger as
				// failed
				ChordLookupRequest lookupRequest = outstandingLookups.remove(
						timerId).getRequest();
				ChordLookupFailed failed = new ChordLookupFailed(lookupRequest
						.getKey(), lookupRequest.getAttachment(), event
						.getPeer());
				component.triggerEvent(failed, lookupRequest
						.getResponseChannel());

				// mark the slow/failed peer as suspected
				fingerTable.fingerSuspected(event.getPeer());
			}
			// else we got the RPC response before the timeout so everything is
			// OK.
		}
	};

	@MayTriggerEventTypes( { ChordLookupRequest.class, ScheduleTimeout.class })
	private EventHandler<FixFingers> handleFixFingers = new EventHandler<FixFingers>() {
		public void handle(FixFingers event) {
			logger.debug("FIX_FINGER");

			int nextFingerToFix = fingerTable.nextFingerToFix();

			timerHandler.setTimer(event, timerSignalChannel,
					fingerStabilizationPeriod);

			// actually fix the finger now
			// finger[next] = findSuccessor(n plus 2^(next-1))
			BigInteger fingerBegin = fingerTable
					.getFingerBegin(nextFingerToFix);
			ChordLookupRequest request = new ChordLookupRequest(fingerBegin,
					fixFingersLookupChannel, nextFingerToFix);
			component.triggerEvent(request, requestChannel);
		}
	};

	private EventHandler<ChordLookupResponse> handleChordLookupResponse = new EventHandler<ChordLookupResponse>() {
		public void handle(ChordLookupResponse event) {
			logger.debug("CHORD_LOOKUP_RESP");

			Address fingerPeer = event.getResponsible();
			int fingerIndex = (Integer) event.getAttachment();

			fingerTable.fingerFixed(fingerIndex, fingerPeer);
		}
	};

	@MayTriggerEventTypes(ChordLookupRequest.class)
	private EventHandler<ChordLookupFailed> handleChordLookupFailed = new EventHandler<ChordLookupFailed>() {
		public void handle(ChordLookupFailed event) {
			logger.debug("CHORD_LOOKUP_FAILED");

			// retry lookup
			// int fingerIndex = (Integer) event.getAttachment();
			// BigInteger fingerBegin = fingerTable.getFingerBegin(fingerIndex);
			// ChordLookupRequest request = new ChordLookupRequest(fingerBegin,
			// fixFingersLookupChannel, fingerIndex);
			// component.triggerEvent(request, requestChannel);
		}
	};

	@MayTriggerEventTypes(GetFingerTableResponse.class)
	private EventHandler<GetFingerTableRequest> handleGetFingerTableRequest = new EventHandler<GetFingerTableRequest>() {
		public void handle(GetFingerTableRequest event) {
			logger.debug("GET_FINGER_TABLE_REQ");

			GetFingerTableResponse response = new GetFingerTableResponse(
					fingerTable.getView(), event.getSource());
			component.triggerEvent(response, pnSendChannel);
		}
	};

	@MayTriggerEventTypes(JoinRingCompleted.class)
	private EventHandler<GetFingerTableResponse> handleGetFingerTableResponse = new EventHandler<GetFingerTableResponse>() {
		public void handle(GetFingerTableResponse event) {
			logger.debug("GET_FINGER_TABLE_RESP");

			// we try to populate our finger table with our successor's finger
			// table
			boolean changed = false;
			// try to use the successors
			for (Address address : event.getFingerTable().finger) {
				if (fingerTable.learnedAboutPeer(address, false, false)) {
					changed = true;
				}
			}
			if (changed) {
				fingerTableChanged();
			}
//			component.triggerEvent(new JoinRingCompleted(localPeer),
//					notificationChannel);
		}
	};

	void fingerTableChanged() {
		NewFingerTable newFingerTable = new NewFingerTable(fingerTable
				.getView());
		component.triggerEvent(newFingerTable, notificationChannel);
	}
}
