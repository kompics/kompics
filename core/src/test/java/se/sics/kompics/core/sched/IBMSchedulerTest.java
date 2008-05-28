package se.sics.kompics.core.sched;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

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
 * Test the KOMPICS scheduler for: mutual exclusion, FIFO channels, priority and
 * fairness
 * 
 * @author cosmin
 * 
 */
@Ignore
public class IBMSchedulerTest extends ComponentTest {

	private Kompics kompics;

	private Component component1;

	private Component component2;

	private Component component3;

	private Component component4;

	private Channel channel1;

	private Channel channel2;

	private Channel channel3;

	private Channel channel4;

	public static int CORES = 4;

	public static final int FAIRNESS = 10;

	public static final int EVENTS = 1000000;

	public static final int COMPONENTS = 4;

	private static AtomicInteger components;

	private Semaphore semaphore;

	private long before;

	private long after;

	@Before
	public void setUp() throws Exception {
		IBMSchedulerTest.CORES = Integer.parseInt(System.getProperty("cores",
				"4"));

		ComponentTest.componentTest = this;
		ComponentTest.eventCount = EVENTS / COMPONENTS;

		semaphore = new Semaphore(0);
		kompics = new Kompics(CORES, FAIRNESS);
		Component boot = kompics.getBootstrapComponent();

		Channel faultChannel = boot.createChannel(FaultEvent.class);

		component1 = boot.createComponent(TestComponent.class.getName(),
				faultChannel);
		component2 = boot.createComponent(TestComponent.class.getName(),
				faultChannel);
		component3 = boot.createComponent(TestComponent.class.getName(),
				faultChannel);
		component4 = boot.createComponent(TestComponent.class.getName(),
				faultChannel);
		IBMSchedulerTest.components = new AtomicInteger(COMPONENTS);

		channel1 = boot.createChannel(TestEvent.class);
		channel2 = boot.createChannel(TestEvent.class);
		channel3 = boot.createChannel(TestEvent.class);
		channel4 = boot.createChannel(TestEvent.class);

		component1.subscribe(channel1, "handleTestEvent");
		component2.subscribe(channel2, "handleTestEvent");
		component3.subscribe(channel3, "handleTestEvent");
		component4.subscribe(channel4, "handleTestEvent");
	}

	@After
	public void tearDown() throws Exception {
		kompics = null;
	}

	@Test
	public void test() {
		TestEvent[] events = new TestEvent[EVENTS];
		for (int i = 0; i < events.length; i++) {
			events[i] = new TestEvent();
		}

		before = System.currentTimeMillis();
		for (int i = 0; i < EVENTS; i++) {
			if (i % 4 == 0) {
				component1.triggerEvent(events[i], channel2);
			} else if (i % 4 == 1) {
				component2.triggerEvent(events[i], channel1);
			} else if (i % 4 == 2) {
				component3.triggerEvent(events[i], channel4);
			} else {
				component4.triggerEvent(events[i], channel3);
			}
		}
		// wait for components to execute
		boolean interrupted = false;
		do {
			try {
				semaphore.acquire();
				interrupted = false;
			} catch (InterruptedException e) {
				interrupted = true;
				continue;
			}
		} while (interrupted);
		// check duration
		System.out.println(EVENTS + " events on " + CORES + " cores and "
				+ COMPONENTS + " components, took " + (after - before)
				+ " milliseconds");
	}

	public static junit.framework.Test suite() {
		return new junit.framework.JUnit4TestAdapter(IBMSchedulerTest.class);
	}

	public void componentDone() {
		int now = IBMSchedulerTest.components.decrementAndGet();
		if (now == 0) {
			after = System.currentTimeMillis();
			semaphore.release();
		}
	}
}
