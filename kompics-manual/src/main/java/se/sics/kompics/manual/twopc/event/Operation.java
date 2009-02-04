package se.sics.kompics.manual.twopc.event;

import se.sics.kompics.Event;

public abstract class Operation extends Event
{
	public enum OpType {READ, WRITE};	

	protected final OpType opType;

	protected int id;
	
	public Operation(int id, OpType opType) {
		this.id = id;
		this.opType = opType;
	}
	
	public int getId() {
		return id;
	}
	
	public OpType getOpType() {
		return opType;
	}
	
	public abstract String getName();
	
	public abstract String getValue();
}
