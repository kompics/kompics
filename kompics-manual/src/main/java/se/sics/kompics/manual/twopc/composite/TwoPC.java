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
import se.sics.kompics.manual.twopc.TwoPhaseCommit;
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
import se.sics.kompics.manual.twopc.main.ApplicationGroup;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

public class TwoPC extends ComponentDefinition {
	
	Component coordinator;
	Component participant;

	Negative<Client> inClient = negative(Client.class);
	Positive<Client> coordinationClient = positive(Client.class);

	Positive<TwoPhaseCommit> participationTPC= positive(TwoPhaseCommit.class);
	Negative<TwoPhaseCommit> coordinationTPC= negative(TwoPhaseCommit.class);
	
	Positive<Network> netPort = positive(Network.class);

	Positive<Timer> timer = positive(Timer.class);
	
	Address self;
	
	private static final Logger logger = LoggerFactory
	.getLogger(TwoPC.class);

	
	public TwoPC() {
		coordinator = create(Coordinator.class);
		participant = create(Participant.class);
		
		// XXX does this work?
//		connect(inClient,coordinationClient);
		
		connect(participant.getNegative(Timer.class), timer);
		connect(coordinator.getNegative(Timer.class), timer);
		
		// events from this component's control port
		subscribe(handleCoordinatorInit, control);
		
		// events from inputCoordination Port (local events)
		subscribe(handleBeginTransaction, inClient);
		subscribe(handleCommitTransaction, inClient);
		subscribe(handleRollbackTransaction, inClient);
		subscribe(handleReadOperation, inClient);
		subscribe(handleWriteOperation, inClient);

		subscribe(handleCommit,coordinationTPC);
		subscribe(handleAbort,coordinationTPC);
		subscribe(handlePrepare,coordinationTPC);
		// events from net Port destined for childCoordination Port
		subscribe(handleCommit,netPort);
		subscribe(handleAbort,netPort);
		subscribe(handlePrepare,netPort);

		// events from childParticipation Port
		subscribe(handleAck,participationTPC);
		subscribe(handlePrepared,participationTPC);
		subscribe(handleParticipantAbort,participationTPC);		
		// events from net Port destined for childParticipation Port		
		subscribe(handlePrepared,netPort);
		subscribe(handleParticipantAbort,netPort);		
		subscribe(handleAck,netPort);
	}
	
	Handler<CoordinatorInit> handleCoordinatorInit = new Handler<CoordinatorInit>() {
		public void handle(CoordinatorInit init) {
			logger.info("Initialising TwoPC: " + init.getId());
			trigger(init,coordinator.getControl());
			self = init.getSelf();
			trigger(new ParticipantInit(init.getSelf()), participant.getControl());
		}
	};
	
	Handler<BeginTransaction> handleBeginTransaction = new Handler<BeginTransaction>() {
		public void handle(BeginTransaction trans) {
			logger.info("client: begin transaction.");
			trigger(trans,coordinationClient);
		}
	};
	
	Handler<CommitTransaction> handleCommitTransaction = new Handler<CommitTransaction>() {
		public void handle(CommitTransaction trans) {
			logger.info("client: commit transaction.");
			trigger(trans,coordinationClient);
		}
	};
	
	Handler<RollbackTransaction> handleRollbackTransaction = new Handler<RollbackTransaction>() {
		public void handle(RollbackTransaction trans) {
			logger.info("rollback transaction.");
			trigger(trans,coordinationClient);
		}
	};
	
	Handler<ReadOperation> handleReadOperation = new Handler<ReadOperation>() {
		public void handle(ReadOperation readOp) {
			logger.info("client: read operation.");
			trigger(readOp,coordinationClient);
		}
	};
	
	Handler<WriteOperation> handleWriteOperation = new Handler<WriteOperation>() {
		public void handle(WriteOperation writeOp) {
			logger.info("client: write operation.");
			trigger(writeOp,coordinationClient);
		}
	};
	
	Handler<Prepared> handlePrepared = new Handler<Prepared>() {
		public void handle(Prepared prepared) {
			logger.info("prepared.");
			forwardCoordination(prepared);
		}
	};
	
	Handler<Commit> handleCommit = new Handler<Commit>() {
		public void handle(Commit commit) {
			logger.info("commit");
			forwardParticipation(commit);
		}
	};
	
	Handler<Abort> handleAbort = new Handler<Abort>() {
		public void handle(Abort abort) {
			logger.info("abort");
			forwardCoordination(abort);
		}
	};
	
	Handler<Ack> handleAck = new Handler<Ack>() {
		public void handle(Ack ack) 
		{
			logger.info("ack");
			forwardCoordination(ack);
		}
	};
	
	
	
	Handler<Prepare> handlePrepare = new Handler<Prepare>() {
		public void handle(Prepare prepare) {
			logger.info("prepare");
			forwardParticipation(prepare);
		}
	};
	
	Handler<Commit> handleCommitP = new Handler<Commit>() {
		public void handle(Commit commit) {
			logger.info("commitP");
			forwardCoordination(commit);
		}
	};

	Handler<Abort> handleParticipantAbort = new Handler<Abort>() {
		public void handle(Abort rollback) {
			logger.info("abortP");
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
			trigger(m,coordinationClient);
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
			trigger(m,participationTPC);
		}
	}
}
