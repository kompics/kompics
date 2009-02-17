package se.sics.kompics.manual.example.composition;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;

public class Inner extends ComponentDefinition {
	Negative<Port> innerPort = negative(Port.class);
	
	public Inner() {
		subscribe(handleTestEvent,innerPort);
	}
	
	Handler<TestEvent> handleTestEvent = new Handler<TestEvent>() {
		public void handle(TestEvent t) {
			System.out.println("Received event at inner component");
		}
	};
}
