package se.sics.kompics.manual.twopc.composite;

import se.sics.kompics.PortType;
import se.sics.kompics.manual.twopc.event.Abort;
import se.sics.kompics.manual.twopc.event.Ack;
import se.sics.kompics.manual.twopc.event.Commit;
import se.sics.kompics.manual.twopc.event.Prepare;
import se.sics.kompics.manual.twopc.event.Prepared;
import se.sics.kompics.manual.twopc.event.ReadOperation;
import se.sics.kompics.manual.twopc.event.ReadResult;
import se.sics.kompics.manual.twopc.event.WriteOperation;
import se.sics.kompics.manual.twopc.event.WriteResult;

public final class TwoPCPort extends PortType {
	{
		negative(Prepare.class);
		negative(Commit.class);
		negative(Abort.class);		

		negative(ReadOperation.class);
		negative(WriteOperation.class);
		positive(ReadOperation.class);
		positive(WriteOperation.class);
		
		positive(Prepared.class);
		positive(Abort.class);
		positive(Ack.class);
		
		negative(ReadResult.class);
		negative(WriteResult.class);
		positive(ReadResult.class);
		positive(WriteResult.class);
	}
}
