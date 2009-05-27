package se.sics.kompics.wan.master;

import java.util.Arrays;
import java.util.Scanner;
import java.util.TreeSet;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.wan.config.MasterConfiguration;

public class UserInput extends ComponentDefinition {
	private Negative<MasterCommands> master = negative(MasterCommands.class);

	private final Scanner scanner;

	
	public UserInput() {
		subscribe(handleStart, control);
		scanner = new Scanner(System.in);
		
	}

	private Handler<Start> handleStart = new Handler<Start>() {
		public void handle(Start event) {
			
			boolean finishedInput = false;
			while (finishedInput == false) {
				
				TreeSet<Address> hosts = MasterConfiguration.getHosts();
				switch (selectOption()) {
				case 1:
					trigger(new PrintConnectedDameons(), master);
					break;
				case 2:
					System.out.print("\tEnter job id: ");
					int jobId = scanner.nextInt();
					trigger(new PrintDaemonsWithLoadedJob(jobId), master);
					break;
				case 3:
					System.out.print("\tEnter daemon id: ");
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
//					System.out.print("\tEnter any optional args (return for none): ");
//					String allArgs = scanner.next();
//					String[] args = allArgs.split(" ");
					String[] args = {};
					trigger(new InstallJobOnHosts(groupId, artifactId, version, mainClass,
									Arrays.asList(args), hosts), master);
					break;
				case 0:
					finishedInput = true;
					System.out.println("\tGoodbye.");
					System.out.println("\tExiting.....");
					System.out.println();
					System.exit(0);
					break;
				default:
					System.out.println("\tInvalid choice.");
					break;
				}
			}
			
			
			
		}
	};
	
		private TreeSet<Address> getHosts()
		{
			int first, last;
			System.out.println("Enter the start of the range of hosts to use:");
			first = scanner.nextInt();
			System.out.println("Enter the end of the range of hosts to use:");
			last = scanner.nextInt();
			return MasterConfiguration.getHosts(first, last);
		}

		private int selectOption() {
			System.out.println();
			System.out.println("Enter a number to select an option from below:");
			System.out.println("\t1) list connected daemons.");
			System.out.println("\t2) list all daemons with specified loaded job.");
			System.out.println("\t3) list loaded jobs for a specified daemon.");
			System.out.println("\t4) load a job to all hosts.");
			System.out.println("\t5) load a job to selected hosts.");
			System.out.println("\t0) exit program");
			System.out.print("Enter your choice: ");
			return scanner.nextInt();
		}
}

