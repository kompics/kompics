package se.sics.kompics.manual.twopc.event;

import java.io.Serializable;

import se.sics.kompics.Event;

public class ReadResult extends Event implements Serializable
{
	private static final long serialVersionUID = 35345677289006L;

	protected final int transactionId;
	
	protected final String name;
	
	protected final String value;
	
	public ReadResult(int id, String name, String value) {
		this.transactionId = id;
		this.name = name;
		this.value = value;
	}
	
	public int getTransactionId() {
		return transactionId;
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getValue() {
		return value;
	}
}
