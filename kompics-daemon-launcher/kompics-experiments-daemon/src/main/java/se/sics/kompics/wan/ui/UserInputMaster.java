package se.sics.kompics.wan.ui;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.TreeSet;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.wan.config.MasterConfiguration;
import se.sics.kompics.wan.master.InstallJobOnHosts;
import se.sics.kompics.wan.master.MasterPort;
import se.sics.kompics.wan.master.PrintConnectedDameons;
import se.sics.kompics.wan.master.PrintDaemonsWithLoadedJob;
import se.sics.kompics.wan.master.PrintLoadedJobs;
import se.sics.kompics.wan.master.ShutdownDaemonRequest;
import se.sics.kompics.wan.master.StartJobOnHosts;
import se.sics.kompics.wan.master.StopJobOnHosts;
import se.sics.kompics.wan.util.HostsParser;
import se.sics.kompics.wan.util.HostsParserException;

public class UserInputMaster extends ComponentDefinition {
	
	private Negative<MasterPort> master = negative(MasterPort.class);
	Positive<Timer> timer = positive(Timer.class);

	private final Scanner scanner;
	
	TreeSet<Address> hosts = null;

	public UserInputMaster() {
		subscribe(handleStart, control);
		subscribe(handleUserInputTimeout, timer);
		scanner = new Scanner(System.in);

	}

	private Handler<Start> handleStart = new Handler<Start>() {
		public void handle(Start event) {

			getInput();
		}
	};

	private Handler<UserInputTimeout> handleUserInputTimeout = new Handler<UserInputTimeout>() {
		public void handle(UserInputTimeout event) {
			getInput();
		}
	};

	private int getJob() {
		System.out.print("\tEnter job id: ");
		return scanner.nextInt();
	}

	private int getNumPeers() {
		System.out.print("\tEnter the number of peers to start at each host: ");
		return scanner.nextInt();
	}

	private void getInput() {
		TreeSet<Address> hosts = MasterConfiguration.getHosts();
		switch (selectOption()) {
		case 1:
			trigger(new PrintConnectedDameons(), master);
			break;
		case 2:
			trigger(new PrintDaemonsWithLoadedJob(getJob()), master);
			break;
		case 3:
			System.out.print("\tEnter daemon-id: ");
			int daemonId = scanner.nextInt();
			trigger(new PrintLoadedJobs(daemonId), master);
			break;
		case 5: // XXX
			hosts = getHosts();
			// deliberate skip of 'break' here.
		case 4:
			System.out.print("\tEnter groupId: ");
			String groupId = scanner.next();
			System.out.print("\tEnter artifactId: ");
			String artifactId = scanner.next();
			System.out.print("\tEnter version: ");
			String version = scanner.next();
			System.out.print("\tEnter mainClass: ");
			String mainClass = scanner.next();
			System.out.print("\tHide Maven output (y/n): ");
			String hideMavenOutput = scanner.next();
			boolean hideOutput = (hideMavenOutput.compareToIgnoreCase("y") == 0) ? true : false;

			// System.out.print("\tEnter any optional args (return for none): ");
			// String allArgs = scanner.next();
			// String[] args = allArgs.split(" ");
			String[] args = {};
			trigger(new InstallJobOnHosts(groupId, artifactId, version, mainClass, Arrays
					.asList(args), hideOutput, hosts), master);
			break;
		case 6:
			int jobId = getJob();
			trigger(new StopJobOnHosts(jobId), master);
			break;
		case 7:
			int sshAuthOpt = 0;
			do {
				sshAuthOpt = selectSshAuthMethod();
			} while (sshAuthOpt < 1 || sshAuthOpt > 2);
			switch (sshAuthOpt) {
			case 1: 
				break;
			case 2:
				break;
			}
			
			hosts = MasterConfiguration.getHosts(); 
			if (hosts == null)
			{
				while (selectHostsFile() == false) ;
			}
			
			break;
		case 8:
			trigger(new ShutdownDaemonRequest(), master);
			break;
		case 9:
			trigger(new StartJobOnHosts(getJob(), getNumPeers()), master);
			break;
		case 0:
			System.out.println("\tGoodbye.");
			System.out.println("\tExiting.....");
			System.out.println();
			System.exit(0);
			break;
		default:
			System.out.println();
			System.out.println("Invalid choice.");
			System.out.println();
			break;
		}
		System.out.println();

		ScheduleTimeout st = new ScheduleTimeout(100);
		UserInputTimeout uit = new UserInputTimeout(st);
		st.setTimeoutEvent(uit);
		trigger(st, timer);
	}
	
	private boolean selectHostsFile()
	{
		boolean succeed = true;
		System.out.println();
		System.out.println("Enter the full pathname of the file containing a list" +
				"of comma-separated hosts in the format host[:port[:id]] ");
		String filename =  scanner.next();
		try {
			TreeSet<Address> hosts = HostsParser.parseHostsFile(filename);
		} catch (FileNotFoundException e) {
			System.out.println("File not found: " + e.getMessage());
			return false;
		} catch (HostsParserException e) {
			System.out.println("Hosts file not formatted correctly: " + e.getMessage());
			return false;
		}		
		
		return succeed;
	}

	private TreeSet<Address> getHosts() {
		int first, last;
		System.out.println("Enter the start of the range of hosts to use:");
		first = scanner.nextInt();
		System.out.println("Enter the end of the range of hosts to use:");
		last = scanner.nextInt();
		return MasterConfiguration.getHosts(first, last);
	}

	private int selectSshAuthMethod() {
		System.out.println();
		System.out.println("Enter a number to select an option from below:");
		System.out.println("\t1) username/password.");
		System.out.println("\t2) public-key authentication.");
		System.out.print("Enter your choice: ");
		return scanner.nextInt();
	}

	private int selectOption() {
		System.out.println();
		System.out.println("Enter a number to select an option from below:");
		System.out.println("\t1) list connected daemons.");
		System.out.println("\t2) specify a job, and list all daemons that have loaded it.");
		System.out.println("\t3) specify a daemon, and list all its loaded jobs.");
		System.out.println("\t4) load a job to all hosts.");
		System.out.println("\t5) load a job to selected hosts.");
		System.out.println("\t6) stop a job on all hosts.");
		System.out.println("\t7) scp (copy) daemon jar file to hosts.");
		System.out.println("\t8) shutdown all hosts.");
		System.out.println("\t9) start a job on all hosts that have loaded the job.");
		System.out.println("\t0) exit program");
		System.out.print("Enter your choice: ");
		return scanner.nextInt();
	}
}
