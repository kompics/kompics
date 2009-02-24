package ${package}.main;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import ${package}.main.event.ApplicationContinue;
import ${package}.main.event.ApplicationInit;
import ${package}.main.event.Hello;
import ${package}.main.event.SendHello;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;

public final class CommandProcessor extends ComponentDefinition {

	private Positive<HelloPort> hello = positive(HelloPort.class);
	private Positive<Timer> timer = positive(Timer.class);

	private static final Logger logger = LoggerFactory
			.getLogger(CommandProcessor.class);

	private String[] commands;
	private int lastCommand;
	private Address self;
	
	
	public CommandProcessor() {
		subscribe(handleInit, control);
		subscribe(handleStart, control);
		subscribe(handleContinue, timer);
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
			logger.info("Starting Command Processor......");
			doNextCommand();
		}
	};

	Handler<ApplicationContinue> handleContinue = new Handler<ApplicationContinue>() {
		public void handle(ApplicationContinue event) {
			doNextCommand();
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
		logger.info("Comand:" + cmd.substring(0, 1));
		if (cmd.startsWith("H")) {
			doSendHello(Integer.parseInt(cmd.substring(1)));
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
		logger.info("Available commands: H<id>, S<n>, help, X");
		logger.info("H<id>: Say Hello to <id>");
		logger.info("Sn: sleeps 'n' milliseconds before the next command");
		logger.info("help: shows this help message");
		logger.info("X: terminates this process");
	}

	private void doSendHello(int id) {
		// you're not included in your list of neighbours =>
		// decrement by 1, id of node to send hello msg to.
		id = id -1;
		SendHello h = new SendHello(id);
		logger.info("Sending hello event");
		trigger(h,hello);
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
