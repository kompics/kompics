package se.sics.kompics.manual.twopc.simple;

import java.util.ArrayList;
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
import se.sics.kompics.manual.twopc.Client;
import se.sics.kompics.manual.twopc.event.Abort;
import se.sics.kompics.manual.twopc.event.Ack;
import se.sics.kompics.manual.twopc.event.BeginTransaction;
import se.sics.kompics.manual.twopc.event.Commit;
import se.sics.kompics.manual.twopc.event.CommitTransaction;
import se.sics.kompics.manual.twopc.event.CoordinatorInit;
import se.sics.kompics.manual.twopc.event.Operation;
import se.sics.kompics.manual.twopc.event.Prepare;
import se.sics.kompics.manual.twopc.event.Prepared;
import se.sics.kompics.manual.twopc.event.ReadOperation;
import se.sics.kompics.manual.twopc.event.RollbackTransaction;
import se.sics.kompics.manual.twopc.event.TransResult;
import se.sics.kompics.manual.twopc.event.Transaction;
import se.sics.kompics.manual.twopc.event.WriteOperation;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

public class TwoPCblob extends ComponentDefinition {
	
	private Negative<Client> inClient = negative(Client.class);
	
	private Positive<Network> network = positive(Network.class);

	private Positive<Timer> timer = positive(Timer.class);
	
	private Address self;
	private int id;
	
	private static final Logger logger = LoggerFactory
	.getLogger(TwoPCblob.class);

	private Map<Integer, Address> mapParticipants;

	private Map<Integer, Integer> tranVotes = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> tranAcks = new HashMap<Integer, Integer>();

	private Map<Integer, List<Operation>> activeTransactions = new HashMap<Integer, List<Operation>>();

    private Map<String,String> database = new HashMap<String,String>();

	public TwoPCblob() {
		
		// events from this component's control port
		subscribe(handleCoordinatorInit, control);
		
		// events from external CommandProcessor (over inClient Port)
		subscribe(handleBeginTransaction, inClient);
		subscribe(handleCommitTransaction, inClient);
		subscribe(handleRollbackTransaction, inClient);
		subscribe(handleReadOperation, inClient);
		subscribe(handleWriteOperation, inClient);

		// events from Network port for coordination 
		subscribe(handleCommit,network);
		subscribe(handleAbort,network);
		subscribe(handlePrepare,network);

		// events from Network port for participation 		
		subscribe(handlePrepared,network);
		subscribe(handleParticipantAbort,network);		
		subscribe(handleAck,network);
	}
	
	Handler<CoordinatorInit> handleCoordinatorInit = new Handler<CoordinatorInit>() {
		public void handle(CoordinatorInit init) {
			id = init.getId();
			self = init.getSelf();
			mapParticipants = init.getMapParticipants();
			logger.info("Initializing Blob: " + id);
		}
	};
	
	Handler<BeginTransaction> handleBeginTransaction = new Handler<BeginTransaction>() {
		public void handle(BeginTransaction trans) {
			logger.info("Begin transaction: " + trans.getTransactionId());
			List<Operation> ops = new ArrayList<Operation>();
			activeTransactions.put(trans.getTransactionId(), ops);
			logger.info("TRANS VOTES is NOW: (" + trans.getTransactionId()+ ", 0)");
			tranVotes.put(trans.getTransactionId(), 0);
		}
	};
	
	Handler<CommitTransaction> handleCommitTransaction = new Handler<CommitTransaction>() {
		public void handle(CommitTransaction trans) {
			logger.info("CommitTransaction: " + trans.getTransactionId());

			// Start Two-Phase Commit with Participants
			List<Operation> ops = activeTransactions.get(trans
					.getTransactionId());

			for (Address dest : mapParticipants.values()) {
				Transaction t = new Transaction(trans.getTransactionId(),
						Transaction.CommitType.COMMIT, ops);

				logger.info("Coordinator: sending prepare to: "
						+ dest.toString());
				trigger(new Prepare(t, self, dest), network);
			}
		}
	};
	
	Handler<RollbackTransaction> handleRollbackTransaction = new Handler<RollbackTransaction>() {
		public void handle(RollbackTransaction trans) {
			logger.info("RollbackTransaction: " + trans.getTransactionId());
			for (Address dest : mapParticipants.values()) {
				trigger(new Abort(trans.getTransactionId(), self, dest),
						network);
			}
		}
	};
	
