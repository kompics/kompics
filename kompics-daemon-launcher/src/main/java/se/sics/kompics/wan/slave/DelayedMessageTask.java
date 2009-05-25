package se.sics.kompics.wan.slave;

import java.util.TimerTask;

import se.sics.kompics.network.Message;

public final class DelayedMessageTask extends TimerTask {

	private final Message message;
	private final Slave master;

	public DelayedMessageTask(Slave master, Message message) {
		super();
		this.master = master;
		this.message = message;
	}

	@Override
	public void run() {
		master.deliverDelayedMessage(message);
	}
}
