package se.sics.kompics.core.sched;

import org.apache.log4j.BasicConfigurator;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.FaultEvent;
import se.sics.kompics.api.Kompics;

/**
 * 
 * Test the KOMPICS scheduler for priority.
 * 
 * @author cosmin
 * 
 */
@Ignore
public class SchedulerPriorityTest extends ComponentTest {

	private Kompics kompics;

	private Component component1;

	private Component component2;

	private Channel channel11;

	private Channel channel12;

	private Channel channel13;

	private Channel channel21;

	private Channel channel22;

	private Channel channel23;

	@Before
	public void setUp() throws Exception {

		BasicConfigurator.configure();

		kompics = new Kompics(2, 0);
		Component boot = kompics.getBootstrapComponent();

		Channel faultChannel = boot.createChannel(FaultEvent.class);

		channel11 = boot.createChannel(TestEvent.class);
		channel12 = boot.createChannel(TestEvent.class);
		channel13 = boot.createChannel(TestEvent.class);
		channel21 = boot.createChannel(TestEvent.class);
		channel22 = boot.createChannel(TestEvent.class);
		channel23 = boot.createChannel(TestEvent.class);

		component1 = boot.createComponent(
				TestPriorityComponent.class.getName(), faultChannel, channel11,
				channel12, channel13);
		component2 = boot.createComponent(
				TestPriorityComponent.class.getName(), faultChannel, channel21,
				channel22, channel23);

		component1.subscribe(channel21, "handleTestEvent");
		component1.subscribe(channel22, "handleTestEvent");
		component1.subscribe(channel23, "handleTestEvent");
		component2.subscribe(channel11, "handleTestEvent");
		component2.subscribe(channel12, "handleTestEvent");
		component2.subscribe(channel13, "handleTestEvent");

		component1.subscribe(faultChannel, "handleFaultEvent");
	}

	@After
	public void tearDown() throws Exception {
		kompics = null;
	}

	@Test
	public void test() {
		component1.triggerEvent(new TestEvent(), channel12);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