	Handler<ReadOperation> handleReadOperation = new Handler<ReadOperation>() {
		public void handle(ReadOperation readOp) {
			logger.info("ReadOperation " + readOp.getTransactionId());

			List<Operation> ops;
			if (activeTransactions.containsKey(readOp.getTransactionId()) == false) {
				ops = new ArrayList<Operation>();
				activeTransactions.put(readOp.getTransactionId(), ops);
			}

			// Add operation to its active transaction
			ops = activeTransactions.get(readOp.getTransactionId());
			ops.add(readOp);
			// TODO send read to participants and result to client, acquire read-lock
		}
	};
	
	Handler<WriteOperation> handleWriteOperation = new Handler<WriteOperation>() {
		public void handle(WriteOperation writeOp) {
			logger.info("WriteOperation "
					+ writeOp.getTransactionId());

			List<Operation> ops;
			if (activeTransactions.containsKey(writeOp.getTransactionId()) == false) {
				ops = new ArrayList<Operation>();
				activeTransactions.put(writeOp.getTransactionId(), ops);
			}
			ops = activeTransactions.get(writeOp.getTransactionId());
			ops.add(writeOp);
		}
	};
	
	Handler<Prepared> handlePrepared = new Handler<Prepared>() {
		public void handle(Prepared commit) {
			int tId = commit.getTransactionId();

			if (tranVotes.get(tId) == null) {
				logger.error("TRANS VOTES WAS NULL for transaction-id:" + tId);
				return;
			}

			if (tranVotes.get(tId) == -1) {
				// do nothing - transaction already aborted
				return;
			}

			tranVotes.put(tId, tranVotes.get(tId) + 1);

			if (tranVotes.get(tId) == mapParticipants.size()) {
				tranAcks.put(commit.getTransactionId(), 0);
				for (Address dest : mapParticipants.values()) {
					logger.info("Coordinator: sending commit to: "
							+ dest.toString());
					trigger(new Commit(tId, self, dest), network);
				}
			}
		}
	};
	
	Handler<Abort> handleAbort = new Handler<Abort>() {
		public void handle(Abort abort) {
			logger.info("Coordinator abort recvd " + abort.getTransactionId());
			// transaction aborted
			tranVotes.put(abort.getTransactionId(), -1);
			trigger(new TransResult(abort.getTransactionId(), false),
					inClient);
		}
	};
	
	
	Handler<Ack> handleAck = new Handler<Ack>() {
		public void handle(Ack ack) {
			int tId = ack.getTransactionId();
			logger.info("Coordinator ack recvd " + tId);

			tranAcks.put(tId, tranAcks.get(tId) + 1);
			if (tranAcks.get(tId) == mapParticipants.size()) {
				TransResult res = new TransResult(ack.getTransactionId(), true);

				if (ack.getResponses().size() > 0) {
					res.setResponses(ack.getResponses());
				}
				trigger(res, inClient);
			}
		}
	};
	
	Handler<Prepare> handlePrepare = new Handler<Prepare>() {
		public void handle(Prepare prepare) {
			Transaction t = prepare.getTrans();
			int id = t.getId();
			logger.info("prepare recvd: " + id);
			activeTransactions.put(id, t.getOperations());
			
			trigger(new Prepared(id, self, prepare.getSource()), network);
		}
	};
	
	Handler<Commit> handleCommit = new Handler<Commit>() {
		public void handle(Commit commit) {
			int tId = commit.getTransactionId();
			logger.info("commit recvd:" + tId);
			
			List<Operation> ops = activeTransactions.get(tId);
			
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
			trigger(new Ack(tId,responses,self,commit.getSource()),network);
		}
	};
	
	
	Handler<Commit> handleCommitP = new Handler<Commit>() {
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
			trigger(new Ack(transactionId,responses,self,commit.getSource()),network);
			activeTransactions.remove(transactionId);
		}
	};

	Handler<Abort> handleParticipantAbort = new Handler<Abort>() {
		public void handle(Abort rollback) {
			logger.info("abort recvd.");
			int id = rollback.getTransactionId();
			activeTransactions.remove(id);
		}
	};
	
}
