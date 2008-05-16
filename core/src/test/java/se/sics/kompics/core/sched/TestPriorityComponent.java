package se.sics.kompics.core.sched;

import org.apache.log4j.Logger;
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

	private final Logger logger = Logger.getLogger(TestPriorityComponent.class);

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
		logger.info("CREATE");
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(TestEvent.class)
	public void handleTestEvent(TestEvent event) {
		String message = event.getMessage();
		if (message == null) {
			logger.info("H1");
			component.triggerEvent(new TestEvent("L"), channel1, Priority.LOW);
			component.triggerEvent(new TestEvent("M"), channel2,
					Priority.MEDIUM);
			component.triggerEvent(new TestEvent("H"), channel3, Priority.HIGH);
			component.triggerEvent(new TestEvent("L"), channel1, Priority.LOW);
			component.triggerEvent(new TestEvent("M"), channel2,
					Priority.MEDIUM);
			component.triggerEvent(new TestEvent("H"), channel3, Priority.HIGH);
			logger.info("H2");
		} else {
			logger.info("HANDLED: " + message);
		}
	}

	@EventHandlerMethod
	public void handleFaultEvent(FaultEvent event) {
		event.getThrowable().printStackTrace(System.err);
	}
}
