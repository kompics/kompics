package se.sics.kompics.core.sched;

import org.apache.log4j.BasicConfigurator;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.Factory;
import se.sics.kompics.api.FaultEvent;
import se.sics.kompics.api.Kompics;

/**
 * 
 * Test the KOMPICS scheduler for priority.
 * 
 * @author cosmin
 * 
 */
public class SchedulerPriorityMain extends ComponentTest {

	private Kompics kompics;

	private Component component1;

	private Component component2;

	private Channel channel11;

	private Channel channel12;

	private Channel channel13;

	private Channel channel21;

	private Channel channel22;

	private Channel channel23;

	public static void main(String args[]) throws Exception {
		new SchedulerPriorityMain().run();
	}

	public void run() throws Exception {
		BasicConfigurator.configure();

		kompics = new Kompics(1, 0);
		Component boot = kompics.getBootstrapComponent();

		Factory factory = boot.createFactory(TestPriorityComponent.class
				.getName());

		Channel faultChannel = boot.createChannel();
		faultChannel.addEventType(FaultEvent.class);

		channel11 = boot.createChannel();
		channel12 = boot.createChannel();
		channel13 = boot.createChannel();
		channel21 = boot.createChannel();
		channel22 = boot.createChannel();
		channel23 = boot.createChannel();
		channel11.addEventType(TestEvent.class);
		channel12.addEventType(TestEvent.class);
		channel13.addEventType(TestEvent.class);
		channel21.addEventType(TestEvent.class);
		channel22.addEventType(TestEvent.class);
		channel23.addEventType(TestEvent.class);

		component1 = factory.createComponent(faultChannel, channel11,
				channel12, channel13);
		component2 = factory.createComponent(faultChannel, channel21,
				channel22, channel23);

		component1.subscribe(channel21, "handleTestEvent");
		component1.subscribe(channel22, "handleTestEvent");
		component1.subscribe(channel23, "handleTestEvent");
		component2.subscribe(channel11, "handleTestEvent");
		component2.subscribe(channel12, "handleTestEvent");
		component2.subscribe(channel13, "handleTestEvent");

		component1.subscribe(faultChannel, "handleFaultEvent");

		component1.triggerEvent(new TestEvent(), channel12);

		// try {
		// Thread.sleep(2000);
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
	}
}
