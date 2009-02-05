package se.sics.kompics.manual.twopc.composite;

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
import se.sics.kompics.manual.twopc.event.Prepared;
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

	Address self;
	
	public TwoPC() {
		coordinator = create(Coordinator.class);
		participant = create(Participant.class);
		
		// XXX does this work?
		connect(inputCoordination,childCoordination);
		
		// events from this component's control port
		subscribe(handleCoordinatorInit, control);
		
		// events from inputCoordination Port (local events)
		subscribe(handleBeginTransaction, inputCoordination);
		subscribe(handleCommitTransaction, inputCoordination);
		subscribe(handleRollbackTransaction, inputCoordination);
		subscribe(handleReadOperation, inputCoordination);
		subscribe(handleWriteOperation, inputCoordination);

		// events from childCoordination Port
		subscribe(handleTransResult, childCoordination); // not sent over network
		subscribe(handleCommit,childCoordination);
		subscribe(handleAbort,childCoordination);
		subscribe(handlePrepare,childCoordination);
		// events from net Port destined for childCoordination Port
		subscribe(handleCommit,netPort);
		subscribe(handleAbort,netPort);
		subscribe(handlePrepare,netPort);

		// events from childParticipation Port
		subscribe(handleAck,childParticipation);
		subscribe(handlePrepared,childParticipation);
		subscribe(handleParticipantAbort,childParticipation);		
		// events from net Port destined for childParticipation Port		
		subscribe(handlePrepared,netPort);
		subscribe(handleParticipantAbort,netPort);		
		subscribe(handleAck,netPort);
	}
	
	Handler<CoordinatorInit> handleCoordinatorInit = new Handler<CoordinatorInit>() {
		public void handle(CoordinatorInit init) {
			trigger(init,coordinator.getControl());
			self = init.getSelf();
			trigger(new ParticipantInit(init.getSelf()), participant.getControl());
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
	
	Handler<Prepared> handlePrepared = new Handler<Prepared>() {
		public void handle(Prepared prepared) {
			forwardCoordination(prepared);
		}
	};
	
	Handler<Commit> handleCommit = new Handler<Commit>() {
		public void handle(Commit commit) {
			forwardParticipation(commit);
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
			forwardCoordination(commit);
		}
	};

	Handler<Abort> handleParticipantAbort = new Handler<Abort>() {
		public void handle(Abort rollback) {
			forwardCoordination(rollback);
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
