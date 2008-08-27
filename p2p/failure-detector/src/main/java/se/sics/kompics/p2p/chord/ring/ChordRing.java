package se.sics.kompics.p2p.chord.ring;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Properties;

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
import se.sics.kompics.p2p.chord.IntervalBounds;
import se.sics.kompics.p2p.chord.RingMath;
import se.sics.kompics.p2p.chord.events.ChordLookupFailed;
import se.sics.kompics.p2p.chord.events.ChordLookupRequest;
import se.sics.kompics.p2p.chord.events.ChordLookupResponse;
import se.sics.kompics.p2p.chord.events.CreateRing;
import se.sics.kompics.p2p.chord.events.JoinRing;
import se.sics.kompics.p2p.chord.ring.events.GetPredecessorRequest;
import se.sics.kompics.p2p.chord.ring.events.GetPredecessorResponse;
import se.sics.kompics.p2p.chord.ring.events.GetSuccessorListRequest;
import se.sics.kompics.p2p.chord.ring.events.GetSuccessorListResponse;
import se.sics.kompics.p2p.chord.ring.events.JoinRingCompleted;
import se.sics.kompics.p2p.chord.ring.events.NewPredecessor;
import se.sics.kompics.p2p.chord.ring.events.NewSuccessorList;
import se.sics.kompics.p2p.chord.ring.events.Notify;
import se.sics.kompics.p2p.chord.ring.events.StabilizeTimerSignal;
import se.sics.kompics.p2p.fd.events.PeerFailureSuspicion;
import se.sics.kompics.p2p.fd.events.StartProbingPeer;
import se.sics.kompics.p2p.fd.events.StopProbingPeer;
import se.sics.kompics.p2p.fd.events.SuspicionStatus;
import se.sics.kompics.p2p.network.events.PerfectNetworkDeliverEvent;
import se.sics.kompics.p2p.network.events.PerfectNetworkSendEvent;
import se.sics.kompics.timer.TimerHandler;
import se.sics.kompics.timer.events.SetTimerEvent;

