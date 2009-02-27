package se.sics.kompics.manual.twopc.event;



public class ReadOperation	extends Operation 
{
	protected static final long serialVersionUID = -3027574709983565623L;

	protected final String name;
	
	protected final String value;

	public ReadOperation(int id, String name) {
		this(id, name, null);
	}

	public ReadOperation(int id, String name, String value) {
		super(id);
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}
	public String getValue() {
		return value;
	}
}
