package org.component.chord.web.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

public final class Component extends ComponentDefinition {

	private static final Logger logger = LoggerFactory
			.getLogger(Component.class);

	private Positive<Network> network = positive(Network.class);
	private Positive<Timer> timer = positive(Timer.class);

	int state;

	public Component() {
		subscribe(handleInit, control);
		subscribe(handleMessage, network);
		subscribe(handleTimeout, timer);
	}

	private Handler<ComponentInit> handleInit = new Handler<ComponentInit>() {
		public void handle(ComponentInit init) {
			state = init.getAttribute();
		}
	};

	private Handler<ComponentMessage> handleMessage = new Handler<ComponentMessage>() {
		public void handle(ComponentMessage message) {
			logger.info("Message received");
		}
	};

	private Handler<ComponentTimeout> handleTimeout = new Handler<ComponentTimeout>() {
		public void handle(ComponentTimeout timeout) {
			logger.info("Timeout received");
		}
	};
}
