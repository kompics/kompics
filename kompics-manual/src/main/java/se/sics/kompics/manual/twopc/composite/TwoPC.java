package se.sics.kompics.manual.twopc.composite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
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
import se.sics.kompics.manual.twopc.event.ParticipantInit;
import se.sics.kompics.manual.twopc.event.Prepare;
import se.sics.kompics.manual.twopc.event.Prepared;
import se.sics.kompics.manual.twopc.event.ReadOperation;
import se.sics.kompics.manual.twopc.event.RollbackTransaction;
import se.sics.kompics.manual.twopc.event.TransResult;
import se.sics.kompics.manual.twopc.event.WriteOperation;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

public class TwoPC extends ComponentDefinition {
	
	private Component coordinator;
	private Component participant;

	private Negative<Client> inClient = negative(Client.class);
	
	private Positive<Network> netPort = positive(Network.class);

	private Positive<Timer> timer = positive(Timer.class);
	
	private Address self;
	private int id;
	
	private static final Logger logger = LoggerFactory
	.getLogger(TwoPC.class);

	
	public TwoPC() {
		coordinator = create(Coordinator.class);
		participant = create(Participant.class);
		
		connect(participant.getNegative(Timer.class), timer);
		connect(coordinator.getNegative(Timer.class), timer);
		
		// events from this component's control port
		subscribe(handleCoordinatorInit, control);
		
		// events from external CommandProcessor (over inClient Port)
		subscribe(handleBeginTransaction, inClient);
		subscribe(handleCommitTransaction, inClient);
		subscribe(handleRollbackTransaction, inClient);
		subscribe(handleReadOperation, inClient);
		subscribe(handleWriteOperation, inClient);

		// events from child coordination port
		subscribe(handleCommit,coordinator.getNegative(TwoPhaseCommit.class));
		subscribe(handleAbort,coordinator.getNegative(TwoPhaseCommit.class));
		subscribe(handlePrepare,coordinator.getNegative(TwoPhaseCommit.class));
		subscribe(handleTransResult,coordinator.getPositive(Client.class));
		
		// events from Network port destined for child coordination port
		subscribe(handleCommit,netPort);
		subscribe(handleAbort,netPort);
		subscribe(handlePrepare,netPort);

		// events from child participation Port
		subscribe(handleAck,participant.getPositive(TwoPhaseCommit.class));
		subscribe(handlePrepared,participant.getPositive(TwoPhaseCommit.class));
		subscribe(handleParticipantAbort,participant.getPositive(TwoPhaseCommit.class));
		
		// events from Network port destined for child participation port		
		subscribe(handlePrepared,netPort);
		subscribe(handleParticipantAbort,netPort);		
		subscribe(handleAck,netPort);
	}
	
	Handler<CoordinatorInit> handleCoordinatorInit = new Handler<CoordinatorInit>() {
		public void handle(CoordinatorInit init) {
			id = init.getId();
			trigger(init,coordinator.getControl());
			self = init.getSelf();
			trigger(new ParticipantInit(init.getSelf()), participant.getControl());
		}
	};
	
	Handler<BeginTransaction> handleBeginTransaction = new Handler<BeginTransaction>() {
		public void handle(BeginTransaction trans) {
			trigger(trans, coordinator.getPositive(Client.class));
		}
	};
	
	Handler<CommitTransaction> handleCommitTransaction = new Handler<CommitTransaction>() {
		public void handle(CommitTransaction trans) {
			trigger(trans, coordinator.getPositive(Client.class));
		}
	};
	
	Handler<RollbackTransaction> handleRollbackTransaction = new Handler<RollbackTransaction>() {
		public void handle(RollbackTransaction trans) {
			trigger(trans, coordinator.getPositive(Client.class));
		}
	};
	
	Handler<ReadOperation> handleReadOperation = new Handler<ReadOperation>() {
		public void handle(ReadOperation trans) {
			trigger(trans, coordinator.getPositive(Client.class));
		}
	};
	
	Handler<WriteOperation> handleWriteOperation = new Handler<WriteOperation>() {
		public void handle(WriteOperation trans) {
			trigger(trans, coordinator.getPositive(Client.class));
		}
	};
	
	Handler<Prepared> handlePrepared = new Handler<Prepared>() {
		public void handle(Prepared prepared) {
			forwardCoordinationTPC(prepared);
		}
	};
	
	Handler<Commit> handleCommit = new Handler<Commit>() {
		public void handle(Commit commit) {
			forwardParticipation(commit);
		}
	};
	
	Handler<Abort> handleAbort = new Handler<Abort>() {
		public void handle(Abort abort) {
			forwardCoordinationTPC(abort);
		}
	};
	
	Handler<Ack> handleAck = new Handler<Ack>() {
		public void handle(Ack ack) 
		{
			forwardCoordinationTPC(ack);
		}
	};
	
	
	
	Handler<Prepare> handlePrepare = new Handler<Prepare>() {
		public void handle(Prepare prepare) {
			forwardParticipation(prepare);
		}
	};
	
	Handler<Commit> handleCommitP = new Handler<Commit>() {
		public void handle(Commit commit) {
			forwardCoordinationClient(commit);
		}
	};

	Handler<Abort> handleParticipantAbort = new Handler<Abort>() {
		public void handle(Abort rollback) {
			forwardCoordinationClient(rollback);
		}
	};

	Handler<TransResult> handleTransResult = new Handler<TransResult>() {
		public void handle(TransResult res) {
			trigger(res,inClient);
		}
	};
	
	protected void forwardCoordinationClient(Message m)
	{
		if (m.getSource().equals(self))
		{
			trigger(m, netPort);
		}
		else
		{
			trigger(m,coordinator.getPositive(Client.class));
		}
	}

	protected void forwardCoordinationTPC(Message m)
	{
		if (m.getSource().equals(self))
		{
			trigger(m, netPort);
		}
		else
		{
			trigger(m,coordinator.getNegative(TwoPhaseCommit.class));
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
			trigger(m, participant.getPositive(TwoPhaseCommit.class));
		}
	}
}
