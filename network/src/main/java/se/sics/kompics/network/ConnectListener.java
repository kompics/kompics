package se.sics.kompics.network;

import java.util.LinkedList;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.network.events.NetworkDeliverEvent;

public class ConnectListener implements IoFutureListener<IoFuture> {

	private static final Logger logger = LoggerFactory
			.getLogger(ConnectListener.class);

	private NetworkComponent networkComponent;

	private Transport protocol;

	private Address address;

	private LinkedList<NetworkDeliverEvent> pendingMessages;

	public ConnectListener(NetworkComponent networkComponent,
			Transport protocol, Address address) {
		super();
		this.networkComponent = networkComponent;
		this.protocol = protocol;
		this.address = address;
		this.pendingMessages = new LinkedList<NetworkDeliverEvent>();
	}

	public void operationComplete(IoFuture future) {
		ConnectFuture connFuture = (ConnectFuture) future;
		if (connFuture.isConnected()) {
			IoSession session = future.getSession();
			boolean duplicate = false;

			if (networkComponent.alreadyConnected(address, protocol)) {
				// TODO Solve duplicate connection
				session = networkComponent.getSession(address, protocol);
				duplicate = true;
			}

			networkComponent.removePendingConnection(address);

			if (!duplicate) {
				networkComponent.addSession(address, session, protocol);
				session.setAttribute("address", address);
				session.setAttribute("protocol", protocol);
			}

			logger.debug("Connected to {}", session.getRemoteAddress());

			// send pending messages
			for (NetworkDeliverEvent deliverEvent : pendingMessages) {
				logger.debug("Sending message {} to {}", deliverEvent
						.getClass().getSimpleName(), address);

				session.write(deliverEvent);
			}
		} else {
			networkComponent.removePendingConnection(address);
			logger.debug("Failed to connect to {}", connFuture.getSession()
					.getRemoteAddress());
		}
	}

	public void addPendingMessage(NetworkDeliverEvent deliverEvent) {
		pendingMessages.add(deliverEvent);
	}
}
