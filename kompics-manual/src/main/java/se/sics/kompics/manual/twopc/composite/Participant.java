package se.sics.kompics.manual.twopc.composite;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.manual.twopc.event.Abort;
import se.sics.kompics.manual.twopc.event.Ack;
import se.sics.kompics.manual.twopc.event.Commit;
import se.sics.kompics.manual.twopc.event.Operation;
import se.sics.kompics.manual.twopc.event.ParticipantInit;
import se.sics.kompics.manual.twopc.event.Prepare;
import se.sics.kompics.manual.twopc.event.Prepared;
import se.sics.kompics.manual.twopc.event.ReadOperation;
import se.sics.kompics.manual.twopc.event.SelectAllOperation;
import se.sics.kompics.manual.twopc.event.Transaction;
import se.sics.kompics.manual.twopc.event.WriteOperation;
import se.sics.kompics.timer.Timer;

/**
 * 
 */
public class Participant extends ComponentDefinition {

	Negative<TwoPCPort> tpcPort = negative(TwoPCPort.class);

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
			Transaction t = prepare.getTrans();
			int id = t.getId();
			logger.info("{}: prepare recvd", id);
			activeTransactions.put(id, t.getOperations());
			
			trigger(new Prepared(id, self, prepare.getSource()), tpcPort);
		}
	};
	
	Handler<Commit> handleCommit = new Handler<Commit>() {
		public void handle(Commit commit) {
			int transactionId = commit.getTransactionId();
			logger.info("{}: commit recvd", transactionId);
			
			List<Operation> ops = activeTransactions.get(transactionId);			
		    Map<String,String> readResults = new HashMap<String,String>();
			// copy from active transactions to DB
			for (Operation op : ops)
			{
				if (op instanceof ReadOperation)
				{
					String name = op.getName();
					String value = database.get(name);
					// Return value read to client in a new ReadOperation
					readResults.put(name, value);
				}
				else if (op instanceof WriteOperation)
				{
					database.put(op.getName(), op.getValue());					
				}
				else if (op instanceof SelectAllOperation)
				{
					readResults.putAll(database);
				}
				else
				{
					throw new IllegalStateException("Invalid operation type ");
				}
			}

			// Send Ack with responses
			trigger(new Ack(transactionId,readResults,self,commit.getSource()),tpcPort);
			activeTransactions.remove(transactionId);
		}
	};

	Handler<Abort> handleRollback = new Handler<Abort>() {
		public void handle(Abort rollback) {
			int id = rollback.getTransactionId();
			logger.info("abort recvd for {}", id);
			activeTransactions.remove(id);
		}
	};

}
