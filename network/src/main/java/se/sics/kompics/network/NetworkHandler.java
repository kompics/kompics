package se.sics.kompics.network;

import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;

import se.sics.kompics.network.events.NetworkDeliverEvent;

public class NetworkHandler extends IoHandlerAdapter {

	private NetworkComponent networkComponent;

	public NetworkHandler(NetworkComponent networkComponent) {
		super();
		this.networkComponent = networkComponent;
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause)
			throws Exception {
		Address address = (Address) session.getAttribute("address");

		if (address != null)
			System.out.println("Problems with "
					+ (Transport) session.getAttribute("protocol")
					+ " connection to " + address + ": ");
		cause.printStackTrace();
	}

	@Override
	public void messageReceived(IoSession session, Object message)
			throws Exception {
		super.messageReceived(session, message);
		Transport protocol = (Transport) session.getAttribute("protocol");

		System.out.println("Message received from: "
				+ session.getRemoteAddress());
		networkComponent
				.deliverMessage((NetworkDeliverEvent) message, protocol);
	}

	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		super.messageSent(session, message);
		System.out.println("Message sent to: " + session.getRemoteAddress());
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		super.sessionClosed(session);
		System.out.println("Connection closed to: "
				+ session.getRemoteAddress());
	}

	@Override
	public void sessionOpened(IoSession session) throws Exception {
		super.sessionOpened(session);
		System.out.println("Connection opened to: "
				+ session.getRemoteAddress());
	}
}
