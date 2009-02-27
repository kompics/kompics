package se.sics.kompics.manual.twopc.event;

import java.io.Serializable;

import se.sics.kompics.Event;

public abstract class Operation extends Event implements Serializable
{
	private static final long serialVersionUID = 3587226473177289006L;

	protected final int transactionId;
	
	public Operation(int id) {
		this.transactionId = id;
	}
	
	public int getTransactionId() {
		return transactionId;
	}
	
	public abstract String getName();
	
	public abstract String getValue();
}
