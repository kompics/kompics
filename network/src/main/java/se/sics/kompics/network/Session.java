package se.sics.kompics.network;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.network.events.Message;

public class Session implements IoFutureListener<IoFuture> {

	private static final Logger logger = LoggerFactory.getLogger(Session.class);

	private IoConnector ioConnector;

	private IoSession ioSession;

	private ConnectFuture connectFuture;

	private CloseFuture closeFuture;

	private Transport protocol;

	private InetSocketAddress remoteAddress;

	private LinkedList<Message> pendingMessages;

	private Lock lock;

	private boolean connected;

	private NetworkComponent component;
	private int retries;

	public Session(IoConnector ioConnector, Transport protocol,
			InetSocketAddress address, NetworkComponent component) {
		super();
		this.ioConnector = ioConnector;
		this.protocol = protocol;
		this.remoteAddress = address;
		this.pendingMessages = new LinkedList<Message>();

		this.component = component;
		this.retries = 0;
		lock = new ReentrantLock();
		connected = false;
	}

	public void operationComplete(IoFuture future) {
		ConnectFuture connFuture = (ConnectFuture) future;
		if (connFuture.isConnected()) {
			ioSession = future.getSession();

			// TODO Solve duplicate connection

			ioSession.setAttribute("address", remoteAddress);
			ioSession.setAttribute("protocol", protocol);

			logger.debug("Connected to {}", ioSession.getRemoteAddress());

			// send pending messages
			lock.lock();
			try {
				for (Message deliverEvent : pendingMessages) {
					logger.debug("Sending message {} to {}", deliverEvent
							.toString(), deliverEvent.getDestination());

					ioSession.write(deliverEvent);
				}
				connected = true;
			} finally {
				lock.unlock();
			}
		} else {
			if (retries < component.connectRetries) {
				retries++;
				logger.debug("Retrying {} connection to {}", protocol,
						remoteAddress);
				connectInit();
			} else {
				logger.debug("Dropping {} connection to {}", protocol,
						remoteAddress);
				// drop this session
				component.dropSession(this);
			}
		}
	}

	public void sendMessage(Message deliverEvent) {
		lock.lock();
		try {
			if (connected) {
				ioSession.write(deliverEvent);
			} else {
				pendingMessages.add(deliverEvent);
			}
		} finally {
			lock.unlock();
		}
	}

	public void connectInit() {
		connectFuture = ioConnector.connect(remoteAddress);
		connectFuture.addListener(this);
	}

	public void closeInit() {
		closeFuture = ioSession.close();
	}

	public void closeWait() {
		closeFuture.awaitUninterruptibly();
	}

	public InetSocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	public Transport getProtocol() {
		return protocol;
	}
}
