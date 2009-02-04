package se.sics.kompics.manual.twopc.simple;

import java.util.HashMap;
import java.util.Map;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.manual.twopc.Coordination;
import se.sics.kompics.manual.twopc.event.Abort;
import se.sics.kompics.manual.twopc.event.Ack;
import se.sics.kompics.manual.twopc.event.Commit;
import se.sics.kompics.manual.twopc.event.CoordinatorInit;
import se.sics.kompics.manual.twopc.event.Prepare;
import se.sics.kompics.manual.twopc.event.TransResult;
import se.sics.kompics.manual.twopc.event.Transaction;
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
	
	public Coordinator() {
	  subscribe(handleCoordinatorInit,control);
	  
	  subscribe(handleStartTransaction, coordinator);
	  
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
	
	Handler<Transaction> handleStartTransaction = new Handler<Transaction>() {
		public void handle(Transaction trans) {
			
			tranVotes.put(trans.getId(), 0);
			
			for (Address dest : mapParticipants.values())
			{
				trigger(new Prepare(trans,self,dest), net);
			}
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
		public void handle(Ack ack) {
		}
	};
}
