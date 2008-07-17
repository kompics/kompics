package se.sics.kompics.p2p.son.ps;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
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
import se.sics.kompics.p2p.fd.events.PeerFailureSuspicion;
import se.sics.kompics.p2p.fd.events.StartProbingPeer;
import se.sics.kompics.p2p.fd.events.StopProbingPeer;
import se.sics.kompics.p2p.fd.events.SuspicionStatus;
import se.sics.kompics.p2p.network.events.PerfectNetworkDeliverEvent;
import se.sics.kompics.p2p.network.events.PerfectNetworkSendEvent;
import se.sics.kompics.p2p.son.ps.events.CreateRing;
import se.sics.kompics.p2p.son.ps.events.FindSuccessorRequest;
import se.sics.kompics.p2p.son.ps.events.FindSuccessorResponse;
import se.sics.kompics.p2p.son.ps.events.GetPredecessorRequest;
import se.sics.kompics.p2p.son.ps.events.GetPredecessorResponse;
import se.sics.kompics.p2p.son.ps.events.GetRingNeighborsRequest;
import se.sics.kompics.p2p.son.ps.events.GetRingNeighborsResponse;
import se.sics.kompics.p2p.son.ps.events.GetSuccessorListRequest;
import se.sics.kompics.p2p.son.ps.events.GetSuccessorListResponse;
import se.sics.kompics.p2p.son.ps.events.JoinRing;
import se.sics.kompics.p2p.son.ps.events.JoinRingCompleted;
import se.sics.kompics.p2p.son.ps.events.NewPredecessor;
import se.sics.kompics.p2p.son.ps.events.NewSuccessor;
import se.sics.kompics.p2p.son.ps.events.Notify;
import se.sics.kompics.p2p.son.ps.events.StabilizeTimerSignal;
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

	private Channel timerSignalChannel;

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
	public void create(Channel requestChannel, Channel notificationChannel) {
		this.requestChannel = requestChannel;
		this.notificationChannel = notificationChannel;

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
		component.subscribe(this.requestChannel,
				"handleGetRingNeighborsRequest");
		component.subscribe(pnDeliverChannel, "handleFindSuccessorRequest");
		component.subscribe(pnDeliverChannel, "handleFindSuccessorResponse");
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

	@ComponentShareMethod
	public ComponentMembrane share(String name) {
		HashMap<Class<? extends Event>, Channel> map = new HashMap<Class<? extends Event>, Channel>();
		map.put(CreateRing.class, requestChannel);
		map.put(JoinRing.class, requestChannel);
		map.put(GetRingNeighborsRequest.class, requestChannel);
		map.put(JoinRingCompleted.class, notificationChannel);
		map.put(NewSuccessor.class, notificationChannel);
		map.put(NewPredecessor.class, notificationChannel);
		map.put(GetRingNeighborsResponse.class, notificationChannel);
		ComponentMembrane membrane = new ComponentMembrane(component, map);
		return component.registerSharedComponentMembrane(name, membrane);
	}

	@ComponentInitializeMethod("chord-ring.properties")
	public void init(Properties properties, Address localPeer) {
		stabilizationPeriod = 1000 * Long.parseLong(properties
				.getProperty("stabilization.period"));
		int log2RingSize = Integer.parseInt(properties
				.getProperty("log2.ring.size"));
		ringSize = new BigInteger("2").pow(log2RingSize);

		this.localPeer = localPeer;

		this.successorList = new SuccessorList(log2RingSize, localPeer,
				ringSize);

		logger = LoggerFactory.getLogger(getClass().getName() + "@"
				+ localPeer.getId());
	}

	@EventHandlerMethod
	@MayTriggerEventTypes( { NewSuccessor.class, NewPredecessor.class,
			JoinRingCompleted.class })
	public void handleCreateRing(CreateRing event) {
		logger.debug("CREATE");

		predecessor = null;
		// successor = localPeer;
		successorList.setSuccessor(localPeer);

		// trigger newSuccessor
		NewSuccessor newSuccessor = new NewSuccessor(localPeer, successorList
				.getSuccessor());
		component.triggerEvent(newSuccessor, notificationChannel);

		// trigger newPredecessor
		NewPredecessor newPredecessor = new NewPredecessor(localPeer,
				predecessor);
		component.triggerEvent(newPredecessor, notificationChannel);

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

		FindSuccessorRequest request = new FindSuccessorRequest(localPeer
				.getId());
		PerfectNetworkSendEvent sendEvent = new PerfectNetworkSendEvent(
				request, event.getInsidePeer());

		// send find successor request
		component.triggerEvent(sendEvent, pnSendChannel);

		// trigger newPredecessor
		NewPredecessor newPredecessor = new NewPredecessor(localPeer,
				predecessor);
		component.triggerEvent(newPredecessor, notificationChannel);
	}

	@EventHandlerMethod
	public void handleFindSuccessorRequest(FindSuccessorRequest event) {
		logger.debug("FIND_SUCC_REQ");

		BigInteger identifier = event.getIdentifier();

		if (RingMath.belongsTo(identifier, localPeer.getId(), successorList
				.getSuccessor().getId(), IntervalBounds.OPEN_CLOSED, ringSize)) {
			// return my successor as the real successor
			FindSuccessorResponse response = new FindSuccessorResponse(
					successorList.getSuccessor(), false);
			PerfectNetworkSendEvent sendEvent = new PerfectNetworkSendEvent(
					response, event.getSource());
			component.triggerEvent(sendEvent, pnSendChannel);
		} else {
			// return an indirection to my successor
			FindSuccessorResponse response = new FindSuccessorResponse(
					successorList.getSuccessor(), true);
			PerfectNetworkSendEvent sendEvent = new PerfectNetworkSendEvent(
					response, event.getSource());
			component.triggerEvent(sendEvent, pnSendChannel);
		}
	}

	@EventHandlerMethod
	@MayTriggerEventTypes( { NewSuccessor.class, JoinRingCompleted.class })
	public void handleFindSuccessorResponse(FindSuccessorResponse event) {
		logger.debug("FIND_SUCC_RESP");

		if (event.isNextHop()) {
			// we got an indirection
			Address nextHop = event.getSuccessor();

			FindSuccessorRequest request = new FindSuccessorRequest(localPeer
					.getId());
			PerfectNetworkSendEvent sendEvent = new PerfectNetworkSendEvent(
					request, nextHop);

			// send find successor request
			component.triggerEvent(sendEvent, pnSendChannel);
		} else {
			// we got the real successor
			successorList.setSuccessor(event.getSuccessor());

			neighborAdded(successorList.getSuccessor());

			// trigger GetSuccessorList
			GetSuccessorListRequest request = new GetSuccessorListRequest(
					RequestState.JOIN);
			PerfectNetworkSendEvent sendEvent = new PerfectNetworkSendEvent(
					request, successorList.getSuccessor());
			component.triggerEvent(sendEvent, pnSendChannel);

			// trigger newSuccessor
			NewSuccessor newSuccessor = new NewSuccessor(localPeer,
					successorList.getSuccessor());
			component.triggerEvent(newSuccessor, notificationChannel);

			logger.info("Join Completed");

			// trigger JoinRingCompleted
			JoinRingCompleted joinRingCompleted = new JoinRingCompleted(
					localPeer);
			component.triggerEvent(joinRingCompleted, notificationChannel);

			// set the stabilization timer
			StabilizeTimerSignal signal = new StabilizeTimerSignal();
			timerHandler.setTimer(signal, timerSignalChannel,
					stabilizationPeriod);
		}
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
	@MayTriggerEventTypes( { NewSuccessor.class, PerfectNetworkSendEvent.class })
	public void handleGetSuccessorListResponse(GetSuccessorListResponse event) {
		logger.debug("GET_SUCC_LIST_RESP");

		HashSet<Address> oldNeighbors = new HashSet<Address>(successorList
				.getSuccessors());

		successorList.updateSuccessorList(event.getSuccessorList());

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
	@MayTriggerEventTypes( { NewSuccessor.class, PerfectNetworkSendEvent.class })
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
				NewSuccessor newSuccessor = new NewSuccessor(localPeer,
						successorList.getSuccessor());
				component.triggerEvent(newSuccessor, notificationChannel);
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
		}
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(GetRingNeighborsResponse.class)
	public void handleGetRingNeighborsRequest(GetRingNeighborsRequest event) {
		GetRingNeighborsResponse response = new GetRingNeighborsResponse(
				localPeer, successorList.getSuccessor(), predecessor,
				successorList.getSuccessors());
		component.triggerEvent(response, notificationChannel);
	}

	@EventHandlerMethod
	public void handlePeerFailureSuspicion(PeerFailureSuspicion event) {
		if (event.getSuspicionStatus().equals(SuspicionStatus.SUSPECTED)) {
			// peer is suspected
			Address suspectedPeer = event.getPeerAddress();

			if (suspectedPeer.equals(predecessor)) {
				// predecessor suspected
				successorList.successorFailed(suspectedPeer);
				predecessor = null;
			}
			if (successorList.getSuccessors().contains(suspectedPeer)) {
				// secondary successor suspected
				successorList.successorFailed(suspectedPeer);
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
