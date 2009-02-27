package se.sics.kompics.manual.twopc.event;

import java.io.Serializable;

public class SelectAllOperation extends Operation implements Serializable
{
	private static final long serialVersionUID = 343426473177289006L;

	public SelectAllOperation(int id) {
		super(id);
	}
	
	public int getTransactionId() {
		return transactionId;
	}

	@Override
	public String getName() {
		return "selectAll";
	}

	@Override
	public String getValue() {
		return "*";
	}
	
}
