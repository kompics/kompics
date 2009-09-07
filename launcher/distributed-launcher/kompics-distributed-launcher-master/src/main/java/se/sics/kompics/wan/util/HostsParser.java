package se.sics.kompics.wan.util;

import java.io.BufferedReader;
import java.io.File;
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
	private static final Logger logger = LoggerFactory.getLogger(AddressParser.class);
	

	public static Set<Host> parseHostsFile(File file) throws FileNotFoundException, HostsParserException
	{
		FileInputStream hostIs = new FileInputStream(file);
		return parseFile(hostIs);
	}
	
	/**
	 * If a hostname from the list is not resolved, a warning is printed (no exception is thrown).
	 * @param fileName
	 * @return
	 * @throws FileNotFoundException
	 * @throws AddressParserException
	 */
	public static Set<Host> parseHostsFile(String fileName) throws FileNotFoundException, HostsParserException
	{
		FileInputStream hostIs = new FileInputStream(fileName);
		return parseFile(hostIs);
	}
	
	private static Set<Host> parseFile(FileInputStream hostIS) throws HostsParserException
	{
		Set<Host> addrs = new HashSet<Host>();
		BufferedReader in = new BufferedReader(
				new InputStreamReader(hostIS));
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
	
	
	
	public static Host parseExperimentHost(String h) throws UnknownHostException 
	{
		String[] addressParts = h.split(":");
		InetAddress host = null;
		host = InetAddress.getByName(addressParts[0]);
		Host addr = new ExperimentHost(host.getHostName());  
		return addr;

	}

}
