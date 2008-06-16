package se.sics.kompics.p2p.son.ps;

import java.math.BigInteger;
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

	@ComponentInitializeMethod("chord-ring.properties")
	public void init(Properties properties, Address localPeer) {
		stabilizationPeriod = Long.parseLong(properties
				.getProperty("stabilization.period"));
		int log2RingSize = Integer.parseInt(properties
				.getProperty("log2.ring.size"));
		ringSize = new BigInteger("2").pow(log2RingSize);

		this.localPeer = localPeer;
	}

	@EventHandlerMethod
	@MayTriggerEventTypes( { NewSuccessor.class, NewPredecessor.class,
			JoinRingCompleted.class })
	public void handleCreateRing(CreateRing event) {
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
	}

	@EventHandlerMethod
	@MayTriggerEventTypes( { PerfectNetworkSendEvent.class,
			NewPredecessor.class })
	public void handleJoinRing(JoinRing event) {
		predecessor = null;

		FindSuccessorRequest request = new FindSuccessorRequest(localPeer);
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

	}

	@EventHandlerMethod
	@MayTriggerEventTypes( { NewSuccessor.class, JoinRingCompleted.class })
	public void handleFindSuccessorResponse(FindSuccessorResponse event) {
		successor = event.getSuccessor();

		// trigger newSuccessor
		NewSuccessor newSuccessor = new NewSuccessor(localPeer, successor);
		component.triggerEvent(newSuccessor, notificationChannel);

		// trigger JoinRingCompleted
		JoinRingCompleted joinRingCompleted = new JoinRingCompleted(localPeer);
		component.triggerEvent(joinRingCompleted, notificationChannel);

		// set the stabilization timer
		StabilizeTimerSignal signal = new StabilizeTimerSignal();
		timerHandler.setTimer(signal, timerSignalChannel, stabilizationPeriod);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes( { PerfectNetworkSendEvent.class, SetTimerEvent.class })
	public void handleStabilizeTimerSignal(StabilizeTimerSignal event) {
		GetPredecessorRequest request = new GetPredecessorRequest();
		PerfectNetworkSendEvent sendEvent = new PerfectNetworkSendEvent(
				request, successor);
		// send get predecessor request
		component.triggerEvent(sendEvent, pnSendChannel);

		// reset the stabilization timer
		timerHandler.setTimer(event, timerSignalChannel, stabilizationPeriod);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(PerfectNetworkSendEvent.class)
	public void handleGetPredecessorRequest(GetPredecessorRequest event) {
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
		Address predecessorOfMySuccessor = event.getPredecessor();
		if (belongsTo(predecessorOfMySuccessor, localPeer, successor)) {
			successor = predecessorOfMySuccessor;

			// trigger newSuccessor
			NewSuccessor newSuccessor = new NewSuccessor(localPeer, successor);
			component.triggerEvent(newSuccessor, notificationChannel);
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
		Address potentialNewPredecessor = event.getFromPeer();

		if (predecessor == null
				|| belongsTo(potentialNewPredecessor, predecessor, localPeer)) {
			predecessor = potentialNewPredecessor;

			// trigger newPredecessor
			NewPredecessor newPredecessor = new NewPredecessor(localPeer,
					predecessor);
			component.triggerEvent(newPredecessor, notificationChannel);
		}
	}

	// x belongs to (from, to)
	private boolean belongsTo(Address xAddress, Address fromAddress,
			Address toAddress) {
		BigInteger x = xAddress.getId();
		BigInteger from = fromAddress.getId();
		BigInteger to = toAddress.getId();

		BigInteger ny = modeMinus(to, from);
		BigInteger nx = modeMinus(x, from);

		return ((from.equals(to) && !x.equals(from)) || (nx
				.compareTo(BigInteger.ZERO) > 0 && nx.compareTo(ny) < 0));
	}

	private BigInteger modeMinus(BigInteger from, BigInteger to) {
		return ringSize.add(from).subtract(to).mod(ringSize);
	}
}
