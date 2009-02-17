package se.sics.kompics.manual.example.composition;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;

public class Outer extends ComponentDefinition {

	Negative<Port> outerPort = negative(Port.class);
	Component inner;
	
	public Outer() {
		inner = create(Inner.class);
		subscribe(handleTestEvent,outerPort);
	}
	
	Handler<TestEvent> handleTestEvent = new Handler<TestEvent>() {
		public void handle(TestEvent t) {
			System.out.println("Received event at outer component");
			trigger(t, inner.getPositive(Port.class));
		}
	};
}
