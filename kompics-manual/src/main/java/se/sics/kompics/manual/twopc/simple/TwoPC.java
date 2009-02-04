package se.sics.kompics.manual.twopc.simple;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.manual.twopc.Coordination;
import se.sics.kompics.manual.twopc.event.CoordinatorInit;
import se.sics.kompics.network.Network;

public class TwoPC extends ComponentDefinition {
	
	Component coordinator;
	Component participant;

	Negative<Coordination> inputCoordination = negative(Coordination.class);
	Positive<Coordination> childCoordination = positive(Coordination.class);

	Positive<Network> netPort = positive(Network.class);

	private int id;

	public TwoPC() {
		coordinator = create(Coordinator.class);
		participant = create(Participant.class);
		
		connect(inputCoordination,childCoordination);
		
		subscribe(handleCoordinatorInit, control);
	}
	
	Handler<CoordinatorInit> handleCoordinatorInit = new Handler<CoordinatorInit>() {
		public void handle(CoordinatorInit init) {
			id = init.getId();
			trigger(init,coordinator.getControl());
		}
	};

}
