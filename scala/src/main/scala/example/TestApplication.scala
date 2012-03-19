/**
 *
 */
package se.sics.kompics.scala.propose

import se.sics.kompics.scala.ComponentDefinition
import se.sics.kompics.timer.Timer
import se.sics.kompics.Event
import se.sics.kompics.address.Address
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import se.sics.kompics.Kompics
import java.io.BufferedReader
import java.io.InputStreamReader
import se.sics.kompics.timer.ScheduleTimeout
import se.kth.ict.id2203.application.ApplicationContinue
import se.sics.kompics.scala.NegativePort
import se.sics.kompics.scala.PositivePort
import se.sics.kompics.scala.Init

/**
 * @author sario
 *
 */
class TestApplication extends ComponentDefinition {
	val reb = --(SimpleBroadcast);
	val timer = --(classOf[Timer]);

	private var commands: Array[String] = null;
	private var lastCommand: Int = 0;
	private var neighborSet: Set[Address] = null;
	private var self: Address = null;

	private def logger = TestApplication.logger;


	ctrl uponEvent {
		case Init(commandScript: String, neighbourSet: Set[Address], self: Address) => { () =>
			commands = commandScript.split(":");
			lastCommand = -1;
			this.neighborSet = neighbourSet;
			this.self = self;
		}
		case Start() => {() => doNextCommand()}
	}
	
	timer uponEvent {
		case Continue(_) => {() => doNextCommand()}
	}
	
	reb uponEvent {
		case SBDeliver(p: Address, m: String) => {() => 
			logger.info("Got a broadcasted message from " + p + ": " + m);
		}
	}
	
	

	private def doNextCommand(): Unit = {
		lastCommand += 1;

		if (lastCommand > commands.length) {
			return ;
		}
		if (lastCommand == commands.length) {
			TestApplication.logger.info("DONE ALL OPERATIONS");
			val applicationThread: Thread = new Thread("ApplicationThread") {

				override def run(): Unit = {
					val in: BufferedReader = new BufferedReader(
						new InputStreamReader(System.in));
					while (true) {
						try {
							val line = in.readLine();
							doCommand(line);
						} catch {
							case e: Throwable => e.printStackTrace();
							case e => println("Unkown error occured: " + e.toString());
						}
					}
				}

			};
			applicationThread.start();
			return ;
		}
		val op = commands(lastCommand);
		doCommand(op);
	}

	private def doCommand(cmd: String): Unit = {
		if (cmd.startsWith("S")) {
			doSleep(Integer.parseInt(cmd.substring(1)));
		} else if (cmd.startsWith("X")) {
			doShutdown();
		} else if (cmd.equals("help")) {
			doHelp();
			doNextCommand();
		} else if (cmd.startsWith("PB")) {
			doBroadcast(cmd.substring(2));
			doNextCommand();
		} else {
			logger.info("Bad command: '{}'. Try 'help'", cmd);
			doNextCommand();
		}
	}

	private def doSleep(delay: Int): Unit = {
		logger.info("Sleeping {} milliseconds...", delay);

		val st = new ScheduleTimeout(delay);
		st.setTimeoutEvent(new ApplicationContinue(st));
		trigger(st, timer);
	}

	private def doHelp(): Unit = {
		logger.info("Available commands: Ofl, Op, PB<m>, L<m>, S<n>, help, X");
		logger.info("PBm: broadcasts a messages to all nodes in topology using Probabilistic Broadcast");
		logger.info("Op and Ofl are mutually exclusive and will override each other.");
		logger.info("Lm: sends lossy message 'm' to all neighbors");
		logger.info("Sn: sleeps 'n' milliseconds before the next command");
		logger.info("help: shows this help message");
		logger.info("X: terminates this process");
	}

	private def doBroadcast(msg: String): Unit = {
		trigger(SBSend(msg), reb);
	}

	private def doShutdown(): Unit = {
		System.out.println("2DIE");
		System.out.close();
		System.err.close();
		Kompics.shutdown();
	}

}

object TestApplication {
	val logger: Logger = LoggerFactory.getLogger(classOf[TestApplication]);
}