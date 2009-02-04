package se.sics.kompics.manual.twopc.simple;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.manual.twopc.Coordination;
import se.sics.kompics.manual.twopc.Participation;
import se.sics.kompics.manual.twopc.event.Abort;
import se.sics.kompics.manual.twopc.event.Ack;
import se.sics.kompics.manual.twopc.event.BeginTransaction;
import se.sics.kompics.manual.twopc.event.Commit;
import se.sics.kompics.manual.twopc.event.CommitTransaction;
import se.sics.kompics.manual.twopc.event.CoordinatorInit;
import se.sics.kompics.manual.twopc.event.ParticipantInit;
import se.sics.kompics.manual.twopc.event.Prepare;
import se.sics.kompics.manual.twopc.event.ReadOperation;
import se.sics.kompics.manual.twopc.event.RollbackTransaction;
import se.sics.kompics.manual.twopc.event.TransResult;
import se.sics.kompics.manual.twopc.event.WriteOperation;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Network;

public class TwoPC extends ComponentDefinition {
	
	Component coordinator;
	Component participant;

	Negative<Coordination> inputCoordination = negative(Coordination.class);
	Positive<Coordination> childCoordination = positive(Coordination.class);
	Positive<Participation> childParticipation= positive(Participation.class);
		
	Positive<Network> netPort = positive(Network.class);

	private int id;

	private Address self;
	
	public TwoPC() {
		coordinator = create(Coordinator.class);
		participant = create(Participant.class);
		
		connect(inputCoordination,childCoordination);
		
		subscribe(handleCoordinatorInit, control);
		subscribe(handleTransResult, childCoordination);
		
		subscribe(handleBeginTransaction, inputCoordination);
		subscribe(handleCommitTransaction, inputCoordination);
		subscribe(handleRollbackTransaction, inputCoordination);
		subscribe(handleReadOperation, inputCoordination);
		subscribe(handleWriteOperation, inputCoordination);
		
		subscribe(handleCommit,childCoordination);
		subscribe(handleAbort,childCoordination);
		subscribe(handleAck,childCoordination);

		subscribe(handleCommit,netPort);
		subscribe(handleAbort,netPort);
		subscribe(handleAck,netPort);
		
		subscribe(handlePrepare,netPort);
		subscribe(handleCommit,netPort);
		subscribe(handleRollback,netPort);		

		subscribe(handlePrepare,childParticipation);
		subscribe(handleCommit,childParticipation);
		subscribe(handleRollback,childParticipation);		
	}
	
	Handler<CoordinatorInit> handleCoordinatorInit = new Handler<CoordinatorInit>() {
		public void handle(CoordinatorInit init) {
			id = init.getId();
			self = init.getSelf();
			
			trigger(init,coordinator.getControl());
			trigger(new ParticipantInit(self), participant.getControl());
		}
	};
	
	Handler<TransResult> handleTransResult = new Handler<TransResult>() {
		public void handle(TransResult event) {
			trigger(event, inputCoordination);
		}
	};

	Handler<BeginTransaction> handleBeginTransaction = new Handler<BeginTransaction>() {
		public void handle(BeginTransaction trans) {
			trigger(trans,childCoordination);
		}
	};
	
	Handler<CommitTransaction> handleCommitTransaction = new Handler<CommitTransaction>() {
		public void handle(CommitTransaction trans) {
			trigger(trans,childCoordination);
		}
	};
	
	Handler<RollbackTransaction> handleRollbackTransaction = new Handler<RollbackTransaction>() {
		public void handle(RollbackTransaction trans) {
			trigger(trans,childCoordination);
		}
	};
	
	Handler<ReadOperation> handleReadOperation = new Handler<ReadOperation>() {
		public void handle(ReadOperation readOp) {
			trigger(readOp,childCoordination);
		}
	};
	
	Handler<WriteOperation> handleWriteOperation = new Handler<WriteOperation>() {
		public void handle(WriteOperation writeOp) {
			trigger(writeOp,childCoordination);
		}
	};
	
	Handler<Commit> handleCommit = new Handler<Commit>() {
		public void handle(Commit commit) {
			forwardCoordination(commit);
		}
	};
	
	Handler<Abort> handleAbort = new Handler<Abort>() {
		public void handle(Abort abort) {
			forwardCoordination(abort);
		}
	};
	
	Handler<Ack> handleAck = new Handler<Ack>() {
		public void handle(Ack ack) 
		{
			forwardCoordination(ack);
		}
	};
	
	
	
	Handler<Prepare> handlePrepare = new Handler<Prepare>() {
		public void handle(Prepare prepare) {
			forwardParticipation(prepare);
		}
	};
	
	Handler<Commit> handleCommitP = new Handler<Commit>() {
		public void handle(Commit commit) {
			forwardParticipation(commit);
		}
	};

	Handler<Abort> handleRollback = new Handler<Abort>() {
		public void handle(Abort rollback) {
			forwardParticipation(rollback);
		}
	};

	protected void forwardCoordination(Message m)
	{
		if (m.getSource().equals(self))
		{
			trigger(m, netPort);
		}
		else
		{
			trigger(m,childCoordination);
		}
	}

	protected void forwardParticipation(Message m)
	{
		if (m.getSource().equals(self))
		{
			trigger(m, netPort);
		}
		else
		{
			trigger(m,childParticipation);
		}
	}
}
