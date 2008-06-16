package se.sics.kompics.p2p.son.ps;

import java.math.BigInteger;
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
import se.sics.kompics.p2p.network.events.PerfectNetworkDeliverEvent;
import se.sics.kompics.p2p.network.events.PerfectNetworkSendEvent;
import se.sics.kompics.p2p.son.ps.events.CreateRing;
import se.sics.kompics.p2p.son.ps.events.FindSuccessorRequest;
import se.sics.kompics.p2p.son.ps.events.FindSuccessorResponse;
import se.sics.kompics.p2p.son.ps.events.GetPredecessorRequest;
import se.sics.kompics.p2p.son.ps.events.GetPredecessorResponse;
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

	private Channel timerSignalChannel;

	private TimerHandler timerHandler;

	private long stabilizationPeriod;

	// local state

	private BigInteger ringSize;

	private Address localPeer, predecessor, successor;

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

		component.subscribe(this.requestChannel, "handleCreateRing");
		component.subscribe(this.requestChannel, "handleJoinRing");
		component.subscribe(pnDeliverChannel, "handleFindSuccessorRequest");
		component.subscribe(pnDeliverChannel, "handleFindSuccessorResponse");
		component.subscribe(pnDeliverChannel, "handleGetPredecessorRequest");
		component.subscribe(pnDeliverChannel, "handleGetPredecessorResponse");
		component.subscribe(pnDeliverChannel, "handleNotify");

		this.timerHandler = new TimerHandler(component, timerSetChannel);
		component.subscribe(timerSignalChannel, "handleStabilizeTimerSignal");
	}

	@ComponentShareMethod
	public ComponentMembrane share(String name) {
		HashMap<Class<? extends Event>, Channel> map = new HashMap<Class<? extends Event>, Channel>();
		map.put(CreateRing.class, requestChannel);
		map.put(JoinRing.class, requestChannel);
		map.put(JoinRingCompleted.class, notificationChannel);
		map.put(NewSuccessor.class, notificationChannel);
		map.put(NewPredecessor.class, notificationChannel);
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

		logger = LoggerFactory.getLogger(getClass().getName() + "@"
				+ localPeer.getId());
	}

	@EventHandlerMethod
	@MayTriggerEventTypes( { NewSuccessor.class, NewPredecessor.class,
			JoinRingCompleted.class })
	public void handleCreateRing(CreateRing event) {
		logger.debug("CREATE");

		predecessor = null;
		successor = null;

		// trigger newSuccessor
		NewSuccessor newSuccessor = new NewSuccessor(localPeer, successor);
		component.triggerEvent(newSuccessor, notificationChannel);

		// trigger newPredecessor
		NewPredecessor newPredecessor = new NewPredecessor(localPeer,
				predecessor);
		component.triggerEvent(newPredecessor, notificationChannel);

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

		if (successor == null) {
			// I have no successor, I return myself
			FindSuccessorResponse response = new FindSuccessorResponse(
					localPeer, false);
			PerfectNetworkSendEvent sendEvent = new PerfectNetworkSendEvent(
					response, event.getSource());
			component.triggerEvent(sendEvent, pnSendChannel);
			return;
		}

		if (belongsTo(identifier, localPeer, successor,
				IntervalBounds.OPEN_CLOSED)) {
			// return my successor as the real successor
			FindSuccessorResponse response = new FindSuccessorResponse(
					successor, false);
			PerfectNetworkSendEvent sendEvent = new PerfectNetworkSendEvent(
					response, event.getSource());
			component.triggerEvent(sendEvent, pnSendChannel);
		} else {
			// return an indirection to my successor
			FindSuccessorResponse response = new FindSuccessorResponse(
					successor, true);
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
			successor = event.getSuccessor();

			// trigger newSuccessor
			NewSuccessor newSuccessor = new NewSuccessor(localPeer, successor);
			component.triggerEvent(newSuccessor, notificationChannel);

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
	@MayTriggerEventTypes( { PerfectNetworkSendEvent.class, SetTimerEvent.class })
	public void handleStabilizeTimerSignal(StabilizeTimerSignal event) {
		logger.debug("STABILIZATION");

		if (successor != null) {
			GetPredecessorRequest request = new GetPredecessorRequest();
			PerfectNetworkSendEvent sendEvent = new PerfectNetworkSendEvent(
					request, successor);
			// send get predecessor request
			component.triggerEvent(sendEvent, pnSendChannel);
		}
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
			if (belongsTo(predecessorOfMySuccessor.getId(), localPeer,
					successor, IntervalBounds.OPEN_OPEN)) {
				successor = predecessorOfMySuccessor;

				// trigger newSuccessor
				NewSuccessor newSuccessor = new NewSuccessor(localPeer,
						successor);
				component.triggerEvent(newSuccessor, notificationChannel);
			}
		}

		Notify notify = new Notify(localPeer);
		PerfectNetworkSendEvent sendEvent = new PerfectNetworkSendEvent(notify,
				successor);
		// send notify
		component.triggerEvent(sendEvent, pnSendChannel);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(NewPredecessor.class)
	public void handleNotify(Notify event) {
		logger.debug("NOTIFY");

		Address potentialNewPredecessor = event.getFromPeer();

		if (predecessor == null
				|| belongsTo(potentialNewPredecessor.getId(), predecessor,
						localPeer, IntervalBounds.OPEN_OPEN)) {
			predecessor = potentialNewPredecessor;

			// trigger newPredecessor
			NewPredecessor newPredecessor = new NewPredecessor(localPeer,
					predecessor);
			component.triggerEvent(newPredecessor, notificationChannel);
		}

		if (successor == null) {
			successor = potentialNewPredecessor;

			// trigger newSuccessor
			NewSuccessor newSuccessor = new NewSuccessor(localPeer, successor);
			component.triggerEvent(newSuccessor, notificationChannel);

			// set the stabilization timer
			StabilizeTimerSignal signal = new StabilizeTimerSignal();
			timerHandler.setTimer(signal, timerSignalChannel,
					stabilizationPeriod);
		}
	}

	// x belongs to (from, to)
	private boolean belongsTo(BigInteger x, Address fromAddress,
			Address toAddress, IntervalBounds bounds) {
		BigInteger from = fromAddress.getId();
		BigInteger to = toAddress.getId();

		BigInteger ny = modMinus(to, from);
		BigInteger nx = modMinus(x, from);

		switch (bounds) {
		case OPEN_OPEN:
			return ((from.equals(to) && !x.equals(from)) || (nx
					.compareTo(BigInteger.ZERO) > 0 && nx.compareTo(ny) < 0));
		case OPEN_CLOSED:
			return (from.equals(to) || (nx.compareTo(BigInteger.ZERO) > 0 && nx
					.compareTo(ny) <= 0));
		case CLOSED_OPEN:
			return (from.equals(to) || (nx.compareTo(BigInteger.ZERO) >= 0 && nx
					.compareTo(ny) < 0));
		case CLOSED_CLOSED:
			return ((from.equals(to) && x.equals(from)) || (nx
					.compareTo(BigInteger.ZERO) >= 0 && nx.compareTo(ny) <= 0));
		}
		return (from.equals(to) || (nx.compareTo(BigInteger.ZERO) > 0 && nx
				.compareTo(ny) <= 0));
	}

	private BigInteger modMinus(BigInteger x, BigInteger y) {
		return ringSize.add(x).subtract(y).mod(ringSize);
	}
}
