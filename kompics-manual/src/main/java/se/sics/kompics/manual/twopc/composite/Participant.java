package se.sics.kompics.manual.twopc.composite;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.manual.twopc.TwoPhaseCommit;
import se.sics.kompics.manual.twopc.event.Abort;
import se.sics.kompics.manual.twopc.event.Ack;
import se.sics.kompics.manual.twopc.event.Commit;
import se.sics.kompics.manual.twopc.event.Operation;
import se.sics.kompics.manual.twopc.event.ParticipantInit;
import se.sics.kompics.manual.twopc.event.Prepare;
import se.sics.kompics.manual.twopc.event.Prepared;
import se.sics.kompics.manual.twopc.event.ReadOperation;
import se.sics.kompics.manual.twopc.event.Transaction;
import se.sics.kompics.manual.twopc.event.WriteOperation;
import se.sics.kompics.timer.Timer;

/**
 * 
 */
public class Participant extends ComponentDefinition {

	Negative<TwoPhaseCommit> tpcPort = negative(TwoPhaseCommit.class);

	Positive<Timer> timer = positive(Timer.class);
	
	private static final Logger logger = LoggerFactory
	.getLogger(Participant.class);

/*
 * TODO: do proper rollback and logging
 * 
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
*/	
	
    private Address self;
    
    private Map<String,String> database = new HashMap<String,String>();

    private Map<Integer,List<Operation>> activeTransactions = new 
    		HashMap<Integer,List<Operation>>();

    
	public Participant() {
		  subscribe(handleParticipantInit,control);
		  subscribe(handlePrepare,tpcPort);
		  subscribe(handleCommit,tpcPort);
		  subscribe(handleRollback,tpcPort);
	}
	
	Handler<ParticipantInit> handleParticipantInit = new Handler<ParticipantInit>() {
		public void handle(ParticipantInit init) {
			self = init.getSelf();
		}
	};

	Handler<Prepare> handlePrepare = new Handler<Prepare>() {
		public void handle(Prepare prepare) {
			logger.info("prepare recvd.");
			Transaction t = prepare.getTrans();
			int id = t.getId();
			activeTransactions.put(id, t.getOperations());
			
			trigger(new Prepared(id, self, prepare.getSource()), tpcPort);
		}
	};
	
	Handler<Commit> handleCommit = new Handler<Commit>() {
		public void handle(Commit commit) {
			logger.info("commit recvd.");
			int transactionId = commit.getTransactionId();
			
			List<Operation> ops = activeTransactions.get(transactionId);
			
		    Map<String,String> responses = new HashMap<String,String>();
			// copy from active transactions to DB
			for (Operation op : ops)
			{
				if (op instanceof ReadOperation)
				{
					String name = op.getName();
					String value = database.get(name);
					// Return value read to client in a new ReadOperation
					responses.put(name, value);
				}
				else if (op instanceof WriteOperation)
				{
					database.put(op.getName(), op.getValue());					
				}
			}

			// Send Ack with responses
			trigger(new Ack(transactionId,responses,self,commit.getSource()),tpcPort);

		}
	};

	Handler<Abort> handleRollback = new Handler<Abort>() {
		public void handle(Abort rollback) {
			logger.info("abort recvd.");
			int id = rollback.getTransactionId();
			activeTransactions.remove(id);
		}
	};

}
