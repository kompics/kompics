package se.sics.kompics.p2p.chord;

import java.util.ArrayList;
import java.util.HashMap;

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
import se.sics.kompics.p2p.chord.events.ChordLookupRequest;
import se.sics.kompics.p2p.chord.events.ChordResponsibility;
import se.sics.kompics.p2p.chord.events.CreateRing;
import se.sics.kompics.p2p.chord.events.GetChordNeighborsRequest;
import se.sics.kompics.p2p.chord.events.GetChordNeighborsResponse;
import se.sics.kompics.p2p.chord.events.GetChordResponsibility;
import se.sics.kompics.p2p.chord.events.JoinRing;
import se.sics.kompics.p2p.chord.events.LeaveRing;
import se.sics.kompics.p2p.chord.ring.events.JoinRingCompleted;
import se.sics.kompics.p2p.chord.ring.events.NewPredecessor;
import se.sics.kompics.p2p.chord.ring.events.NewSuccessorList;
import se.sics.kompics.p2p.chord.router.FingerTableView;
import se.sics.kompics.p2p.chord.router.events.NewFingerTable;

/**
 * The <code>Chord</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public class Chord {

	private Logger logger;

	private final Component component;

	private Component ringComponent, routerComponent;

	// Chord channels
	private Channel requestChannel, notificationChannel;

	private Address localPeer;

	private Address predecessor;

	private ArrayList<Address> successorListView;

	private FingerTableView fingerTableView;

	public Chord(Component component) {
		this.component = component;
	}

	@ComponentCreateMethod
	public void create(Channel requestChannel, Channel notificationChannel) {
		this.requestChannel = requestChannel;
		this.notificationChannel = notificationChannel;

		Channel ringRouterChannel = component.createChannel(
				ChordLookupRequest.class, NewSuccessorList.class,
				NewPredecessor.class);

		ringComponent = component.createComponent(
				"se.sics.kompics.p2p.chord.ring.ChordRing", component
						.getFaultChannel(), requestChannel,
				notificationChannel, ringRouterChannel);

		routerComponent = component.createComponent(
				"se.sics.kompics.p2p.chord.router.ChordIterativeRouter",
				component.getFaultChannel(), requestChannel,
				notificationChannel, ringRouterChannel);

		component.subscribe(requestChannel, "handleGetChordNeighborsRequest");
		component.subscribe(requestChannel, "handleGetChordResponsibility");

		component.subscribe(notificationChannel, "handleNewSuccessorList");
		component.subscribe(notificationChannel, "handleNewPredecessor");
		component.subscribe(notificationChannel, "handleNewFingerTable");
	}

	@ComponentInitializeMethod
	public void initialize(Address localPeer) {
		this.localPeer = localPeer;

		logger = LoggerFactory.getLogger(getClass().getName() + "@"
				+ localPeer.getId());

		ringComponent.initialize(localPeer);
		routerComponent.initialize(localPeer);
	}

	@ComponentShareMethod
	public ComponentMembrane share(String name) {
		HashMap<Class<? extends Event>, Channel> map = new HashMap<Class<? extends Event>, Channel>();
		map.put(CreateRing.class, requestChannel);
		map.put(JoinRing.class, requestChannel);
		map.put(LeaveRing.class, requestChannel);
		map.put(ChordLookupRequest.class, requestChannel);
		map.put(GetChordResponsibility.class, requestChannel);
		map.put(GetChordNeighborsRequest.class, requestChannel);
		map.put(JoinRingCompleted.class, notificationChannel);
		map.put(ChordResponsibility.class, notificationChannel);
		map.put(GetChordNeighborsResponse.class, notificationChannel);
		ComponentMembrane membrane = new ComponentMembrane(component, map);
		return component.registerSharedComponentMembrane(name, membrane);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(GetChordNeighborsResponse.class)
	public void handleGetChordNeighborsRequest(GetChordNeighborsRequest event) {
		GetChordNeighborsResponse response = new GetChordNeighborsResponse(
				localPeer, (successorListView == null ? localPeer
						: successorListView.get(0)), predecessor,
				successorListView, fingerTableView);
		component.triggerEvent(response, notificationChannel);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(ChordResponsibility.class)
	public void handleGetChordResponsibility(GetChordResponsibility event) {
		ChordResponsibility response = new ChordResponsibility(predecessor
				.getId(), localPeer.getId());
		component.triggerEvent(response, notificationChannel);
	}

	@EventHandlerMethod
	public void handleNewSuccessorList(NewSuccessorList event) {
		logger.debug("NEW_SUCCESSOR_LIST {}", event.getSuccessorListView());

		successorListView = event.getSuccessorListView();
	}

	@EventHandlerMethod
	public void handleNewPredecessor(NewPredecessor event) {
		logger.debug("NEW_PREDECESSOR {}", event.getPredecessorPeer());

		predecessor = event.getPredecessorPeer();
	}

	@EventHandlerMethod
	public void handleNewFingerTable(NewFingerTable event) {
		logger.debug("NEW_FINGER_TABLE {}", event.getFingerTableView());

		fingerTableView = event.getFingerTableView();
	}

}
