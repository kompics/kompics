package se.sics.kompics.wan.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.address.Address;
import se.sics.kompics.wan.config.Configuration;
import se.sics.kompics.wan.ssh.ExperimentHost;
import se.sics.kompics.wan.ssh.Host;

/**
 * The <code>HostsParser</code> class.
 * 
 * @author Jim Dowling 
 */
public final class HostsParser {
	private static final Logger logger = LoggerFactory.getLogger(HostsParser.class);
	
	
	/**
	 * If a hostname from the list is not resolved, a warning is printed (no exception is thrown).
	 * @param fileName
	 * @return
	 * @throws FileNotFoundException
	 * @throws HostsParserException
	 */
	public static Set<Host> parseHostsFile(String fileName) throws FileNotFoundException, HostsParserException
	{
		Set<Host> addrs = new HashSet<Host>();
		FileInputStream hostFile = new FileInputStream(fileName);
		
		BufferedReader in = new BufferedReader(
				new InputStreamReader(hostFile));
		if (in == null)
		{
			return null;
		}
		String hostPortIdPerLine = "";
		while (hostPortIdPerLine != null) {
			try {
				hostPortIdPerLine = in.readLine();
				if (hostPortIdPerLine != null)
				{
					String[] hosts = hostPortIdPerLine.split(",");
					for (String h : hosts) {
						try {
							Host addr = parseExperimentHost(h);
							if (addr != null) {
								addrs.add(addr);
							}
						}
						catch (UnknownHostException e)
						{
							logger.warn("Unknown host:" + e.getMessage());
						}
					}
				}
			} catch (NumberFormatException e) {
				throw new HostsParserException("Invalid port or id number: " + e.getMessage());
			}
			catch (IOException e) {
				throw new HostsParserException(e.getMessage());
			}
		
		}
		try {
			in.close();
		} catch (IOException e) {
			throw new HostsParserException(e.getMessage());
		}
		
		return addrs;
	}
	
	
	public static Set<Address> parseAddresses(String fileName) throws FileNotFoundException, HostsParserException
	{
		Set<Address> addrs = new HashSet<Address>();
		FileInputStream hostFile = new FileInputStream(fileName);
		
		BufferedReader in = new BufferedReader(
				new InputStreamReader(hostFile));
		if (in == null)
		{
			return null;
		}
		String hostPortIdPerLine = "";
		while (hostPortIdPerLine != null) {
			try {
				hostPortIdPerLine = in.readLine();
				if (hostPortIdPerLine != null)
				{
					String[] hosts = hostPortIdPerLine.split(",");
					for (String h : hosts) {
						try {
							Address addr = parseHost(h);
							if (addr != null) {
								addrs.add(addr);
							}
						}
						catch (UnknownHostException e)
						{
							logger.warn("Unknown host:" + e.getMessage());
						}
					}
				}
			} catch (NumberFormatException e) {
				throw new HostsParserException("Invalid port or id number: " + e.getMessage());
			}
			catch (IOException e) {
				throw new HostsParserException(e.getMessage());
			}
		
		}
		try {
			in.close();
		} catch (IOException e) {
			throw new HostsParserException(e.getMessage());
		}
		
		return addrs;
	}
	
	
	public static Address parseHost(String h) throws UnknownHostException
	{
		String[] addressParts = h.split(":");
		InetAddress host = null;
		host = InetAddress.getByName(addressParts[0]);
		int port = Configuration.DEFAULT_PORT;
		int id =  Configuration.DEFAULT_ID;
		if (addressParts.length >= 2)
		{
			port =  Integer.parseInt(addressParts[1]);
		}
		if (addressParts.length == 3)
		{
			id =  Integer.parseInt(addressParts[2]);
		}
		Address addr = new Address(host, port, id);

		return addr;

	}
	
	public static Host parseExperimentHost(String h) throws UnknownHostException 
	{
		String[] addressParts = h.split(":");
		InetAddress host = null;
		host = InetAddress.getByName(addressParts[0]);
		Host addr = new ExperimentHost(host.getHostName());  
		return addr;

	}

}
