package se.sics.kompics.manual.twopc.simple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.manual.twopc.Coordination;
import se.sics.kompics.manual.twopc.event.Abort;
import se.sics.kompics.manual.twopc.event.Ack;
import se.sics.kompics.manual.twopc.event.BeginTransaction;
import se.sics.kompics.manual.twopc.event.Commit;
import se.sics.kompics.manual.twopc.event.CommitTransaction;
import se.sics.kompics.manual.twopc.event.CoordinatorInit;
import se.sics.kompics.manual.twopc.event.Operation;
import se.sics.kompics.manual.twopc.event.Prepare;
import se.sics.kompics.manual.twopc.event.ReadOperation;
import se.sics.kompics.manual.twopc.event.RollbackTransaction;
import se.sics.kompics.manual.twopc.event.TransResult;
import se.sics.kompics.manual.twopc.event.Transaction;
import se.sics.kompics.manual.twopc.event.WriteOperation;
import se.sics.kompics.network.Network;

/**
 * <h2>Two-phase-commit protocol</h2>
 * <h3>(Phase 1) Commit-request phase</h3>
 * <ul>
 * <li>1. The coordinator sends a query to commit message to all participants.</li>
 * <li>2. The participants execute the transaction up to the point where they will 
 *        be asked to commit. They each write an entry to their undo log and an entry to their redo log.</li>
 * <li>3. Each participant replies with an agreement message if the transaction succeeded, 
 *    or an abort message if the transaction failed.</li>
 * <li>4. The coordinator waits until it has a message from each participant.</li>
 * </ul>
 * 
 * <h3>(Phase 2) Commit phase</h3>
 *  If the coordinator received an agreement message from all participants during the commit-request phase:
 * <ul> 
 * <li>1. The coordinator writes a commit record into its log.</li>
 * <li>2. The coordinator sends a commit message to all the participants.</li>
 * <li>3. Each participant completes the operation, and releases all the locks and resources held 
 * 	  during the transaction.</li>
 * <li>4. Each participant sends an acknowledgement to the coordinator.</li>
 * <li>5. The coordinator completes the transaction when acknowledgements have been received.</li>
 * </ul>
 * If any participant sent an abort message during the commit-request phase:
 * <ul>
 * <li>1. The coordinator sends an rollback message to all the participants.</li>
 * <li>2. Each participant undoes the transaction using the undo log, and releases the 
 *    resources and locks held during the transaction.</li>
 * <li>3. Each participant sends an acknowledgement to the coordinator.</li>
 * <li>4. The coordinator completes the transaction when acknowledgements have been received.</li>
 * </ul>
 */
public class Coordinator extends ComponentDefinition {
	
	Negative<Coordination> coordinator = negative(Coordination.class);
	Positive<Network> net= positive(Network.class);

	private int id;
    
	private Address self;

	private Map<Integer,Address> mapParticipants;

	private Map<Integer,Integer> tranVotes = new HashMap<Integer,Integer>();
	
	private int transactionCount = 0;
	
	private Map<Integer,List<Operation>> activeTransactions = new HashMap<Integer,List<Operation>>();
	
	public Coordinator() {
	  subscribe(handleCoordinatorInit,control);
	  
	  subscribe(handleBeginTransaction, coordinator);
	  subscribe(handleCommitTransaction, coordinator);
	  subscribe(handleRollbackTransaction, coordinator);
	  subscribe(handleReadOperation, coordinator);
	  subscribe(handleWriteOperation, coordinator);
	  
	  subscribe(handleCommit,net);
	  subscribe(handleAbort,net);
	  subscribe(handleAck,net);
	}
	
	Handler<CoordinatorInit> handleCoordinatorInit = new Handler<CoordinatorInit>() {
		public void handle(CoordinatorInit init) {
			id = init.getId();
			self = init.getSelf();
			mapParticipants = init.getMapParticipants();
		}
	};
	
	Handler<BeginTransaction> handleBeginTransaction = new Handler<BeginTransaction>() {
		public void handle(BeginTransaction trans) {
			List<Operation> ops = new ArrayList<Operation>();
			activeTransactions.put(trans.getTransactionId(), ops);
			tranVotes.put(trans.getTransactionId(), 0);
		}
	};
	
	Handler<CommitTransaction> handleCommitTransaction = new Handler<CommitTransaction>() {
		public void handle(CommitTransaction trans) {
			// Start Two-Phase Commit with Participants
			List<Operation> ops = activeTransactions.get(trans.getTransactionId());
			
			for (Address dest : mapParticipants.values())
			{
				Transaction t = new Transaction(trans.getTransactionId(),
						Transaction.CommitType.COMMIT, ops);
				trigger(new Prepare(t,self,dest), net);
			}
		}
	};
	
	Handler<RollbackTransaction> handleRollbackTransaction = new Handler<RollbackTransaction>() {
		public void handle(RollbackTransaction trans) {
			
			for (Address dest : mapParticipants.values())
			{
				trigger(new Abort(trans.getTransactionId(), self, dest), net);
			}
		}
	};
	
	Handler<ReadOperation> handleReadOperation = new Handler<ReadOperation>() {
		public void handle(ReadOperation readOp) {
			// Add operation to its active transaction
			List<Operation> ops = activeTransactions.get(readOp.getTransactionId());
			ops.add(readOp);
			// TODO send read to participants and result to client, read-lock tuple  
		}
	};
	
	Handler<WriteOperation> handleWriteOperation = new Handler<WriteOperation>() {
		public void handle(WriteOperation writeOp) {
			// Add operation to its active transaction
			List<Operation> ops = activeTransactions.get(writeOp.getTransactionId());
			ops.add(writeOp);
		}
	};
	

	Handler<Commit> handleCommit = new Handler<Commit>() {
		public void handle(Commit commit) {
			
			int tId = commit.getTransactionId();
			if (tranVotes.get(tId) == -1)
			{
				// do nothing - transaction aborted
				return; 
			}

			tranVotes.put(tId, tranVotes.get(tId)+1);

			if (tranVotes.get(tId) == mapParticipants.size())
			{
				// transaction committed
				trigger(new TransResult(tId,true),coordinator);
			}
		}
	};
	
	Handler<Abort> handleAbort = new Handler<Abort>() {
		public void handle(Abort abort) {
			// transaction aborted
			tranVotes.put(abort.getTransactionId(),-1);
			trigger(new TransResult(abort.getTransactionId(),false),coordinator);
		}
	};
	
	Handler<Ack> handleAck = new Handler<Ack>() {
		public void handle(Ack ack) 
		{
			TransResult res = new TransResult(ack.getTransactionId(),true);
			
			if (ack.getResponses().size() > 0)
			{
				res.setResponses(ack.getResponses());
			}
			trigger(res,coordinator);
		}
	};
}
