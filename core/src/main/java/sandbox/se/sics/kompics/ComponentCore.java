package sandbox.se.sics.kompics;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ComponentCore implements Component {

	/* outside ports */
	private HashMap<Class<? extends PortType>, PortCore<? extends PortType>> positivePorts;
	private HashMap<Class<? extends PortType>, PortCore<? extends PortType>> negativePorts;

	private PortCore<ControlPort> positiveControl, negativeControl;

	ComponentCore parent;

	private static ThreadLocal<ComponentCore> parentThreadLocal = new ThreadLocal<ComponentCore>();

	private List<ComponentCore> children;

	ComponentDefinition component;

	private Scheduler scheduler;

	int wid;

	public ComponentCore(ComponentDefinition componentDefinition) {
		this.positivePorts = new HashMap<Class<? extends PortType>, PortCore<? extends PortType>>();
		this.negativePorts = new HashMap<Class<? extends PortType>, PortCore<? extends PortType>>();
		this.parent = parentThreadLocal.get();
		this.component = componentDefinition;
	}

	public Positive<ControlPort> getControl() {
		return positiveControl;
	}

	@SuppressWarnings("unchecked")
	public <P extends PortType> Negative<P> getNegative(Class<P> portType) {
		Negative<P> port = (Negative<P>) negativePorts.get(portType);
		if (port == null)
			throw new RuntimeException(component + " has no negative "
					+ portType.getCanonicalName());
		return port;
	}

	@SuppressWarnings("unchecked")
	public <P extends PortType> Positive<P> getPositive(Class<P> portType) {
		Positive<P> port = (Positive<P>) positivePorts.get(portType);
		if (port == null)
			throw new RuntimeException(component + " has no positive "
					+ portType.getCanonicalName());
		return port;
	}

	<P extends PortType> Negative<P> createNegativePort(Class<P> portType) {
		PortCore<P> negativePort = new PortCore<P>(false, PortType
				.getPortType(portType), this);
		PortCore<P> positivePort = new PortCore<P>(true, PortType
				.getPortType(portType), parent);

		negativePort.setPair(positivePort);
		positivePort.setPair(negativePort);

		Positive<?> existing = positivePorts.put(portType, positivePort);
		if (existing != null)
			throw new RuntimeException("Cannot create multiple negative "
					+ portType.getCanonicalName());
		return negativePort;
	}

	<P extends PortType> Positive<P> createPositivePort(Class<P> portType) {
		PortCore<P> negativePort = new PortCore<P>(false, PortType
				.getPortType(portType), parent);
		PortCore<P> positivePort = new PortCore<P>(true, PortType
				.getPortType(portType), this);

		negativePort.setPair(positivePort);
		positivePort.setPair(negativePort);

		Negative<?> existing = negativePorts.put(portType, negativePort);
		if (existing != null)
			throw new RuntimeException("Cannot create multiple positive "
					+ portType.getCanonicalName());
		return positivePort;
	}

	Negative<ControlPort> createControlPort() {
		negativeControl = new PortCore<ControlPort>(false, PortType
				.getPortType(ControlPort.class), this);
		positiveControl = new PortCore<ControlPort>(true, PortType
				.getPortType(ControlPort.class), parent);

		positiveControl.setPair(negativeControl);
		negativeControl.setPair(positiveControl);

		negativeControl.doSubscribe(handleStart);
		negativeControl.doSubscribe(handleStop);

		return negativeControl;
	}

	Component doCreate(Class<? extends ComponentDefinition> definition) {
		// create an instance of the implementing component type
		ComponentDefinition component;
		try {
			parentThreadLocal.set(this);
			component = definition.newInstance();
			ComponentCore child = component.getComponentCore();
			child.setScheduler(scheduler);
			if (children == null) {
				children = new LinkedList<ComponentCore>();
			}
			children.add(child);

			return child;
		} catch (InstantiationException e) {
			throw new RuntimeException("Cannot create component "
					+ definition.getCanonicalName(), e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Cannot create component "
					+ definition.getCanonicalName(), e);
		}
	}

	<P extends PortType> Channel<P> doConnect(Positive<P> positive,
			Negative<P> negative) {
		PortCore<P> positivePort = (PortCore<P>) positive;
		PortCore<P> negativePort = (PortCore<P>) negative;
		ChannelCore<P> channel = new ChannelCore<P>(positivePort, negativePort,
				negativePort.getPortType());

		positivePort.addChannel(channel);
		negativePort.addChannel(channel);

		return channel;
	}

	/* === SCHEDULING === */

	private AtomicInteger workCount = new AtomicInteger(0);

	private SpinlockQueue<PortCore<?>> readyPorts = new SpinlockQueue<PortCore<?>>();

	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	void workReceived(PortCore<?> port, int wid) {
		readyPorts.offer(port);

		int wc = workCount.getAndIncrement();
		// System.err.println(component + ".workReceived: " + wc + " -> " + (wc
		// + 1));
		if (wc == 0) {
			Scheduler.scheduler.schedule(this, wid);
		}
	}

	void execute(int wid) {
		this.wid = wid;
		// 1. pick a port with a non-empty event queue
		// 2. execute the first event
		// 3. make component ready

		PortCore<?> nextPort = readyPorts.poll();

		Work work = nextPort.pickWork();
		executeWork(work);

		int wc = workCount.decrementAndGet();
		// System.err.println(component + ".schedule: " + (wc + 1) + " -> " +
		// wc);
		if (wc > 0) {
			Scheduler.scheduler.schedule(this, wid);
		}
	}

	@SuppressWarnings("unchecked")
	private void executeWork(Work work) {
		try {
			((Handler<Event>) work.getHandler()).handle(work.getEvent());
		} catch (Throwable throwable) {
			handleFault(throwable);
		}

	}

	void handleFault(Throwable throwable) {
		if (parent != null) {
			negativeControl.doTrigger(new Fault(throwable), wid);
		} else {
			StackTraceElement[] stackTrace = throwable.getStackTrace();
			Kompics.logger.error("Kompics isolated fault: {}", throwable
					.getMessage());
			do {
				for (int i = 0; i < stackTrace.length; i++) {
					Kompics.logger.error("    {}", stackTrace[i]);
				}
				throwable = throwable.getCause();
				if (throwable != null) {
					stackTrace = throwable.getStackTrace();
					Kompics.logger.error("Caused by: {}: {}", throwable,
							throwable.getMessage());
				}
			} while (throwable != null);
		}
	}

	/* === LIFECYCLE === */

	Handler<Start> handleStart = new Handler<Start>() {
		public void handle(Start event) {
			// System.err.println(component + " defaultStart");
			if (children != null) {
				for (ComponentCore child : children) {
					// start child
					((PortCore<ControlPort>) child.getControl()).doTrigger(
							Start.event, wid);
				}
			}
		}
	};

	Handler<Stop> handleStop = new Handler<Stop>() {
		public void handle(Stop event) {
			// System.err.println(component + " defaultStop");
			if (children != null) {
				for (ComponentCore child : children) {
					// stop child
					((PortCore<ControlPort>) child.getControl()).doTrigger(
							Stop.event, wid);
				}
			}
		}
	};
}
