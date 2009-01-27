package se.sics.kompics;

import java.util.ArrayDeque;

public abstract class Response extends Event {

	private ArrayDeque<Channel<?>> channelStack;
	
	protected Response(Request request) {
		channelStack = request.channelStack;
	}

	@Override
	void forwardedBy(Channel<?> channel) {
		channelStack.pop();
	}

	@Override
	Channel<?> getTopChannel() {
		return channelStack.peek();
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		Response response = (Response) super.clone();
		response.channelStack = channelStack.clone();
		return response;
	}
}
