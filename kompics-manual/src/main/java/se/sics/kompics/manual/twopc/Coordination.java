package se.sics.kompics.manual.twopc;

import se.sics.kompics.PortType;
import se.sics.kompics.manual.twopc.event.Abort;
import se.sics.kompics.manual.twopc.event.TransResult;
import se.sics.kompics.manual.twopc.event.Transaction;

public final class Coordination extends PortType {
	{
		positive(TransResult.class);
		negative(Transaction.class);
	}
}
