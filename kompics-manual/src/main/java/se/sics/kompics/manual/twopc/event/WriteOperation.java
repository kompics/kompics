package se.sics.kompics.manual.twopc.event;


public class WriteOperation extends Operation {
	
	private static final long serialVersionUID = -902665337914756590L;

	protected final String name;
	
	protected final String value;
	
	public WriteOperation(int id, String name, String value) {
		super(id);
		this.name = name;
		this.value = value;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public String getValue() {
		return value;
	}
	
}
