package se.sics.kompics.p2p.chord.router;

import java.math.BigInteger;
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
import se.sics.kompics.p2p.chord.events.FindSuccessorRequest;
import se.sics.kompics.p2p.chord.ring.events.StabilizeTimerSignal;
import se.sics.kompics.p2p.chord.router.events.FixFingers;
import se.sics.kompics.p2p.network.events.PerfectNetworkDeliverEvent;
import se.sics.kompics.p2p.network.events.PerfectNetworkSendEvent;
import se.sics.kompics.timer.TimerHandler;
import se.sics.kompics.timer.events.SetTimerEvent;

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
	private Channel requestChannel, responseChannel;

	// PerfectNetwork send channel
	private Channel pnSendChannel;

	private Channel timerSignalChannel;

	private TimerHandler timerHandler;

	private long fingerStabilizationPeriod;

	private int log2RingSize;

	private BigInteger ringSize;

	private Address localPeer;

	private FingerTable fingerTable;

	private int nextFingerToFix;
	
	public ChordIterativeRouter(Component component) {
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
		timerSignalChannel = component
				.createChannel(StabilizeTimerSignal.class);

		// use shared PerfectNetwork component
		ComponentMembrane pnMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.p2p.network.PerfectNetwork");
		pnSendChannel = pnMembrane.getChannel(PerfectNetworkSendEvent.class);
		Channel pnDeliverChannel = pnMembrane
				.getChannel(PerfectNetworkDeliverEvent.class);

//		component.subscribe(this.requestChannel, "handleCreateRing");
//		component.subscribe(this.requestChannel, "handleJoinRing");
//		component.subscribe(this.requestChannel,
//				"handleGetRingNeighborsRequest");
//		component.subscribe(pnDeliverChannel, "handleFindSuccessorRequest");
//		component.subscribe(pnDeliverChannel, "handleFindSuccessorResponse");
//		component.subscribe(pnDeliverChannel, "handleGetPredecessorRequest");
//		component.subscribe(pnDeliverChannel, "handleGetPredecessorResponse");
//		component.subscribe(pnDeliverChannel, "handleGetSuccessorListRequest");
//		component.subscribe(pnDeliverChannel, "handleGetSuccessorListResponse");
//		component.subscribe(pnDeliverChannel, "handleNotify");

		this.timerHandler = new TimerHandler(component, timerSetChannel);
		component.subscribe(timerSignalChannel, "handleFixFingers");
	}

	@ComponentInitializeMethod("chord-router.properties")
	public void init(Properties properties, Address localPeer) {
		fingerStabilizationPeriod = 1000 * Long.parseLong(properties
				.getProperty("finger.stabilization.period"));
		log2RingSize = Integer.parseInt(properties
				.getProperty("log2.ring.size"));
		ringSize = new BigInteger("2").pow(log2RingSize);

		this.localPeer = localPeer;

		fingerTable = new FingerTable(log2RingSize, localPeer);
		nextFingerToFix = 0;
		
		logger = LoggerFactory.getLogger(getClass().getName() + "@"
				+ localPeer.getId());
	}
	
	@EventHandlerMethod
	@MayTriggerEventTypes( { PerfectNetworkSendEvent.class, SetTimerEvent.class })
	public void handleFindSuccessorRequest(FindSuccessorRequest event) {
		logger.debug("FIND_SUCC_REQ");

	}	

	@EventHandlerMethod
	@MayTriggerEventTypes( { PerfectNetworkSendEvent.class, SetTimerEvent.class })
	public void handleFixFingers(FixFingers event) {
		logger.debug("FIX_FINGER");

		nextFingerToFix++;
		if (nextFingerToFix > log2RingSize) {
			nextFingerToFix = 1;
		}
		
		timerHandler.setTimer(event, timerSignalChannel, fingerStabilizationPeriod);

		// actually fix the finger now
		// finger[next] = findSuccessor(n plus 2^(next-1))
	}

}
