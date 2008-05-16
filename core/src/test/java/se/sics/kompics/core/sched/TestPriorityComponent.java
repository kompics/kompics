package se.sics.kompics.core.sched;

import org.junit.Ignore;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.FaultEvent;
import se.sics.kompics.api.Priority;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentType;
import se.sics.kompics.api.annotation.EventHandlerMethod;
import se.sics.kompics.api.annotation.MayTriggerEventTypes;

@Ignore
@ComponentType
public class TestPriorityComponent {

	private Component component;

	private Channel channel1;
	private Channel channel2;
	private Channel channel3;

	public TestPriorityComponent(Component component) {
		super();
		this.component = component;
	}

	@ComponentCreateMethod
	public void create(Channel channel1, Channel channel2, Channel channel3) {
		this.channel1 = channel1;
		this.channel2 = channel2;
		this.channel3 = channel3;
		System.out.println("CREATE");
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(TestEvent.class)
	public void handleTestEvent(TestEvent event) {
		String message = event.getMessage();
		if (message == null) {
			System.out.println("H1");
			component.triggerEvent(new TestEvent("L"), channel1, Priority.LOW);
			component.triggerEvent(new TestEvent("M"), channel2,
					Priority.MEDIUM);
			component.triggerEvent(new TestEvent("H"), channel3, Priority.HIGH);
			System.out.println("H2");
		} else {
			System.out.println("HANDLED: " + message);
		}
	}

	@EventHandlerMethod
	public void handleFaultEvent(FaultEvent event) {
		event.getThrowable().printStackTrace(System.err);
	}
}