/**
 * The <code>ChordRing</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public class ChordRing {

	private Logger logger;

	private final Component component;

	// ChordRing channels
	private Channel requestChannel, notificationChannel;

	// PerfectNetwork send channel
	private Channel pnSendChannel;

	// FailureDetector channels
	private Channel fdRequestChannel, fdNotificationChannel;

	private Channel timerSignalChannel, joinLookupChannel, chordRouterChannel;

	private TimerHandler timerHandler;

	private long stabilizationPeriod;

	// local state

	private BigInteger ringSize;

	private Address localPeer, predecessor;

	private SuccessorList successorList;

	public ChordRing(Component component) {
		this.component = component;
	}

	@ComponentCreateMethod
	public void create(Channel requestChannel, Channel notificationChannel,
			Channel chordRouterChannel) {
		this.requestChannel = requestChannel;
		this.notificationChannel = notificationChannel;
		this.chordRouterChannel = chordRouterChannel;

		joinLookupChannel = component.createChannel(ChordLookupResponse.class,
				ChordLookupFailed.class);

		// use shared timer component
		ComponentMembrane timerMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.Timer");
		Channel timerSetChannel = timerMembrane.getChannel(SetTimerEvent.class);
		timerSignalChannel = component
				.createChannel(StabilizeTimerSignal.class);

		// use shared PerfectNetwork component
		ComponentMembrane pnMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.p2p.network.PerfectNetwork");
		pnSendChannel = pnMembrane.getChannel(PerfectNetworkSendEvent.class);
		Channel pnDeliverChannel = pnMembrane
				.getChannel(PerfectNetworkDeliverEvent.class);

		// use shared FailureDetector component
		ComponentMembrane fdMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.p2p.fd.FailureDetector");
		fdRequestChannel = fdMembrane.getChannel(StartProbingPeer.class);
		fdNotificationChannel = component
				.createChannel(PeerFailureSuspicion.class);

		component.subscribe(this.requestChannel, "handleCreateRing");
		component.subscribe(this.requestChannel, "handleJoinRing");
		component.subscribe(joinLookupChannel, "handleChordLookupResponse");
		component.subscribe(joinLookupChannel, "handleChordLookupFailed");
		component.subscribe(pnDeliverChannel, "handleGetPredecessorRequest");
		component.subscribe(pnDeliverChannel, "handleGetPredecessorResponse");
		component.subscribe(pnDeliverChannel, "handleGetSuccessorListRequest");
		component.subscribe(pnDeliverChannel, "handleGetSuccessorListResponse");
		component.subscribe(pnDeliverChannel, "handleNotify");

		this.timerHandler = new TimerHandler(component, timerSetChannel);
		component.subscribe(timerSignalChannel, "handleStabilizeTimerSignal");

		component
				.subscribe(fdNotificationChannel, "handlePeerFailureSuspicion");
	}

	@ComponentInitializeMethod("chord-ring.properties")
	public void init(Properties properties, Address localPeer) {
		stabilizationPeriod = 1000 * Long.parseLong(properties
				.getProperty("stabilization.period"));
		int log2RingSize = Integer.parseInt(properties
				.getProperty("log2.ring.size"));
		ringSize = new BigInteger("2").pow(log2RingSize);

		int successorListLength = Integer.parseInt(properties
				.getProperty("successor.list.length"));

		this.localPeer = localPeer;

		this.successorList = new SuccessorList(successorListLength, localPeer,
				ringSize);

		logger = LoggerFactory.getLogger(getClass().getName() + "@"
				+ localPeer.getId());
	}

	@EventHandlerMethod
	@MayTriggerEventTypes( { NewSuccessorList.class, NewPredecessor.class,
			JoinRingCompleted.class })
	public void handleCreateRing(CreateRing event) {
		logger.debug("CREATE");

		predecessor = null;
		// successor = localPeer;
		successorList.setSuccessor(localPeer);

		// trigger newSuccessor
		NewSuccessorList newSuccessor = new NewSuccessorList(localPeer,
				successorList.getSuccessorListView());
		component.triggerEvent(newSuccessor, notificationChannel);
		component.triggerEvent(newSuccessor, chordRouterChannel);

		// trigger newPredecessor
		NewPredecessor newPredecessor = new NewPredecessor(localPeer,
				predecessor);
		component.triggerEvent(newPredecessor, notificationChannel);
		component.triggerEvent(newPredecessor, chordRouterChannel);

		logger.info("Join Completed");

		// trigger JoinRingCompleted
		JoinRingCompleted joinRingCompleted = new JoinRingCompleted(localPeer);
		component.triggerEvent(joinRingCompleted, notificationChannel);

		// set the stabilization timer
		StabilizeTimerSignal signal = new StabilizeTimerSignal();
		timerHandler.setTimer(signal, timerSignalChannel, stabilizationPeriod);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes( { PerfectNetworkSendEvent.class,
			NewPredecessor.class })
	public void handleJoinRing(JoinRing event) {
		logger.debug("JOIN");

		predecessor = null;

		ChordLookupRequest lookupRequest = new ChordLookupRequest(localPeer
				.getId(), joinLookupChannel, event.getInsidePeer(), event
				.getInsidePeer());

		component.triggerEvent(lookupRequest, chordRouterChannel);

		// trigger newPredecessor
		NewPredecessor newPredecessor = new NewPredecessor(localPeer,
				predecessor);
		component.triggerEvent(newPredecessor, notificationChannel);
		component.triggerEvent(newPredecessor, chordRouterChannel);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes( { NewSuccessorList.class, JoinRingCompleted.class })
	public void handleChordLookupResponse(ChordLookupResponse event) {
		logger.debug("CHORD_LOOKUP_RESP R({})={}", event.getKey(), event
				.getResponsible());

		// TODO try to get our successor for joining in parallel from multiple
		// peers, or retry joining sequentially to different peers

		// we got the real successor
		successorList.setSuccessor(event.getResponsible());

		neighborAdded(successorList.getSuccessor());

		// trigger GetSuccessorList
		GetSuccessorListRequest request = new GetSuccessorListRequest(
				RequestState.JOIN);
		PerfectNetworkSendEvent sendEvent = new PerfectNetworkSendEvent(
				request, successorList.getSuccessor());
		component.triggerEvent(sendEvent, pnSendChannel);

		// trigger newSuccessor
		NewSuccessorList newSuccessor = new NewSuccessorList(localPeer,
				successorList.getSuccessorListView());
		component.triggerEvent(newSuccessor, notificationChannel);
		component.triggerEvent(newSuccessor, chordRouterChannel);

		logger.info("Join Completed");

		// trigger JoinRingCompleted
		JoinRingCompleted joinRingCompleted = new JoinRingCompleted(localPeer);
		component.triggerEvent(joinRingCompleted, notificationChannel);

		// set the stabilization timer
		StabilizeTimerSignal signal = new StabilizeTimerSignal();
		timerHandler.setTimer(signal, timerSignalChannel, stabilizationPeriod);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(ChordLookupRequest.class)
	public void handleChordLookupFailed(ChordLookupFailed event) {
		logger.debug("CHORD_LOOKUP_FAILED");

		// retry lookup
		ChordLookupRequest request = new ChordLookupRequest(localPeer.getId(),
				joinLookupChannel, event.getAttachment(), (Address) event
						.getAttachment() /*
										 * TODO cycle through other joining
										 * peers or retry bootstrap
										 */);
		component.triggerEvent(request, chordRouterChannel);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(PerfectNetworkSendEvent.class)
	public void handleGetSuccessorListRequest(GetSuccessorListRequest event) {
		logger.debug("GET_SUCC_LIST_REQ");

		GetSuccessorListResponse response = new GetSuccessorListResponse(
				successorList, event.getRequestState());
		PerfectNetworkSendEvent sendEvent = new PerfectNetworkSendEvent(
				response, event.getSource());
		// reply with successorList
		component.triggerEvent(sendEvent, pnSendChannel);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes( { NewSuccessorList.class,
			PerfectNetworkSendEvent.class })
	public void handleGetSuccessorListResponse(GetSuccessorListResponse event) {
		logger.debug("GET_SUCC_LIST_RESP my={} got={}", successorList
				.getSuccessors(), event.getSuccessorList().getSuccessors());

		HashSet<Address> oldNeighbors = new HashSet<Address>(successorList
				.getSuccessors());

		successorList.updateSuccessorList(event.getSuccessorList());

		// trigger newSuccessor
		NewSuccessorList newSuccessor = new NewSuccessorList(localPeer,
				successorList.getSuccessorListView());
		component.triggerEvent(newSuccessor, notificationChannel);
		component.triggerEvent(newSuccessor, chordRouterChannel);

		// account for neighbor set change
		oldNeighbors.add(predecessor);
		oldNeighbors.add(successorList.getSuccessor());

		HashSet<Address> newNeighbors = new HashSet<Address>(successorList
				.getSuccessors());
		newNeighbors.add(predecessor);
		newNeighbors.add(successorList.getSuccessor());

		// start monitoring new neighbors
		newNeighbors.removeAll(oldNeighbors);
		for (Address address : newNeighbors) {
			neighborAdded(address);
		}

		newNeighbors = new HashSet<Address>(successorList.getSuccessors());
		newNeighbors.add(predecessor);
		newNeighbors.add(successorList.getSuccessor());

		// stop monitoring former neighbors
		oldNeighbors.removeAll(newNeighbors);
		for (Address address : oldNeighbors) {
			neighborRemoved(address);
		}
	}

	@EventHandlerMethod
	@MayTriggerEventTypes( { PerfectNetworkSendEvent.class, SetTimerEvent.class })
	public void handleStabilizeTimerSignal(StabilizeTimerSignal event) {
		logger.debug("STABILIZATION");

		GetPredecessorRequest request = new GetPredecessorRequest();
		PerfectNetworkSendEvent sendEvent = new PerfectNetworkSendEvent(
				request, successorList.getSuccessor());
		// send get predecessor request
		component.triggerEvent(sendEvent, pnSendChannel);
		// reset the stabilization timer
		timerHandler.setTimer(event, timerSignalChannel, stabilizationPeriod);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(PerfectNetworkSendEvent.class)
	public void handleGetPredecessorRequest(GetPredecessorRequest event) {
		logger.debug("GET_PRED_REQ");

		GetPredecessorResponse response = new GetPredecessorResponse(
				predecessor);
		PerfectNetworkSendEvent sendEvent = new PerfectNetworkSendEvent(
				response, event.getSource());
		// reply with predecessor
		component.triggerEvent(sendEvent, pnSendChannel);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes( { NewSuccessorList.class,
			PerfectNetworkSendEvent.class })
	public void handleGetPredecessorResponse(GetPredecessorResponse event) {
		logger.debug("GET_PRED_RESP");

		Address predecessorOfMySuccessor = event.getPredecessor();

		if (predecessorOfMySuccessor != null) {
			if (RingMath.belongsTo(predecessorOfMySuccessor.getId(), localPeer
					.getId(), successorList.getSuccessor().getId(),
					IntervalBounds.OPEN_OPEN, ringSize)) {
				successorList.setSuccessor(predecessorOfMySuccessor);

				neighborAdded(successorList.getSuccessor());

				// trigger newSuccessor
				NewSuccessorList newSuccessor = new NewSuccessorList(localPeer,
						successorList.getSuccessorListView());
				component.triggerEvent(newSuccessor, notificationChannel);
				component.triggerEvent(newSuccessor, chordRouterChannel);
			}
		}

		// trigger GetSuccessorList
		GetSuccessorListRequest request = new GetSuccessorListRequest(
				RequestState.STABILIZE);
		PerfectNetworkSendEvent gslSendEvent = new PerfectNetworkSendEvent(
				request, successorList.getSuccessor());
		component.triggerEvent(gslSendEvent, pnSendChannel);

		Notify notify = new Notify(localPeer);
		PerfectNetworkSendEvent nSendEvent = new PerfectNetworkSendEvent(
				notify, successorList.getSuccessor());
		// send notify
		component.triggerEvent(nSendEvent, pnSendChannel);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(NewPredecessor.class)
	public void handleNotify(Notify event) {
		logger.debug("NOTIFY");

		Address potentialNewPredecessor = event.getFromPeer();

		if (predecessor == null
				|| RingMath.belongsTo(potentialNewPredecessor.getId(),
						predecessor.getId(), localPeer.getId(),
						IntervalBounds.OPEN_OPEN, ringSize)) {
			predecessor = potentialNewPredecessor;

			neighborAdded(potentialNewPredecessor);

			// trigger newPredecessor
			NewPredecessor newPredecessor = new NewPredecessor(localPeer,
					predecessor);
			component.triggerEvent(newPredecessor, notificationChannel);
			component.triggerEvent(newPredecessor, chordRouterChannel);
		}
	}

	@EventHandlerMethod
	public void handlePeerFailureSuspicion(PeerFailureSuspicion event) {
		logger.debug("FAILURE_SUSPICION");

		if (event.getSuspicionStatus().equals(SuspicionStatus.SUSPECTED)) {
			// peer is suspected
			Address suspectedPeer = event.getPeerAddress();

			if (suspectedPeer.equals(predecessor)) {
				// predecessor suspected
				predecessor = null;

				// trigger newPredecessor
				NewPredecessor newPredecessor = new NewPredecessor(localPeer,
						predecessor);
				component.triggerEvent(newPredecessor, notificationChannel);
				component.triggerEvent(newPredecessor, chordRouterChannel);
			}
			if (successorList.getSuccessors().contains(suspectedPeer)) {
				// secondary successor suspected
				successorList.successorFailed(suspectedPeer);

				// trigger newSuccessor
				NewSuccessorList newSuccessor = new NewSuccessorList(localPeer,
						successorList.getSuccessorListView());
				component.triggerEvent(newSuccessor, notificationChannel);
				component.triggerEvent(newSuccessor, chordRouterChannel);
			}
			neighborRemoved(suspectedPeer);
		} else {
			// peer is alive again
		}
	}

	private void neighborAdded(Address peer) {
		if (!peer.equals(localPeer)) {
			// start failure detection on new neighbor
			StartProbingPeer request = new StartProbingPeer(peer, component,
					fdNotificationChannel);
			component.triggerEvent(request, fdRequestChannel);
		}
	}

	private void neighborRemoved(Address peer) {
		// stop failure detection on neighbor
		StopProbingPeer request = new StopProbingPeer(peer, component);
		component.triggerEvent(request, fdRequestChannel);
	}
}
