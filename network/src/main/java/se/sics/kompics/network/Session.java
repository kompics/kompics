package se.sics.kompics.network;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.mina.common.CloseFuture;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.network.events.NetworkDeliverEvent;

public class Session implements IoFutureListener<IoFuture> {

	private static final Logger logger = LoggerFactory.getLogger(Session.class);

	private IoConnector ioConnector;

	private IoSession ioSession;

	private ConnectFuture connectFuture;

	private CloseFuture closeFuture;

	private Transport protocol;

	private InetSocketAddress remoteAddress;

	private LinkedList<NetworkDeliverEvent> pendingMessages;

	private Lock lock;

	private boolean connected;

	public Session(IoConnector ioConnector, Transport protocol,
			InetSocketAddress address) {
		super();
		this.ioConnector = ioConnector;
		this.protocol = protocol;
		this.remoteAddress = address;
		this.pendingMessages = new LinkedList<NetworkDeliverEvent>();

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
				for (NetworkDeliverEvent deliverEvent : pendingMessages) {
					logger.debug("Sending message {} to {}", deliverEvent
							.toString(), deliverEvent.getDestination());

					ioSession.write(deliverEvent);
				}
				connected = true;
			} finally {
				lock.unlock();
			}
		} else {
			logger.debug("Failed to connect to {}", connFuture.getSession()
					.getRemoteAddress());
		}
	}

	public void sendMessage(NetworkDeliverEvent deliverEvent) {
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
		// TODO can this be too late and lose the operationComplete call?
		connectFuture.addListener(this);
	}

	public void closeInit() {
		closeFuture = ioSession.close();
	}

	public void closeWait() {
		closeFuture.awaitUninterruptibly();
	}
}
