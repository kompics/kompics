package se.sics.kompics.manual.twopc.event;


public class WriteOperation extends ReadOperation {
	
	private static final long serialVersionUID = -9026656367914756590L;

	public WriteOperation(int id, String name, String value) {
		super(id, name, value);
	}
	
	public void setValue(String value)
	{
		this.value = value;
	}
}
