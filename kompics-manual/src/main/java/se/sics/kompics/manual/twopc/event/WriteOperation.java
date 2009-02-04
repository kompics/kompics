package se.sics.kompics.manual.twopc.event;


public class WriteOperation extends ReadOperation {
	
	public WriteOperation(int id, String name, String value) {
		super(id, Operation.OpType.WRITE, name, value);
	}
	
	public void setValue(String value)
	{
		this.value = value;
	}
}
