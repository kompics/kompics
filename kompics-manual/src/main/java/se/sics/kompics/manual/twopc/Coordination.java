package se.sics.kompics.manual.twopc;

import se.sics.kompics.PortType;
import se.sics.kompics.manual.twopc.event.Abort;
import se.sics.kompics.manual.twopc.event.BeginTransaction;
import se.sics.kompics.manual.twopc.event.Commit;
import se.sics.kompics.manual.twopc.event.CommitTransaction;
import se.sics.kompics.manual.twopc.event.Operation;
import se.sics.kompics.manual.twopc.event.Prepare;
import se.sics.kompics.manual.twopc.event.RollbackTransaction;
import se.sics.kompics.manual.twopc.event.TransResult;

public final class Coordination extends PortType {
	{
		positive(TransResult.class);
		negative(BeginTransaction.class);
		negative(Operation.class);		
		negative(CommitTransaction.class);
		negative(RollbackTransaction.class);
		
		positive(Prepare.class);
		positive(Commit.class);
		positive(Abort.class);
	}
}
