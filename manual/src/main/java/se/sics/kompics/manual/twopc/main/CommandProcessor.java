package se.sics.kompics.manual.twopc.main;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.manual.twopc.Coordination;
import se.sics.kompics.manual.twopc.event.ApplicationInit;
import se.sics.kompics.manual.twopc.event.TransResult;
import se.sics.kompics.manual.twopc.event.Transaction;
import se.sics.kompics.manual.twopc.main.event.ApplicationContinue;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;

public final class CommandProcessor extends ComponentDefinition {

	Positive<Coordination> coordinator = positive(Coordination.class);
	Positive<Timer> timer = positive(Timer.class);

	private static final Logger logger = LoggerFactory
			.getLogger(CommandProcessor.class);

	private String[] commands;
	private int lastCommand;
	private Address self;
	
	private int transCount=0;

	public CommandProcessor() {
		subscribe(handleInit, control);
		subscribe(handleStart, control);
		subscribe(handleContinue, timer);

		subscribe(handleTransResult, coordinator);
	}

	Handler<ApplicationInit> handleInit = new Handler<ApplicationInit>() {
		public void handle(ApplicationInit event) {
			self = event.getSelf();
			commands = event.getCommandScript().split(":");
			lastCommand = -1;
		}
	};

	Handler<Start> handleStart = new Handler<Start>() {
		public void handle(Start event) {
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
			logger.info("Received TransResult for transaction-id {} is {}", 
					event.getTransactionId(), event.isSuccess());
		}
	};


	private final void doNextCommand() {
		lastCommand++;

		if (lastCommand > commands.length) {
			return;
		}
		if (lastCommand == commands.length) {
			logger.info("DONE ALL OPERATIONS");
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
		if (cmd.startsWith("T")) {
			doStartTransaction(Integer.parseInt(cmd.substring(1)));
			doNextCommand();
		} else if (cmd.startsWith("S")) {
			doSleep(Integer.parseInt(cmd.substring(1)));
		} else if (cmd.startsWith("X")) {
			doShutdown();
		} else if (cmd.equals("help")) {
			doHelp();
			doNextCommand();
		} else {
			logger.info("Bad command: '{}'. Try 'help'", cmd);
			doNextCommand();
		}
	}

	private final void doHelp() {
		logger.info("Available commands: T<m>, S<n>, help, X");
		logger.info("Tm: Transaction created with 'm' operations");
		logger.info("Sn: sleeps 'n' milliseconds before the next command");
		logger.info("help: shows this help message");
		logger.info("X: terminates this process");
	}

	private void doStartTransaction(int numOps) {
		logger.info("Creating transaction with {} operations", numOps);
		
		Transaction t = new Transaction(transCount++);
		for (int i=0; i<numOps;i++)
		{
				t.addOperation(t.new Operation(new Integer((int)System.currentTimeMillis()).toString(), 
						new Integer(i).toString()));
		}

		
		trigger(t, coordinator);
	}

	private void doSleep(long delay) {
		logger.info("Sleeping {} milliseconds...", delay);

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
