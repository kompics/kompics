package se.sics.kompics;


public abstract class ComponentDefinition {

	protected <P extends PortType> Negative<P> negative(Class<P> portType) {
		return core.createNegativePort(portType);
	}

	protected <P extends PortType> Positive<P> positive(Class<P> portType) {
		return core.createPositivePort(portType);
	}

	protected <P extends PortType> void trigger(Event event, Port<P> port) {
		((PortCore<P>) port).doTrigger(event, core.wid);
	}

	protected void expect(Filter<?>... filter) {
		// TODO
	}

	protected <E extends Event, P extends PortType> void subscribe(
			Handler<E> handler, Port<P> port) {
		((PortCore<P>) port).doSubscribe(handler);
	}

	protected <E extends Event, P extends PortType> void unsubscribe(
			Handler<E> handler, Port<P> port) {
		((PortCore<P>) port).doUnsubscribe(handler);
	}

	protected Component create(Class<? extends ComponentDefinition> definition) {
		return core.doCreate(definition);
	}

	protected <P extends PortType> Channel<P> connect(Positive<P> positive,
			Negative<P> negative) {
		return core.doConnect(positive, negative);
	}

	protected <P extends PortType> Channel<P> connect(Negative<P> negative,
			Positive<P> positive) {
		return core.doConnect(positive, negative);
	}

	// protected <E extends PortType> Channel<E> connect(Positive<E> p,
	// Negative<E> q, ChannelFilter filter) {
	// return null;
	// } TODO
	//
	// protected <E extends PortType> Channel<E> connect(Negative<E> p,
	// Positive<E> q, ChannelFilter filter) {
	// return null;
	// }

	protected Negative<ControlPort> control;

	/* === PRIVATE === */

	private ComponentCore core;

	protected ComponentDefinition() {
		core = new ComponentCore(this);
		control = core.createControlPort();
	}

	ComponentCore getComponentCore() {
		return core;
	}
}
