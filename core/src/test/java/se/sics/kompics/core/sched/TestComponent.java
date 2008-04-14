package se.sics.kompics.core.sched;

import org.junit.Ignore;

import se.sics.kompics.api.Component;
import se.sics.kompics.api.annotation.ComponentType;
import se.sics.kompics.api.annotation.EventHandlerMethod;

@Ignore
@ComponentType
public class TestComponent {

	private Component component;

	private int events;

	private ComponentTest componentTest;

	public TestComponent(Component component) {
		super();
		this.component = component;
		this.componentTest = ComponentTest.componentTest;
		this.events = ComponentTest.eventCount;
		this.component.getClass();
	}

	@EventHandlerMethod
	public void handleTestEvent(TestEvent event) {
		events--;
		if (events == 0) {
			componentTest.componentDone();
		}
	}
}
