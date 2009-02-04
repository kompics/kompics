package se.sics.kompics.manual.twopc.event;


public class ReadOperation	extends Operation 
{
	protected static final long serialVersionUID = -3027574709983565623L;

	protected final String name;
	
	protected String value;

	public ReadOperation(int id, String name) {
		this(id, Operation.OpType.READ, name);
	}
	
	public ReadOperation(int id, String name, String value) {
		this(id, Operation.OpType.READ, name, value);
	}

	protected ReadOperation(int id, Operation.OpType opType, String name) {
		this(id, opType, name, null);
	}

	protected ReadOperation(int id, Operation.OpType opType, String name, String value) {
		super(id, opType);
		this.name = name;
		this.value = value;
	}

	public OpType getOpType() {
		return opType;
	}
	public String getName() {
		return name;
	}
	public String getValue() {
		return value;
	}
}
