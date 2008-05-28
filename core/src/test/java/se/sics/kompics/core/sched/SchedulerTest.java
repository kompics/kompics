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
public class SchedulerTest extends ComponentTest {

	private Kompics kompics;

	private Component component1;

	private Component component2;

	private Channel channel1;

	private Channel channel2;

	public static final int CORES = 2;

	public static final int FAIRNESS = 3;

	public static final int EVENTS = 1000000;

	public static final int COMPONENTS = 2;

	private static AtomicInteger components;

	private Semaphore semaphore;

	private long before;

	private long after;

	@Before
	public void setUp() throws Exception {
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
		SchedulerTest.components = new AtomicInteger(COMPONENTS);

		channel1 = boot.createChannel(TestEvent.class);
		channel2 = boot.createChannel(TestEvent.class);

		component1.subscribe(channel1, "handleTestEvent");
		component2.subscribe(channel2, "handleTestEvent");
	}

	@After
	public void tearDown() throws Exception {
		kompics = null;
	}

	@Test
	@Ignore
	public void test1() {
		TestEvent[] events = new TestEvent[EVENTS];
		for (int i = 0; i < events.length; i++) {
			events[i] = new TestEvent();
		}

		before = System.currentTimeMillis();
		for (int i = 0; i < EVENTS; i++) {
			// if (i % 2 == 0) {
			component1.triggerEvent(events[i], channel2);
			// } else {
			// component2.triggerEvent(events[i], channel1);
			// }
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

	@Test
	public void test2() {
		TestEvent[] events = new TestEvent[EVENTS];
		for (int i = 0; i < events.length; i++) {
			events[i] = new TestEvent();
		}

		before = System.currentTimeMillis();
		for (int i = 0; i < EVENTS; i++) {
			if (i % 2 == 0) {
				component1.triggerEvent(events[i], channel2);
			} else {
				component2.triggerEvent(events[i], channel1);
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
		return new junit.framework.JUnit4TestAdapter(SchedulerTest.class);
	}

	public void componentDone() {
		int now = SchedulerTest.components.decrementAndGet();
		if (now == 0) {
			after = System.currentTimeMillis();
			semaphore.release();
		}
	}
}
