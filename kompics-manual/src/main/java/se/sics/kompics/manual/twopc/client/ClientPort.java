package se.sics.kompics.manual.twopc.client;

import se.sics.kompics.PortType;
import se.sics.kompics.manual.twopc.event.BeginTransaction;
import se.sics.kompics.manual.twopc.event.CommitTransaction;
import se.sics.kompics.manual.twopc.event.ReadOperation;
import se.sics.kompics.manual.twopc.event.ReadResult;
import se.sics.kompics.manual.twopc.event.RollbackTransaction;
import se.sics.kompics.manual.twopc.event.SelectAllOperation;
import se.sics.kompics.manual.twopc.event.TransResult;
import se.sics.kompics.manual.twopc.event.WriteOperation;
import se.sics.kompics.manual.twopc.event.WriteResult;

public final class ClientPort extends PortType {
	{
		negative(BeginTransaction.class);
		negative(ReadOperation.class);		
		negative(WriteOperation.class);
		negative(SelectAllOperation.class);
		
		negative(CommitTransaction.class);
		negative(RollbackTransaction.class);
		
		positive(TransResult.class);
		positive(ReadResult.class);
		positive(WriteResult.class);
	}
}
