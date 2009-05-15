package se.sics.kompics.kdld.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.address.Address;

/**
 * The <code>HostsParser</code> class.
 * 
 * @author Jim Dowling 
 */
public final class HostsParser {
	private static final Logger logger = LoggerFactory.getLogger(HostsParser.class);
	
	private final static int DEFAULT_PORT = 22031;
	private final static int DEFAULT_ID = 1;
	
	public static List<Address> parseHostsFile(String fileName) throws FileNotFoundException, HostsParserException
	{
		List<Address> addrs = new ArrayList<Address>();
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
						String[] addressParts = h.split(":");
						InetAddress host = null;
						try {
							host = InetAddress.getByName(addressParts[0]);
						} catch (UnknownHostException e) {
							logger.warn("Unknown host:" + e.getMessage());
							continue;
						}
						int port = DEFAULT_PORT;
						int id =  DEFAULT_ID;
						if (addressParts.length >= 2)
						{
							port =  Integer.parseInt(addressParts[1]);
						}
						if (addressParts.length == 3)
						{
							id =  Integer.parseInt(addressParts[2]);
						}
						Address addr = new Address(host, port, id);
						addrs.add(addr);
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

}
