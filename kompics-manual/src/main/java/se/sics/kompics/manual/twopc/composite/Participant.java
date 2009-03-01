package se.sics.kompics.manual.twopc.composite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import se.sics.kompics.manual.twopc.event.ReadResult;
import se.sics.kompics.manual.twopc.event.SelectAllOperation;
import se.sics.kompics.manual.twopc.event.Transaction;
import se.sics.kompics.manual.twopc.event.WriteOperation;
import se.sics.kompics.manual.twopc.event.WriteResult;
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
	public enum RowLock { READ_LOCK, WRITE_LOCK, NO_LOCK };
	
	
    private Address self;
    
    private Map<String,String> database = new HashMap<String,String>();
    
   /**
    *  LinkedHashMap is a Hash table and linked list implementation of the Map interface, 
    *  with predictable iteration order.
    *  See http://www.roseindia.net/javatutorials/linkedhashmap.shtml
    *  
    */
    private Map<String,LinkedHashMap<Operation,RowLock>> lockQueue = 
	   new HashMap<String,LinkedHashMap<Operation,RowLock>>();

    private Map<Integer,List<Operation>> activeTransactions = new 
    		HashMap<Integer,List<Operation>>();

    private Map<Integer,Map<String,String>> transactionUpdates = new 
	HashMap<Integer,Map<String,String>>();

    
	public Participant() {
		  subscribe(handleParticipantInit,control);
		  subscribe(handlePrepare,tpcPort);
		  subscribe(handleCommit,tpcPort);
		  subscribe(handleRollback,tpcPort);

		  subscribe(handleRead,tpcPort);
		  subscribe(handleWrite,tpcPort);
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

		    // Ops waiting on locks to be triggered when this transaction commits
		    List<Operation> triggerOps = new ArrayList<Operation>();
		    
		    for (Operation op : ops)
			{
				if (op instanceof ReadOperation)
				{
					String name = op.getName();
					String value = database.get(name);
					// Return value read to client in a new ReadOperation
					readResults.put(name, value);
					releaseLock(op);
					
					triggerOps.add(op);
				}
				else if (op instanceof WriteOperation)
				{
					database.put(op.getName(), op.getValue());
					
					triggerOps.add(op);
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

			// TODO: Release all locks for the transaction!
			// THEN, schedule next operations on queue for execution
			triggerNextOpOnLockQueue(triggerOps);

		}
	};

	Handler<Abort> handleRollback = new Handler<Abort>() {
		public void handle(Abort rollback) {
			int id = rollback.getTransactionId();
			logger.info("abort recvd for {}", id);
			activeTransactions.remove(id);
		}
	};
	
	Handler<ReadOperation> handleRead = new Handler<ReadOperation>() {
		public void handle(ReadOperation readOp) {
			int id = readOp.getTransactionId();
			logger.info("{}: read Op for {}", id, readOp.getName());
			boolean noLock = acquireLock(readOp, RowLock.READ_LOCK);
			
			if (noLock == true)
			{
				String value = database.get(readOp.getName());
				ReadResult r = new ReadResult(id,readOp.getName(), value);
				trigger(r, tpcPort);
			}
			else
			{
				// if I have the lock
				
//				if (id == )
//				{
//					// check if the data is in myUpdates first, if not then in database
//					
//				}
				
				logger.info("Placed on Lock Queue waiting for {}", readOp.getName());
			}
		}
	};
	
	Handler<WriteOperation> handleWrite = new Handler<WriteOperation>() {
		public void handle(WriteOperation writeOp) {
			int id = writeOp.getTransactionId();
			logger.info("{}: writeOp for {}", id, writeOp.getName());
			boolean noLock = acquireLock(writeOp, RowLock.WRITE_LOCK);
			
			if (noLock == true)
			{
				String value = database.get(writeOp.getName());
				WriteResult r = new WriteResult(id,writeOp.getName(), value);
				trigger(r, tpcPort);
			}
			else
			{
				logger.info("Waiting for write lock on {}", writeOp.getName());
			}
		}
	};
	
	
	private void addToLockQueue(String name, Operation op, RowLock lock)
	{
		LinkedHashMap<Operation,RowLock> ops = lockQueue.get(name);
		if (ops == null)
		{
			ops = new LinkedHashMap<Operation,RowLock>();
			lockQueue.put(name, ops);
		}
		ops.put(op,lock);
	}
	
	private boolean acquireLock(Operation op, RowLock lock)
	{
		boolean acquired = true;
		String name = op.getName();
		Map<Operation,RowLock> ops = lockQueue.get(name);

		
		if (ops != null)
		{
			if (lock == RowLock.READ_LOCK)
			{
				// scan for write locks
				for (RowLock l : ops.values())
				{
					if (l == RowLock.WRITE_LOCK)
					{
						acquired = false;
						break;
					}
				}
			}
			else if (lock == RowLock.WRITE_LOCK)
			{
				if (ops.size() > 0)
				{
					acquired = false;
				}
				
			}
		}
		addToLockQueue(name, op, lock);
		return acquired;
	}
	
	private void releaseLock(Operation op)
	{
		int id = op.getTransactionId();
		String name = op.getName();
		
		LinkedHashMap<Operation,RowLock> locks = lockQueue.get(name);
		
		boolean foundLock = false;
		for (Operation o : locks.keySet())
		{
			if (o == op)
			{
				foundLock = true;
				locks.remove(op);
			}
		}
		
		if (foundLock == false)
		{
			throw new IllegalStateException("Couldn't find lock for operation" + op.getName());
		}
	}

	private void triggerNextOpOnLockQueue(List<Operation> listOps)
	{
		for (Operation op : listOps)
		{
			LinkedHashMap<Operation,RowLock> locks = lockQueue.get(op.getName());
			if (locks.size() > 0)
			{
				// TODO: for values, locks are ordered, not sure about keys
				Iterator<Operation> it = locks.keySet().iterator(); 
				if (it.hasNext())
				{
					Operation nextOp = it.next();
					if (nextOp instanceof ReadOperation)
					{
						String value = database.get(nextOp.getName());
						ReadResult r = new ReadResult(nextOp.getTransactionId(), 
								nextOp.getName(), value);
						trigger(r, tpcPort);						
					}
					else if (nextOp instanceof WriteOperation)
					{
						String value = database.get(nextOp.getName());
						WriteResult r = new WriteResult(nextOp.getTransactionId(),
								nextOp.getName(), value);
						trigger(r, tpcPort);
					}
	
				}
			}
		}
	}
}
