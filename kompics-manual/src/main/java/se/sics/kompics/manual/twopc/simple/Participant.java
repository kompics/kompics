package se.sics.kompics.manual.twopc.simple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.manual.twopc.event.Commit;
import se.sics.kompics.manual.twopc.event.Operation;
import se.sics.kompics.manual.twopc.event.ParticipantInit;
import se.sics.kompics.manual.twopc.event.Prepare;
import se.sics.kompics.manual.twopc.event.ReadOperation;
import se.sics.kompics.manual.twopc.event.RollbackTransaction;
import se.sics.kompics.manual.twopc.event.Transaction;
import se.sics.kompics.network.Network;

/**
 * 
 */
public class Participant extends ComponentDefinition {

	Positive<Network> net= positive(Network.class);

	public class LogEntry
	{
		private long transactionId;
		private Operation operation;
		
		public LogEntry(long index, Operation operation) {
			super();
			this.transactionId = index;
			this.operation = operation;
		}
		
		public long getTransactionId() {
			return transactionId;
		}
		
		public Operation getOperation() {
			return operation;
		}		
	}
	
	
	
	List<LogEntry> redoLog = new ArrayList<LogEntry>();
	List<LogEntry> undoLog = new ArrayList<LogEntry>();
	
	private long redoLogIndex=0;
	private long undoLogIndex=0;
	
	private int participantId;
	
    private Address self;
    private Address coordinatorAddress;
    
    private Map<String,String> database = new HashMap<String,String>();

    private Map<Integer,Map<String,String>> outstandingTransactions = new 
    		HashMap<Integer,Map<String,String>>();

    
	public Participant() {
		  subscribe(handleParticipantInit,control);
		  subscribe(handlePrepare,net);
		  subscribe(handleCommit,net);
		  subscribe(handleRollback,net);
	}
	
	Handler<ParticipantInit> handleParticipantInit = new Handler<ParticipantInit>() {
		public void handle(ParticipantInit init) {
			participantId = init.getId();
			coordinatorAddress = init.getCoordinatorAddress();
			self = init.getSelf();
		}
	};

	Handler<Prepare> handlePrepare = new Handler<Prepare>() {
		public void handle(Prepare prepare) {
			Transaction t = prepare.getTrans();
			int id = t.getId();
			
			List<Operation> ops = t.getOperations();
			Map<String,String> entries = new HashMap<String,String>();
			for (Operation op : ops)
			{
				entries.put(op.getName(), op.getValue());
			}
			outstandingTransactions.put(id, entries);
			
			trigger(new Commit(id, self, prepare.getSource()), net);
		}
	};
	
	Handler<Commit> handleCommit = new Handler<Commit>() {
		public void handle(Commit commit) {
			int tId = commit.getTransactionId();
			
			// copy from outstanding transactions
			
		}
	};

	Handler<RollbackTransaction> handleRollback = new Handler<RollbackTransaction>() {
		public void handle(RollbackTransaction rollback) {
			int tId = rollback.getTransactionId();
			
		}
	};

}
