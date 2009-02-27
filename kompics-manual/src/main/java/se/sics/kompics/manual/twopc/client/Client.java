package se.sics.kompics.manual.twopc.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.manual.twopc.event.ApplicationInit;
import se.sics.kompics.manual.twopc.event.BeginTransaction;
import se.sics.kompics.manual.twopc.event.CommitTransaction;
import se.sics.kompics.manual.twopc.event.ReadOperation;
import se.sics.kompics.manual.twopc.event.RollbackTransaction;
import se.sics.kompics.manual.twopc.event.SelectAllOperation;
import se.sics.kompics.manual.twopc.event.TransResult;
import se.sics.kompics.manual.twopc.event.WriteOperation;
import se.sics.kompics.manual.twopc.main.event.ApplicationContinue;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;

public final class Client extends ComponentDefinition {

	Positive<ClientPort> coordinator = positive(ClientPort.class);
	
	Positive<Timer> timer = positive(Timer.class);

	private static final Logger logger = LoggerFactory
			.getLogger(Client.class);

	private String[] commands;
	private int lastCommand;
//	private Address self;
	
	private int transId=0;

//	private Transaction trans;
	
	public Client() {
		subscribe(handleInit, control);
		subscribe(handleStart, control);
		subscribe(handleContinue, timer);

		subscribe(handleTransResult, coordinator);
	}

	Handler<ApplicationInit> handleInit = new Handler<ApplicationInit>() {
		public void handle(ApplicationInit event) {
//			self = event.getSelf();
			commands = event.getCommandScript().split(":");
			lastCommand = -1;
		}
	};

	Handler<Start> handleStart = new Handler<Start>() {
		public void handle(Start event) {
			logger.debug("Starting Command Processor......");
			doNextCommand();
		}
	};

	Handler<ApplicationContinue> handleContinue = new Handler<ApplicationContinue>() {
		public void handle(ApplicationContinue event) {
			doNextCommand();
		}
	};

	Handler<TransResult> handleTransResult = new Handler<TransResult>() {
		public void handle(TransResult event) {
			logger.debug("{}: Received TransResult is {}", 
					event.getTransactionId(), event.isSuccess());
			
			logger.debug("Returned tuples:");
			Map<String,String> readValues = event.getResponses();
			if (readValues.size() > 0)
			{
				for (Map.Entry<String,String> res : readValues.entrySet())
				{
					logger.info(event.getTransactionId() + ": ReadValue for {} is {}",
							res.getKey(), res.getValue());
				}
			}
		}
	};


	private final void doNextCommand() {
		lastCommand++;

		if (lastCommand > commands.length) {
			return;
		}
		if (lastCommand == commands.length) {
			logger.debug("DONE ALL OPERATIONS");
			Thread applicationThread = new Thread("ApplicationThread") {
				public void run() {
					BufferedReader in = new BufferedReader(
							new InputStreamReader(System.in));
					while (true) {
						try {
							String line = in.readLine();
							doCommand(line);
						} catch (Throwable e) {
							e.printStackTrace();
						}
					}
				}
			};
			applicationThread.start();
			return;
		}
		String op = commands[lastCommand];
		doCommand(op);
	}

	private void doCommand(String cmd) {
		logger.debug("Comand:" + cmd.substring(0, 1));
		if (cmd.startsWith("B")) {
			doBeginTransaction();
			doNextCommand();
		} else if (cmd.startsWith("R")) {
			doReadOperation(cmd.substring(1));
			doNextCommand();
		} else if (cmd.startsWith("W")) {
			int endOfName = cmd.indexOf(",");
			doWriteOperation(cmd.substring(1,endOfName), cmd.substring(endOfName+1));
			doNextCommand();
		} else if (cmd.startsWith("C")) {
			doCommitTransaction();
			doNextCommand();
		} else if (cmd.startsWith("A")) {
			doRollbackTransaction();
			doNextCommand();
		} else if (cmd.startsWith("S")) {
			doSleep(Integer.parseInt(cmd.substring(1)));
		} else if (cmd.startsWith("*")) {
			doSelectAllOperation();
			doNextCommand();
		} else if (cmd.startsWith("X")) {
			doShutdown();
		} else if (cmd.equals("help")) {
			doHelp();
			doNextCommand();
		} else if (cmd.equals("") || cmd.equals(" ")) {
			// do nothing if no command given
			doNextCommand();
		} else {
			logger.info("Bad command: '{}'. Try 'help'", cmd);
			doNextCommand();
		}
	}

	private final void doHelp() {
		logger.info("Available commands: B, R<n>,W<n,v>, C, A, help, X");
		logger.info("B: Transaction begin");
		logger.info("C: Transaction commit");
		logger.info("A: Transaction abort (rollback)");		
		logger.info("R<n>: Read Operation for name, e.g., Rkompics:");
		logger.info("W<n,v>: Write Operation to set name to value, e.g., Wkompics,09:");
		logger.info("Sn: sleeps 'n' milliseconds before the next command");
		logger.info("help: shows this help message");
		logger.info("X: terminates this process");
	}

	private void doBeginTransaction() {
		transId++;
		BeginTransaction b = new BeginTransaction(transId);
		logger.debug("Beginning transaction with {} id", transId);
		trigger(b,coordinator);
	}

	private void doReadOperation(String name) {
		logger.debug("Creating Read Operation with {} as name.", name);
		trigger(new ReadOperation(transId, name),coordinator);
	}
	
	private void doWriteOperation(String name, String value) {
		logger.debug("Creating Write Operation with {} as name and {} as value.", name, value);
		trigger(new WriteOperation(transId, name,value),coordinator);
	}

	private void doSelectAllOperation() {
		logger.debug("Select all values from database.");
		trigger(new SelectAllOperation(transId),coordinator);
	}
	
	private void doCommitTransaction() {
		logger.debug("Sending Commit Transaction {}", transId);
		trigger(new CommitTransaction(transId), coordinator);
	}

	private void doRollbackTransaction() {
		logger.debug("Sending Rollback Transaction {}", transId);
		trigger(new RollbackTransaction(transId), coordinator);		
	}

	private void doSleep(long delay) {
		logger.debug("Sleeping {} milliseconds...", delay);

		ScheduleTimeout st = new ScheduleTimeout(delay);
		st.setTimeoutEvent(new ApplicationContinue(st));
		trigger(st, timer);
	}

	private void doShutdown() {
		System.out.close();
		System.err.close();
		System.exit(0);
	}
}
