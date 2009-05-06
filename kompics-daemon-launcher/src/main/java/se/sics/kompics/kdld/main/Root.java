package se.sics.kompics.kdld.main;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import javax.swing.UnsupportedLookAndFeelException;

import se.sics.kompics.address.Address;
import se.sics.kompics.kdld.util.LocalNetworkConfiguration;
import se.sics.kompics.launch.Scenario;
import se.sics.kompics.launch.Topology;

/**
 * The <code>Application</code> class.
 * 
 * @author Jim Dowling 
 */
@SuppressWarnings("serial")
public final class Root {

	private final static int PORT = 22031;
	
	private static 	List<Address> addrs = new ArrayList<Address>();
	
	private static void helpAndExit(String[] args)
	{
		System.out.println("Daemon Launcher Client program.");
		System.out.println("Usage: <prog> [hostsFileName]");
		System.out.println("Num of args was " + args.length);
		System.exit(-1);
	}
	/**
	 * The main method.
	 * Execute using:
	 * mvn exec:java -Dexec.mainClass=[package].main.Root
	 * 
	 * @param args
	 *            the arguments
	 * @throws UnsupportedLookAndFeelException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 */
	public static final void main(String[] args) {

		if (args.length > 1)
		{
			helpAndExit(args);
		}
		if (args.length == 1)
		{
			try {
				addrs = parseHostsFile(args[0]);
			} catch (FileNotFoundException e) {
				System.out.println("Coudln't find hosts file: " + args[0]);
				System.exit(-1);
			}
		}
		

		Topology topology1 = new Topology() {
			{
				String hostAddr = LocalNetworkConfiguration.findLocalHostAddress();;
				node(0, hostAddr, PORT);
				for (Address a : addrs)
				{
					node(0,a.getIp().getHostAddress(),PORT);
				}
//				defaultLinks(1000,0);
			}
		};

//		Scenario scenario1 = new Scenario(MavenExecMain.class) {
//				{
//					command(0, "H1:");
//				}
//			};
//		scenario1.executeOn(topology1);
		System.exit(0);
	}
	
	
	public static List<Address> parseHostsFile(String fileName) throws FileNotFoundException
	{
		List<Address> addrs = new ArrayList<Address>();
		FileInputStream hostFile = new FileInputStream(fileName);
		
		BufferedReader in = new BufferedReader(
				new InputStreamReader(hostFile));
		if (in == null)
		{
			return null;
		}
		String hostPerLine = "";
		while (hostPerLine != null) {
			try {
				hostPerLine = in.readLine();
				if (hostPerLine != null)
				{
					Address addr = new Address(InetAddress.getByName(hostPerLine),PORT,0);
					addrs.add(addr);
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return addrs;
	}

}
